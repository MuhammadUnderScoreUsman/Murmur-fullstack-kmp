package com.example.chatappkross.auth.presentation

import com.example.chatappkross.auth.data.AuthServiceImpl
import com.example.chatappkross.auth.data.common.AuthException
import com.example.chatappkross.auth.domain.UserService
import com.example.chatappkross.model.ApiError
import com.example.chatappkross.model.ErrorCode
import com.example.chatappkross.auth.presentation.routes.authRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing


fun Application.authModule(userService: UserService) {
    val authService = AuthServiceImpl(userService)

    install(ContentNegotiation) { json() }

    install(StatusPages) {
        exception<AuthException> { call, e ->
            call.respond(e.status, ApiError(e.code, e.message))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ApiError(ErrorCode.INVALID_INPUT, "Malformed request body"))
        }
        exception<Throwable> { call, e ->
            call.application.log.error("Unhandled error", e)
            call.respond(HttpStatusCode.InternalServerError, ApiError(ErrorCode.INTERNAL, "Something went wrong"))
        }
    }

    routing {
        get("/") { call.respondText("Chat server is running") }
        authRoutes(authService)
    }
}
