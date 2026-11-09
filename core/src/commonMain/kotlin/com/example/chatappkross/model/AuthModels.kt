package com.example.chatappkross.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val username: String, val password: String)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class UserDto(val id: String, val username: String)

@Serializable
data class AuthResponse(val user: UserDto)

@Serializable
enum class ErrorCode {
    INVALID_INPUT,
    USERNAME_TAKEN,
    INVALID_CREDENTIALS,
    INTERNAL,

    // New: used by the chat feature
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
}

@Serializable
data class ApiError(val code: ErrorCode, val message: String)

/** Pure Kotlin rules, usable on the server and in both apps. Returns an error message or null. */
object AuthValidator {
    const val MIN_USERNAME = 3
    const val MAX_USERNAME = 20
    const val MIN_PASSWORD = 6

    private val usernameRegex = Regex("^[a-zA-Z0-9_]+$")

    fun validateUsername(username: String): String? {
        val u = username.trim()
        return when {
            u.length < MIN_USERNAME -> "Username must be at least $MIN_USERNAME characters"
            u.length > MAX_USERNAME -> "Username must be at most $MAX_USERNAME characters"
            !usernameRegex.matches(u) -> "Username may only contain letters, numbers and underscores"
            else -> null
        }
    }

    fun validatePassword(password: String): String? =
        if (password.length < MIN_PASSWORD) "Password must be at least $MIN_PASSWORD characters" else null
}