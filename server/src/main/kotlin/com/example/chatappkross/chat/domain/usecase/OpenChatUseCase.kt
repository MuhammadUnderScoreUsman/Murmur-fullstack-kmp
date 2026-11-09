package com.example.chatappkross.chat.domain.usecase

import com.example.chatappkross.auth.data.common.toDto
import com.example.chatappkross.auth.domain.UserService
import com.example.chatappkross.chat.data.toDto
import com.example.chatappkross.chat.domain.common.invalid
import com.example.chatappkross.chat.domain.common.notFound
import com.example.chatappkross.chat.domain.service.ConversationService
import com.example.chatappkross.chat.domain.service.MessageService
import com.example.chatappkross.model.ConversationDto

class OpenChatUseCase(
    private val conversations: ConversationService,
    private val messages: MessageService,
    private val users: UserService,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    suspend operator fun invoke(callerId: String, otherUserId: String): ConversationDto {
        if (otherUserId == callerId) throw invalid("You can't start a chat with yourself")
        val other = users.findById(otherUserId) ?: throw notFound("User not found")
        val conversation = conversations.getOrCreateDirect(callerId, otherUserId, clock())
        return ConversationDto(
            id = conversation.id,
            participant = other.toDto(),
            lastMessage = messages.last(conversation.id)?.toDto(),
        )
    }
}