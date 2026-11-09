package com.example.chatappkross.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.chatappkross.presentation.chatting.chat.ChatViewModel
import com.example.chatappkross.presentation.session.SessionState
import com.example.chatappkross.core.SharedContainer
import com.example.chatappkross.model.UserDto
import com.example.chatappkross.presentation.app.AppContainer
import com.example.chatappkross.presentation.auth.login.LoginViewModel
import com.example.chatappkross.presentation.auth.register.RegisterViewModel
import com.example.chatappkross.presentation.chatting.list.ChatListViewModel
import kotlin.jvm.java


class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val shared = container.shared
        val session = container.sessionManager
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(shared.createLoginViewModel { id, name -> session.onAuthenticated(
                    UserDto(id, name)
                ) })

            modelClass.isAssignableFrom(RegisterViewModel::class.java) ->
                RegisterViewModel(shared.createRegisterViewModel { id, name -> session.onAuthenticated(UserDto(id, name)) })

            modelClass.isAssignableFrom(ChatListViewModel::class.java) ->
                ChatListViewModel(
                    session,
                    shared.createChatListViewModel(myId = (session.state.value as? SessionState.LoggedIn)?.user?.id),
                )
            else -> error("Unknown ViewModel: ${modelClass.name}")
        } as T
    }
}


fun chatViewModelFactory(
    container: SharedContainer,
    myId: String,
    conversationId: String,
    peerId: String,
    peerUsername: String,
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        ChatViewModel(container.createChatViewModel(conversationId, peerId, peerUsername, myId))
    }
}