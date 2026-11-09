package com.example.chatappkross.chat.data.reposiotry

import com.example.chatappkross.chat.data.addPending
import com.example.chatappkross.chat.data.client.RealtimeClient
import com.example.chatappkross.chat.data.discardFailed
import com.example.chatappkross.chat.data.failIfSending
import com.example.chatappkross.chat.data.failPending
import com.example.chatappkross.chat.data.fromDto
import com.example.chatappkross.chat.data.markSendingAgain
import com.example.chatappkross.chat.data.mergeServerMessages
import com.example.chatappkross.chat.data.toDto
import com.example.chatappkross.chat.data.updateThread
import com.example.chatappkross.chat.data.withLastMessage
import com.example.chatappkross.chat.domain.ChatState
import com.example.chatappkross.chat.domain.ConversationsState
import com.example.chatappkross.chat.domain.SendStatus
import com.example.chatappkross.chat.domain.model.Message
import com.example.chatappkross.chat.domain.realtime.ConnectionState
import com.example.chatappkross.chat.domain.repository.ChatRemoteDataSource
import com.example.chatappkross.chat.domain.repository.ChatRepository
import com.example.chatappkross.chat.domain.util.ChatResult
import com.example.chatappkross.chat.domain.util.OpenConversationResult
import com.example.chatappkross.event.ChatError
import com.example.chatappkross.event.MessageReceived
import com.example.chatappkross.event.SendMessage
import com.example.chatappkross.event.ServerEvent
import com.example.chatappkross.model.ChatValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * The chat repository: the conversation list, the message threads, sending with sending/sent/failed states,
 * retries, history paging and live messages, merged from two sources (HTTP history and the live socket).
 *
 * The screens only render [state] and call the functions of [com.example.chatappkross.chat.domain.repository.ChatRepository]. Android collects [state];
 * iOS uses [watch].
 *
 * It keeps the user's data for the length of the session. [setUser] (called for you by the auth
 * repository in SharedContainer) clears everything when the user changes or logs out.
 */

