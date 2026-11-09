package com.example.chatappkross.presentation.chatting.chat

import androidx.lifecycle.ViewModel
import com.example.chatappkross.chat.presentation.ChatUiState
import com.example.chatappkross.chat.presentation.SharedChatViewModel
import kotlinx.coroutines.flow.StateFlow


class ChatViewModel(private val shared: SharedChatViewModel) : ViewModel() {

    val state: StateFlow<ChatUiState> = shared.state

    fun reload() = shared.reload()
    fun loadOlder() = shared.loadOlder()
    fun send(text: String): Boolean = shared.send(text)
    fun retry(messageId: String) = shared.retry(messageId)
    fun discard(messageId: String) = shared.discard(messageId)

    override fun onCleared() = shared.clear()
}
