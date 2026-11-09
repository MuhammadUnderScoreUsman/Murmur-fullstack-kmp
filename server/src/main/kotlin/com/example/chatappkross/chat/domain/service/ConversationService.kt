package com.example.chatappkross.chat.domain.service

import com.example.chatappkross.chat.domain.model.ConversationRecord


interface ConversationService {
    suspend fun getOrCreateDirect(userA: String, userB: String, now: Long): ConversationRecord
    suspend fun findById(id: String): ConversationRecord?
    suspend fun listForUser(userId: String): List<ConversationRecord>
}