class ChatRepositoryImpl internal constructor(
    private val remote: ChatRemoteDataSource,
    private val realtime: RealtimeClient,
) : ChatRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(ChatState())
    override val state: StateFlow<ChatState> = _state.asStateFlow()

    private val currentUserId = MutableStateFlow<String?>(null)

    init {
        // UNDISPATCHED: start listening right now, so no event can slip past before the collector is running
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            realtime.events.collect { handleEvent(it) }
        }

        // Every time the socket (re)connects, catch up on anything missed while it was down
        scope.launch {
            realtime.state
                .map { it.connection }
                .distinctUntilChanged()
                .collect { if (it == ConnectionState.CONNECTED) resync() }
        }
    }

    // ---------- user ----------

    /** Called when someone logs in, a session is restored (id), or logs out (null). A different user starts clean. */
    override fun setUser(userId: String?) {
        if (currentUserId.value == userId) return
        currentUserId.value = userId
        _state.value = ChatState()
    }

    // ---------- conversation list ----------

    override fun refreshConversations() {
        val user = currentUserId.value ?: return
        scope.launch {
            _state.update {
                it.copy(conversations = it.conversations.copy(isLoading = it.conversations.items.isEmpty(), errorMessage = null))
            }
            val result = remote.conversations()
            if (currentUserId.value != user) return@launch // logged out or switched user while loading
            when (result) {
                is ChatResult.Success -> _state.update {
                    it.copy(
                        conversations = ConversationsState(
                            items = result.data.sortedByDescending { c ->
                                c.lastMessage?.createdAt ?: 0L
                            },
                        ),
                    )
                }

                is ChatResult.Failure -> _state.update {
                    it.copy(conversations = it.conversations.copy(isLoading = false, errorMessage = result.message))
                }
            }
        }
    }

    override suspend fun openConversation(userId: String): OpenConversationResult = remote.openConversation(userId)

    // ---------- one thread ----------

    override fun loadThread(conversationId: String) {
        val user = currentUserId.value ?: return
        scope.launch {
            _state.update { s ->
                s.updateThread(conversationId) { it.copy(isLoading = it.messages.isEmpty(), errorMessage = null) }
            }
            val result = remote.history(conversationId, beforeMessageId = null, limit = PAGE_SIZE)
            if (currentUserId.value != user) return@launch
            when (result) {
                is ChatResult.Success -> _state.update { s ->
                    s.updateThread(conversationId) { t ->
                        val firstEverLoad = t.messages.none { it.status == SendStatus.SENT }
                        t.mergeServerMessages(result.data.map { it.toDto() }).copy(
                            isLoading = false,
                            hasMoreOlder = if (firstEverLoad) result.data.size >= PAGE_SIZE else t.hasMoreOlder,
                        )
                    }
                }

                is ChatResult.Failure -> _state.update { s ->
                    s.updateThread(conversationId) { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    override fun loadOlder(conversationId: String) {
        val user = currentUserId.value ?: return
        val thread = _state.value.thread(conversationId)
        val oldest = thread.messages.firstOrNull { it.status == SendStatus.SENT } ?: return
        if (thread.isLoadingOlder || !thread.hasMoreOlder) return

        scope.launch {
            _state.update { s -> s.updateThread(conversationId) { it.copy(isLoadingOlder = true, errorMessage = null) } }
            val result = remote.history(conversationId, beforeMessageId = oldest.id, limit = PAGE_SIZE)
            if (currentUserId.value != user) return@launch
            when (result) {
                is ChatResult.Success -> _state.update { s ->
                    s.updateThread(conversationId) { thread ->
                        thread.mergeServerMessages(result.data.map { it.toDto() })
                            .copy(isLoadingOlder = false, hasMoreOlder = result.data.size >= PAGE_SIZE)
                    }
                }

                is ChatResult.Failure -> _state.update { s ->
                    s.updateThread(conversationId) { it.copy(isLoadingOlder = false, errorMessage = result.message) }
                }
            }
        }
    }

    // ---------- sending ----------

    override fun send(conversationId: String, text: String): Boolean {
        val me = currentUserId.value ?: return false
        val trimmed = text.trim()
        if (ChatValidator.validateMessageText(trimmed) != null) return false

        val item = Message(
            id = generateMessageId(),
            conversationId = conversationId,
            senderId = me,
            text = trimmed,
            createdAt = 0L,
            status = SendStatus.SENDING,
        )
        _state.update { s -> s.updateThread(conversationId) { it.addPending(item) } }
        transmit(item)
        return true
    }

    override fun retry(conversationId: String, messageId: String) {
        val item = _state.value.thread(conversationId).messages
            .firstOrNull { it.id == messageId && it.status == SendStatus.FAILED } ?: return
        _state.update { s -> s.updateThread(conversationId) { it.markSendingAgain(messageId) } }
        transmit(item)
    }

    override fun discard(conversationId: String, messageId: String) {
        _state.update { s -> s.updateThread(conversationId) { it.discardFailed(messageId) } }
    }

    private fun transmit(item: Message) {
        val queued = realtime.send(SendMessage(item.id, item.conversationId, item.text))
        if (!queued) {
            failMessage(item.conversationId, item.id, "Not connected")
            return
        }
        scope.launch {
            delay(SEND_TIMEOUT_MILLIS)
            failMessage(item.conversationId, item.id, "No response from the server") // does nothing if it was confirmed
        }
    }

    private fun failMessage(conversationId: String, messageId: String, reason: String) {
        _state.update { s -> s.updateThread(conversationId) { it.failIfSending(messageId, reason) } }
    }

    // ---------- live events and reconnects ----------

    private fun handleEvent(event: ServerEvent) {
        when (event) {
            is MessageReceived -> {
                val message = event.message.fromDto() // events speak core DTOs; convert once, here
                val knownConversation = _state.value.conversations.items.any { it.id == message.conversationId }
                _state.update { s ->
                    // Only threads the user has opened are kept up to date; others load fresh when opened
                    val withThread = if (message.conversationId in s.threads) {
                        s.updateThread(message.conversationId) { it.mergeServerMessages(listOf(message.toDto())) }
                    } else {
                        s
                    }
                    withThread.copy(conversations = withThread.conversations.withLastMessage(message.toDto()))
                }
                // A conversation we have never seen (someone just wrote to us for the first time)
                if (!knownConversation) refreshConversations()
            }

            is ChatError -> event.messageId?.let { id -> _state.update { it.failPending(id, event.message) } }

            else -> Unit // presence is handled by RealtimeClient
        }
    }

    private fun resync() {
        refreshConversations()
        _state.value.threads.keys.forEach { loadThread(it) }
    }

    // ---------- lifecycle ----------

    /** For Swift: calls [onState] now and on every change, on the main thread. Dispose the handle when done. */
    fun watch(onState: (ChatState) -> Unit): DisposableHandle {
        val job = scope.launch(Dispatchers.Main) { state.collect { onState(it) } }
        return DisposableHandle { job.cancel() }
    }

    fun close() {
        scope.cancel()
    }

    private companion object {
        const val PAGE_SIZE = 50
        const val SEND_TIMEOUT_MILLIS = 10_000L
    }

    internal fun generateMessageId(): String {
        val bytes = Random.nextBytes(16)
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()
        val hex = bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
        return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}"
    }

}