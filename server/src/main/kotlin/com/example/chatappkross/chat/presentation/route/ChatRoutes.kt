package com.example.chatappkross.chat.presentation.route

import com.example.chatappkross.auth.domain.UserService
import com.example.chatappkross.chat.domain.usecase.StartConversationUseCases
import com.example.chatappkross.model.OpenConversationRequest
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.chatRoutes(chat: StartConversationUseCases, users: UserService) {
    route("/conversations") {
        get {
            val me = call.callerId(users)
            call.respond(chat.conversationUseCase(me))
        }

        post {
            val me = call.callerId(users)
            val request = call.receive<OpenConversationRequest>()
            call.respond(chat.openChatUseCase(me, request.userId))
        }

        get("/{id}/messages") {
            val me = call.callerId(users)
            val conversationId = call.parameters["id"].orEmpty()
            val before = call.request.queryParameters["before"]
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE
            call.respond(chat.historyUseCase(me, conversationId, before, limit))
        }
    }
}

private const val DEFAULT_PAGE_SIZE = 50
