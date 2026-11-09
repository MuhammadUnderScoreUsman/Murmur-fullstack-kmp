package com.example.chatappkross.auth.presentation.routes

import com.example.chatappkross.auth.domain.AuthService
import com.example.chatappkross.model.LoginRequest
import com.example.chatappkross.model.RegisterRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/register") {
            call.respond(HttpStatusCode.Created, authService.register(call.receive<RegisterRequest>()))
        }
        post("/login") {
            call.respond(authService.login(call.receive<LoginRequest>()))
        }
    }
}
