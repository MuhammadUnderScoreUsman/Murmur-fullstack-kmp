package com.example.chatappkross.auth.domain

import com.example.chatappkross.auth.domain.model.UserRecord

interface UserService {

    suspend fun create(user: UserRecord): Boolean
    suspend fun findByUsername(username: String): UserRecord?
    suspend fun findById(id: String): UserRecord?
    suspend fun listAll(): List<UserRecord>
    suspend fun sortedViaName(): List<UserRecord>

}
