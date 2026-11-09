package com.example.chatappkross.auth.presentation.routes


import com.example.chatappkross.auth.data.common.toDto
import com.example.chatappkross.auth.domain.UserService
import com.example.chatappkross.auth.domain.model.UserRecord
import com.example.chatappkross.model.UserDto
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.userRoutes(users: UserService) {
    route("/users"){
        get {
            call.respond(
                users.sortedViaName().map {
                    it.toDto()
                },
            )
        }
    }
}

