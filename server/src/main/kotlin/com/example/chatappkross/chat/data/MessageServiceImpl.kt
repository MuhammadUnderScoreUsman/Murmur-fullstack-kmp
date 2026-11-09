package com.example.chatappkross.chat.data

import com.example.chatappkross.chat.domain.service.MessageService
import com.example.chatappkross.chat.domain.common.AppendResult
import com.example.chatappkross.chat.domain.model.MessageRecord

class MessageServiceImpl : MessageService {
    private val lock = Any()
    private val byConversation = HashMap<String, MutableList<MessageRecord>>() // in the order they were stored
    private val byKey = HashMap<String, MessageRecord>()                       // "conversationId:messageId"

    override suspend fun append(message: MessageRecord): AppendResult = synchronized(lock) {
        val key = "${message.conversationId}:${message.id}"
        val existing = byKey[key]
        if (existing != null) return@synchronized AppendResult.Duplicate(existing)

        byConversation.getOrPut(message.conversationId) { mutableListOf() } += message
        byKey[key] = message
        AppendResult.Created(message)
    }

    override suspend fun history(
        conversationId: String,
        beforeMessageId: String?,
        limit: Int,
    ): List<MessageRecord> = synchronized(lock) {
        val all = byConversation[conversationId] ?: return@synchronized emptyList()
        val end = if (beforeMessageId == null) all.size else all.indexOfFirst { it.id == beforeMessageId }
        if (end < 0) return@synchronized emptyList()
        all.subList((end - limit).coerceAtLeast(0), end).toList()
    }

    override suspend fun last(conversationId: String): MessageRecord? = synchronized(lock) {
        byConversation[conversationId]?.lastOrNull()
    }
}