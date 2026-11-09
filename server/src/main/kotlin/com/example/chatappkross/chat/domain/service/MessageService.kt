package com.example.chatappkross.chat.domain.service

import com.example.chatappkross.chat.domain.common.AppendResult
import com.example.chatappkross.chat.domain.model.MessageRecord


interface MessageService {
    suspend fun append(message: MessageRecord): AppendResult
    suspend fun history(conversationId: String, beforeMessageId: String?, limit: Int): List<MessageRecord>
    suspend fun last(conversationId: String): MessageRecord?
}
