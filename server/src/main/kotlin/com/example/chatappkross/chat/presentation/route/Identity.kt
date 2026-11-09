package com.example.chatappkross.chat.presentation.route

import com.example.chatappkross.auth.data.common.AuthException
import com.example.chatappkross.chat.domain.common.ChatException
import com.example.chatappkross.auth.domain.UserService
import com.example.chatappkross.model.ErrorCode
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall

const val USER_ID_HEADER = "X-User-Id"

suspend fun ApplicationCall.callerId(users: UserService): String {
    val id = request.headers[USER_ID_HEADER]
    if (id.isNullOrBlank() || users.findById(id) == null) {
        throw ChatException(
            ErrorCode.UNAUTHORIZED,
            "Missing or unknown user",
            HttpStatusCode.Unauthorized
        )
    }
    return id
}
