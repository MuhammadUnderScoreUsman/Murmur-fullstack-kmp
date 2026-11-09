package com.example.chatappkross.chat.domain.usecase

import com.example.chatappkross.auth.data.common.toDto
import com.example.chatappkross.auth.domain.UserService
import com.example.chatappkross.chat.data.toDto
import com.example.chatappkross.chat.domain.service.ConversationService
import com.example.chatappkross.chat.domain.service.MessageService
import com.example.chatappkross.model.ConversationDto

class ConversationUseCase(
    private val conversations: ConversationService,
    private val messages: MessageService,
    private val users: UserService,
) {

    suspend operator fun invoke(callerId: String): List<ConversationDto> =
        conversations.listForUser(callerId)
            .mapNotNull { conversation ->
                val last = messages.last(conversation.id) ?: return@mapNotNull null
                val otherId = conversation.participantIds.firstOrNull { it != callerId } ?: return@mapNotNull null
                val other = users.findById(otherId) ?: return@mapNotNull null
                ConversationDto(conversation.id, other.toDto(), last.toDto())
            }
            .sortedByDescending { it.lastMessage?.createdAt ?: 0L }

}