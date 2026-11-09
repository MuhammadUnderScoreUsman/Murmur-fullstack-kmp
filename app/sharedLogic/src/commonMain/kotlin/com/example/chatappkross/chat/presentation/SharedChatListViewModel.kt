package com.example.chatappkross.chat.presentation

import com.example.chatappkross.chat.data.client.RealtimeClient
import com.example.chatappkross.chat.domain.realtime.ConnectionState
import com.example.chatappkross.chat.domain.repository.ChatRepository
import com.example.chatappkross.chat.domain.repository.UserRepository
import com.example.chatappkross.chat.domain.util.ChatResult
import com.example.chatappkross.chat.domain.util.TimeLabels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One row: a person, whether they are online, and the latest message of your chat with them (if any). */
data class ContactItem(
    val id: String,
    val username: String,
    val isOnline: Boolean,
    /** null until you have exchanged a message with them or opened the chat once. */
    val conversationId: String? = null,
    /** "You: see you soon" or "see you soon". null if there are no messages yet. */
    val lastMessagePreview: String? = null,
    /** "14:05", "Yesterday", "Nov 9"... null if there are no messages yet. */
    val lastMessageTime: String? = null,
    /** Server time of the last message, 0 if none. */
    val lastMessageAt: Long = 0L,
)

data class ChatListUiState(
    val contacts: List<ContactItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val connection: ConnectionState = ConnectionState.DISCONNECTED,
    /** The person whose chat is being opened right now (shows a spinner on that row). */
    val openingUserId: String? = null,
)

/** The answer to "the user tapped this person". The platform turns it into navigation or an alert. */
sealed interface OpenChatResult {
    data class Opened(val conversationId: String, val peerId: String, val peerUsername: String) : OpenChatResult
    data class Failed(val message: String) : OpenChatResult

    /** Another chat is still being opened (a double tap): do nothing. */
    data object Ignored : OpenChatResult
}

private data class Person(val id: String, val username: String)

private data class LoadState(
    val people: List<Person> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/**
 * The chat list's logic, shared by Android and iOS: loading the people, merging them with live presence and
 * the latest messages, the ordering rules, and opening a chat. The platform view models only add what is
 * their own: navigation and alerts, and logging out through their session.
 */
class SharedChatListViewModel(
    private val myId: String?,
    private val usersRepository: UserRepository,
    private val realtimeClient: RealtimeClient,
    private val chatRepository: ChatRepository,
    scope: CoroutineScope = newViewModelScope(),
) : SharedViewModel(scope) {

    private val loaded = MutableStateFlow(LoadState())
    private val opening = MutableStateFlow<String?>(null)
    private var loadJob: Job? = null

    val state: StateFlow<ChatListUiState> = combine(
        loaded,
        realtimeClient.state,
        chatRepository.state,
        opening,
    ) { load, realtime, chat, openingId ->
        val conversationsByPeer = chat.conversations.items.associateBy { it.peerId }
        ChatListUiState(
            contacts = load.people
                .filter { it.id != myId }
                .map { person ->
                    val conversation = conversationsByPeer[person.id]
                    val last = conversation?.lastMessage
                    ContactItem(
                        id = person.id,
                        username = person.username,
                        isOnline = realtime.isOnline(person.id),
                        conversationId = conversation?.id,
                        lastMessagePreview = last?.let { (if (it.senderId == myId) "You: " else "") + it.text },
                        lastMessageTime = last?.let { TimeLabels.listLabel(it.createdAt) },
                        lastMessageAt = last?.createdAt ?: 0L,
                    )
                }
                // Chats with messages first (latest on top), then everyone else: online first, then by name
                .sortedWith(
                    compareByDescending<ContactItem> { it.lastMessageAt }
                        .thenByDescending { it.isOnline }
                        .thenBy { it.username.lowercase() },
                ),
            isLoading = load.isLoading,
            errorMessage = load.errorMessage,
            connection = realtime.connection,
            openingUserId = openingId,
        )
    }.stateIn(scope, SharingStarted.Eagerly, ChatListUiState())

    init {
        refresh()

        // Someone registered after we loaded the list and just came online: fetch the list again
        scope.launch {
            realtimeClient.state
                .map { it.onlineUserIds }
                .distinctUntilChanged()
                .collect { online ->
                    val current = loaded.value
                    val known = current.people.map { it.id }.toSet()
                    if (!current.isLoading && online.any { it !in known }) refresh()
                }
        }
    }

    /** Reloads the people and the conversations. Does nothing extra if a load is already running. */
    fun refresh() {
        chatRepository.refreshConversations()
        if (loadJob?.isActive == true) return
        loadJob = scope.launch {
            loaded.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = usersRepository.fetchUsers()) {
                is ChatResult.Success ->
                    loaded.value = LoadState(people = result.data.map { Person(it.id, it.username) }, isLoading = false)

                is ChatResult.Failure -> loaded.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    /** What to do when a person is tapped. Creates the conversation first if there isn't one yet. */
    suspend fun openChat(contactId: String): OpenChatResult {
        val contact = state.value.contacts.firstOrNull { it.id == contactId }
            ?: return OpenChatResult.Failed("That person isn't in your list anymore.")

        contact.conversationId?.let { return OpenChatResult.Opened(it, contact.id, contact.username) }

        if (!opening.compareAndSet(null, contactId)) return OpenChatResult.Ignored
        return try {
            when (val result = chatRepository.openConversation(contactId)) {
                is ChatResult.Success -> OpenChatResult.Opened(result.data.id, contact.id, contact.username)
                is ChatResult.Failure -> OpenChatResult.Failed(result.message)
            }
        } finally {
            opening.value = null
        }
    }

    /** For Swift: calls [onState] now and on every change. Dispose the handle when the screen goes away. */
    fun watch(onState: (ChatListUiState) -> Unit): DisposableHandle = observe(state, onState)
}
