package com.example.chatappkross.auth.domain

import com.example.chatappkross.model.AuthResponse
import com.example.chatappkross.model.LoginRequest
import com.example.chatappkross.model.RegisterRequest

interface AuthService  {
    suspend fun register(request: RegisterRequest): AuthResponse
    suspend fun login(request: LoginRequest): AuthResponse
}