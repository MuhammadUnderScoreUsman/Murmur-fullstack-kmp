package com.example.chatappkross.core

import com.example.chatappkross.auth.data.AuthRepositoryImpl
import com.example.chatappkross.auth.data.ChatSessionHooks
import com.example.chatappkross.auth.data.InMemorySessionStore
import com.example.chatappkross.auth.data.SessionStore
import com.example.chatappkross.auth.domain.AuthRepository
import com.example.chatappkross.auth.presentation.SharedLoginViewModel
import com.example.chatappkross.auth.presentation.SharedRegisterViewModel
import com.example.chatappkross.chat.data.client.RealtimeClient
import com.example.chatappkross.chat.data.reposiotry.ChatRemoteDataSourceImpl
import com.example.chatappkross.chat.data.reposiotry.ChatRepositoryImpl
import com.example.chatappkross.chat.data.reposiotry.UsersRepositoryImpl
import com.example.chatappkross.chat.domain.repository.UserRepository
import com.example.chatappkross.chat.presentation.SharedChatListViewModel
import com.example.chatappkross.chat.presentation.SharedChatViewModel
import com.example.chatappkross.model.AuthValidator

/**
 * Entry point for both apps. Create it once at app start.
 *
 * baseUrl examples (note the trailing slash):
 *  - Android emulator: "http://10.0.2.2:8080/"
 *  - iOS simulator:    "http://localhost:8080/"
 */
class SharedContainer(baseUrl: String, sessionStore: SessionStore) {
    // Swift can't see Kotlin default arguments, so the one-argument version is explicit
    constructor(baseUrl: String) : this(baseUrl, InMemorySessionStore())

    private val httpClient = createHttpClient(baseUrl)
    private val realtimeHttpClient = createRealtimeHttpClient()

    /** The live connection: presence and messages. */
    val realtimeClient = RealtimeClient(realtimeHttpClient, toWebSocketBaseUrl(baseUrl))

    /** Conversations, message threads, sending and retrying. */
    val chatRepositoryImpl = ChatRepositoryImpl(ChatRemoteDataSourceImpl(httpClient, sessionStore), realtimeClient)

    /**
     * Wrapped so ChatRepositoryImpl always knows who is logged in and starts clean for a new user:
     * the apps just use this repository as before and never have to tell the chat layer anything.
     */
    val authRepository: AuthRepository =
        ChatSessionHooks(AuthRepositoryImpl(httpClient, sessionStore), chatRepositoryImpl)

    val usersRepository: UserRepository = UsersRepositoryImpl(httpClient)

    /** Returns an error message, or null if valid. Same rules the server enforces. */
    fun validateUsername(username: String): String? = AuthValidator.validateUsername(username)

    fun validatePassword(password: String): String? = AuthValidator.validatePassword(password)

    fun createLoginViewModel(onAuthenticated: (userId: String, username: String) -> Unit): SharedLoginViewModel =
        SharedLoginViewModel(authRepository, onAuthenticated)

    fun createRegisterViewModel(onAuthenticated: (userId: String, username: String) -> Unit): SharedRegisterViewModel =
        SharedRegisterViewModel(authRepository, onAuthenticated)

    fun createChatListViewModel(myId: String?): SharedChatListViewModel =
        SharedChatListViewModel(myId, usersRepository, realtimeClient, chatRepositoryImpl /* your ChatRepositoryImpl instance */)

    fun createChatViewModel(conversationId: String, peerId: String, peerUsername: String, myId: String): SharedChatViewModel =
        SharedChatViewModel(conversationId, peerId, peerUsername, myId, chatRepositoryImpl, realtimeClient)

    fun close() {
        chatRepositoryImpl.close()
        realtimeClient.close()
        realtimeHttpClient.close()
        httpClient.close()
    }
}