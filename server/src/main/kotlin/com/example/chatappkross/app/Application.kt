package com.example.chatappkross.app

import com.example.chatappkross.auth.data.UserServiceImpl
import com.example.chatappkross.auth.presentation.authModule
import com.example.chatappkross.auth.presentation.routes.userRoutes
import com.example.chatappkross.chat.data.ConversationServiceImpl
import com.example.chatappkross.chat.data.MessageServiceImpl
import com.example.chatappkross.chat.domain.usecase.ConversationUseCase
import com.example.chatappkross.chat.domain.usecase.HistoryUseCase
import com.example.chatappkross.chat.domain.usecase.OpenChatUseCase
import com.example.chatappkross.chat.domain.usecase.SendUseCase
import com.example.chatappkross.chat.domain.usecase.StartConversationUseCases
import com.example.chatappkross.chat.presentation.realtime.PresenceRegistry
import com.example.chatappkross.chat.presentation.realtime.realtimeModule
import com.example.chatappkross.chat.presentation.route.chatRoutes
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val users = UserServiceImpl()
    val registry = PresenceRegistry()
    val csI = ConversationServiceImpl()
    val msI = MessageServiceImpl()

    val sendMessage = SendUseCase(csI, msI, registry)
    val convoUseCases = StartConversationUseCases(
        conversationUseCase = ConversationUseCase(
           conversations =  csI, messages = msI, users = users,
        ),
        historyUseCase = HistoryUseCase(
            conversations =  csI,
            messages = msI,
        ),
        openChatUseCase = OpenChatUseCase(
            conversations =  csI, messages = msI, users = users,
        )

    )

    authModule(users)
    realtimeModule(users, registry, sendMessage)
    routing {
        userRoutes(users)
        chatRoutes(convoUseCases, users)
    }
}