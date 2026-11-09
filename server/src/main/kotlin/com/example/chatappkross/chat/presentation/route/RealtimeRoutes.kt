package com.example.chatappkross.chat.presentation.route

import com.example.chatappkross.auth.domain.UserService
import com.example.chatappkross.chat.domain.common.ChatException
import com.example.chatappkross.chat.domain.usecase.SendUseCase
import com.example.chatappkross.chat.presentation.realtime.Connection
import com.example.chatappkross.chat.presentation.realtime.PresenceRegistry
import com.example.chatappkross.event.ChatError
import com.example.chatappkross.event.SendMessage
import com.example.chatappkross.event.decodeClientEvent
import com.example.chatappkross.event.encodeServerEvent
import com.example.chatappkross.model.ErrorCode
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException


fun Route.realtimeRoutes(registry: PresenceRegistry, users: UserService, chat: SendUseCase) {
    webSocket("/ws") {
        // TEMPORARY identity: the client says who it is, so anyone could claim to be anyone.
        // Replace with a validated token before this is used for real (same seam as Identity.kt).
        val userId = call.request.queryParameters["userId"]
        val user = userId?.let { users.findById(it) }
        if (user == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unknown user"))
            return@webSocket
        }

        val connection = Connection(user.id)
        registry.connect(connection)

        // One writer per socket, so frames go out in order and a slow client only affects itself
        val writer = launch {
            for (event in connection.outbound) {
                send(Frame.Text(encodeServerEvent(event)))
            }
            // The queue was closed: the user disconnected, or stopped reading and was dropped
            close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "Connection closed"))
        }

        try {
            // Frames from one client are handled one at a time, in the order they were sent
            for (frame in incoming) {
                if (frame is Frame.Text) handleFrame(user.id, connection, frame.readText(), registry, chat)
            }
        } finally {
            // Runs on a normal close, a network drop (ping timeout) and cancellation alike
            registry.disconnect(connection)
            writer.cancel()
        }
    }
}


private suspend fun handleFrame(
    userId: String,
    connection: Connection,
    text: String,
    registry: PresenceRegistry,
    chat: SendUseCase,
) {
    val event = try {
        decodeClientEvent(text)
    } catch (e: IllegalArgumentException) {
        registry.send(connection,
            ChatError(code = ErrorCode.INVALID_INPUT, message = "Unrecognised message")
        )
        return
    }

    when (event) {
        is SendMessage -> try {
            chat(userId, connection, event)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ChatException) {
            registry.send(connection, ChatError(event.messageId, e.code, e.message))
        } catch (e: Exception) {
            registry.send(connection, ChatError(event.messageId, ErrorCode.INTERNAL, "Something went wrong"))
        }
    }
}
