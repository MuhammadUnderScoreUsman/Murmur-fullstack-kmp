package com.example.chatappkross.chat.data

import com.example.chatappkross.chat.domain.service.ConversationService
import com.example.chatappkross.chat.domain.model.ConversationRecord
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap


class ConversationServiceImpl : ConversationService {

    private val byId = ConcurrentHashMap<String, ConversationRecord>()
    private val idByPair = ConcurrentHashMap<String, String>()

    override suspend fun getOrCreateDirect(userA: String, userB: String, now: Long): ConversationRecord {
        val pairKey = listOf(userA, userB).sorted().joinToString("|")
        // computeIfAbsent is atomic, so two simultaneous "open chat" calls can't create two conversations
        val id = idByPair.computeIfAbsent(pairKey) {
            val record = ConversationRecord(
                id = UUID.randomUUID().toString(),
                participantIds = listOf(userA, userB),
                createdAt = now,
            )
            byId[record.id] = record
            record.id
        }
        return byId.getValue(id)
    }

    override suspend fun findById(id: String): ConversationRecord? = byId[id]

    override suspend fun listForUser(userId: String): List<ConversationRecord> =
        byId.values.filter { userId in it.participantIds }
}
