package com.example.chatappkross.auth.domain

import com.example.chatappkross.model.UserDto

interface AuthRepository {
    suspend fun restoreSession(): UserDto?
    suspend fun register(username: String, password: String): AuthResult
    suspend fun login(username: String, password: String): AuthResult
    suspend fun logout()
}
