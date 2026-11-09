package com.example.chatappkross.auth.domain

import com.example.chatappkross.model.ErrorCode
import com.example.chatappkross.model.UserDto

/** Why a call failed, in a form the UI can show without knowing about HTTP. */
sealed interface AuthFailure {
    val message: String

    /** The server answered with an error (wrong password, username taken, ...). */
    data class Server(val code: ErrorCode, override val message: String) : AuthFailure

    /** No answer: offline, timeout, server down, unreadable response. */
    data class Network(
        override val message: String = "Can't reach the server. Check your connection.",
    ) : AuthFailure
}

sealed interface AuthResult {
    data class Success(val user: UserDto) : AuthResult
    data class Failure(val failure: AuthFailure) : AuthResult
}
