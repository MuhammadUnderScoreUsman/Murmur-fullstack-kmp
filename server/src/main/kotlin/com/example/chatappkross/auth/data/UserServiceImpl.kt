package com.example.chatappkross.auth.data

import com.example.chatappkross.auth.domain.model.UserRecord
import com.example.chatappkross.auth.domain.UserService
import java.util.concurrent.ConcurrentHashMap

class UserServiceImpl : UserService {

    private val byId = ConcurrentHashMap<String, UserRecord>()
    private val idByUsername = ConcurrentHashMap<String, String>()

    override suspend fun create(user: UserRecord): Boolean {
        if (idByUsername.putIfAbsent(user.username.lowercase(), user.id) != null) return false
        byId[user.id] = user
        return true
    }

    override suspend fun findByUsername(username: String): UserRecord? =
        idByUsername[username.lowercase()]?.let { byId[it] }

    override suspend fun findById(id: String): UserRecord? = byId[id]

    override suspend fun listAll(): List<UserRecord> = byId.values.toList()

    override suspend fun sortedViaName(): List<UserRecord> {
        return listAll().sortedBy { it.username }
    }
}
