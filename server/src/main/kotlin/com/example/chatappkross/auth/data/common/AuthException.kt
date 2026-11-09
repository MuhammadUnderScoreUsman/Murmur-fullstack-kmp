package com.example.chatappkross.auth.data.common

import com.example.chatappkross.model.ErrorCode
import io.ktor.http.HttpStatusCode


class AuthException(
    val code: ErrorCode,
    override val message: String,
    val status: HttpStatusCode,
) : RuntimeException(message)
