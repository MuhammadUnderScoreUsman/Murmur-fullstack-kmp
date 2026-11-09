package com.example.chatappkross.auth.data

import com.example.chatappkross.auth.data.common.AuthException
import com.example.chatappkross.auth.data.common.toDto
import com.example.chatappkross.auth.domain.AuthService
import com.example.chatappkross.auth.domain.UserService
import com.example.chatappkross.auth.domain.model.UserRecord
import com.example.chatappkross.model.AuthResponse
import com.example.chatappkross.model.AuthValidator
import com.example.chatappkross.model.ErrorCode
import com.example.chatappkross.model.LoginRequest
import com.example.chatappkross.model.RegisterRequest
import io.ktor.http.HttpStatusCode
import java.util.UUID

class AuthServiceImpl(private val users: UserService): AuthService {

    override suspend fun register(request: RegisterRequest): AuthResponse {
        AuthValidator.validateUsername(request.username)?.let { invalidInput(it) }
        AuthValidator.validatePassword(request.password)?.let { invalidInput(it) }

        val user = UserRecord(
            id = UUID.randomUUID().toString(),
            username = request.username.trim(),
            password = request.password,
        )
        if (!users.create(user)) {
            throw AuthException(
                ErrorCode.USERNAME_TAKEN,
                "Username is already taken",
                HttpStatusCode.Conflict
            )
        }
        return AuthResponse(user.toDto())
    }

   override suspend fun login(request: LoginRequest): AuthResponse {
        val user = users.findByUsername(request.username.trim())
        // Same message for unknown user and wrong password
        if (user == null || user.password != request.password) {
            throw AuthException(ErrorCode.INVALID_CREDENTIALS, "Invalid username or password", HttpStatusCode.Unauthorized)
        }
        return AuthResponse(user.toDto())
    }

    private fun invalidInput(message: String): Nothing =
        throw AuthException(ErrorCode.INVALID_INPUT, message, HttpStatusCode.BadRequest)

}