package com.example.chatappkross.auth.data

import com.example.chatappkross.model.UserDto


interface SessionStore {
    suspend fun load(): UserDto?
    suspend fun save(user: UserDto)
    suspend fun clear()
}

class InMemorySessionStore : SessionStore {
    private var user: UserDto? = null

    override suspend fun load(): UserDto? = user
    override suspend fun save(user: UserDto) { this.user = user }
    override suspend fun clear() { user = null }
}
