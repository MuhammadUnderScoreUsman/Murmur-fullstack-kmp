package com.example.chatappkross.chat.domain.usecase

import com.example.chatappkross.chat.data.toDto
import com.example.chatappkross.chat.domain.common.requireMember
import com.example.chatappkross.chat.domain.service.ConversationService
import com.example.chatappkross.chat.domain.service.MessageService
import com.example.chatappkross.model.MessageDto

class HistoryUseCase(
    private val messages: MessageService,
    private val conversations: ConversationService,
) {


    suspend operator fun invoke(callerId: String, conversationId: String, beforeMessageId: String?, limit: Int): List<MessageDto> {
        conversations.requireMember(callerId, conversationId)
        return messages.history(conversationId, beforeMessageId, limit.coerceIn(1, MAX_PAGE_SIZE)).map { it.toDto() }
    }
    private companion object {
        const val MAX_PAGE_SIZE = 100
    }
}