package com.example.chatappkross.chat.presentation

import com.example.chatappkross.chat.data.client.RealtimeClient
import com.example.chatappkross.chat.domain.SendStatus
import com.example.chatappkross.chat.domain.realtime.ConnectionState
import com.example.chatappkross.chat.domain.repository.ChatRepository
import com.example.chatappkross.chat.domain.util.TimeLabels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** One bubble, ready to draw. */
data class MessageUi(
    val id: String,
    val text: String,
    val isMine: Boolean,
    val status: SendStatus,
    val failureReason: String?,
    /** null while the server hasn't confirmed the message yet. */
    val time: String?,
)

data class ChatUiState(
    val peerUsername: String,
    val peerOnline: Boolean = false,
    val connection: ConnectionState = ConnectionState.DISCONNECTED,
    /** Oldest first. */
    val messages: List<MessageUi> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingOlder: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * One open chat, shared by Android and iOS. The real work (history, sending, retrying, live messages) is in the
 * chat repository; this class picks this conversation out of its state and shapes it for the screen.
 */
class SharedChatViewModel(
    private val conversationId: String,
    private val peerId: String,
    private val peerUsername: String,
    private val myId: String,
    private val chatRepository: ChatRepository,
    realtimeClient: RealtimeClient,
    scope: CoroutineScope = newViewModelScope(),
) : SharedViewModel(scope) {

    val state: StateFlow<ChatUiState> = combine(chatRepository.state, realtimeClient.state) { chat, realtime ->
        val thread = chat.thread(conversationId)
        ChatUiState(
            peerUsername = peerUsername,
            peerOnline = realtime.isOnline(peerId),
            connection = realtime.connection,
            messages = thread.messages.map {
                MessageUi(
                    id = it.id,
                    text = it.text,
                    isMine = it.senderId == myId,
                    status = it.status,
                    failureReason = it.failureReason,
                    time = TimeLabels.clock(it.createdAt),
                )
            },
            isLoading = thread.isLoading,
            isLoadingOlder = thread.isLoadingOlder,
            errorMessage = thread.errorMessage,
        )
    }.stateIn(scope, SharingStarted.Eagerly, ChatUiState(peerUsername = peerUsername))

    init {
        reload()
    }

    fun reload() = chatRepository.loadThread(conversationId)

    fun loadOlder() = chatRepository.loadOlder(conversationId)

    /** Returns true if the message was accepted (so the screen can clear the text box). */
    fun send(text: String): Boolean = chatRepository.send(conversationId, text)

    fun retry(messageId: String) = chatRepository.retry(conversationId, messageId)

    fun discard(messageId: String) = chatRepository.discard(conversationId, messageId)

    /** For Swift: calls [onState] now and on every change. Dispose the handle when the screen goes away. */
    fun watch(onState: (ChatUiState) -> Unit): DisposableHandle = observe(state, onState)
}
