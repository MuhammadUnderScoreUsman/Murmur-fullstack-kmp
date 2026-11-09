package com.example.chatappkross.auth.data

import com.example.chatappkross.auth.domain.AuthRepository
import com.example.chatappkross.auth.domain.AuthResult
import com.example.chatappkross.chat.domain.repository.ChatRepository
import com.example.chatappkross.model.UserDto

class ChatSessionHooks(
    private val inner: AuthRepository,
    private val chat: ChatRepository,
) : AuthRepository {

    override suspend fun restoreSession(): UserDto? =
        inner.restoreSession().also { chat.setUser(it?.id) }

    override suspend fun register(username: String, password: String): AuthResult =
        inner.register(username, password).also(::track)

    override suspend fun login(username: String, password: String): AuthResult =
        inner.login(username, password).also(::track)

    override suspend fun logout() {
        inner.logout()
        chat.setUser(null)
    }

    private fun track(result: AuthResult) {
        if (result is AuthResult.Success) chat.setUser(result.user.id)
    }
}