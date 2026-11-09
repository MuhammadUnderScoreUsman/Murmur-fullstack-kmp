package com.example.chatappkross.chat.domain.repository

import com.example.chatappkross.chat.domain.util.UserResult

interface UserRepository {
    suspend fun fetchUsers(): UserResult
}