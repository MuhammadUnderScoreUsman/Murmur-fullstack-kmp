package com.example.chatappkross.presentation.chatting.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatappkross.presentation.session.SessionManager
import com.example.chatappkross.chat.presentation.ChatListUiState
import com.example.chatappkross.chat.presentation.ContactItem
import com.example.chatappkross.chat.presentation.OpenChatResult
import com.example.chatappkross.chat.presentation.SharedChatListViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface ChatListEvent {
    data class OpenChat(val conversationId: String, val peerId: String, val peerUsername: String) : ChatListEvent
    data class ShowError(val message: String) : ChatListEvent
}

class ChatListViewModel(
    private val sessionManager: SessionManager,
    private val shared: SharedChatListViewModel,
) : ViewModel() {

    val state: StateFlow<ChatListUiState> = shared.state

    private val eventChannel = Channel<ChatListEvent>(Channel.BUFFERED)
    val events: Flow<ChatListEvent> = eventChannel.receiveAsFlow()

    fun refresh() = shared.refresh()

    fun onContactClick(contact: ContactItem) {
        viewModelScope.launch {
            when (val result = shared.openChat(contact.id)) {
                is OpenChatResult.Opened ->
                    eventChannel.send(ChatListEvent.OpenChat(result.conversationId, result.peerId, result.peerUsername))

                is OpenChatResult.Failed -> eventChannel.send(ChatListEvent.ShowError(result.message))
                OpenChatResult.Ignored -> Unit
            }
        }
    }

    fun logout() = sessionManager.logout()

    override fun onCleared() = shared.clear()
}