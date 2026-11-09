package com.example.chatappkross.chat.presentation.realtime


import com.example.chatappkross.auth.domain.UserService
import com.example.chatappkross.chat.domain.usecase.SendUseCase
import com.example.chatappkross.chat.presentation.route.realtimeRoutes
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlin.time.Duration.Companion.seconds


fun Application.realtimeModule(users: UserService, registry: PresenceRegistry, chat: SendUseCase) {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 30.seconds
        maxFrameSize = 64 * 1024
    }

    routing { realtimeRoutes(registry, users, chat) }
}