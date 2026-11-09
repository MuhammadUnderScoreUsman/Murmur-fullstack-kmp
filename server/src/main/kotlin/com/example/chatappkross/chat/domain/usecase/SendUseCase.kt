package com.example.chatappkross.chat.domain.usecase

import com.example.chatappkross.chat.data.toDto
import com.example.chatappkross.chat.domain.common.AppendResult
import com.example.chatappkross.chat.domain.common.invalid
import com.example.chatappkross.chat.domain.common.requireMember
import com.example.chatappkross.chat.domain.model.MessageRecord
import com.example.chatappkross.chat.domain.service.ConversationService
import com.example.chatappkross.chat.domain.service.MessageService
import com.example.chatappkross.chat.presentation.realtime.Connection
import com.example.chatappkross.chat.presentation.realtime.PresenceRegistry
import com.example.chatappkross.event.MessageReceived
import com.example.chatappkross.event.SendMessage
import com.example.chatappkross.model.ChatValidator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SendUseCase(
    private val conversation: ConversationService,
    private val messages: MessageService,
    private val registry: PresenceRegistry,
    private val clock: () -> Long = System::currentTimeMillis,

) {

    private val sendLock = Mutex()

    suspend operator fun invoke(senderId: String, origin: Connection, request: SendMessage) {
        val text = request.text.trim()
        ChatValidator.validateMessageId(request.messageId)?.let { throw invalid(it) }
        ChatValidator.validateMessageText(text)?.let { throw invalid(it) }
        val conversation = conversation.requireMember(senderId, request.conversationId)

        sendLock.withLock {
            val record = MessageRecord(
                id = request.messageId,
                conversationId = conversation.id,
                senderId = senderId,
                text = text,
                createdAt = clock(),
            )
            when (val result = messages.append(record)) {
                is AppendResult.Created -> {
                    // Everyone in the conversation, on every device, including the sender (their confirmation)
                    val event = MessageReceived(result.message.toDto())
                    conversation.participantIds.forEach { registry.sendToUser(it, event) }
                }

                is AppendResult.Duplicate -> {
                    if (result.existing.senderId != senderId) throw invalid("Message id is already used")
                    // A retry of something already stored: confirm to the retrying connection only
                    registry.send(origin, MessageReceived(result.existing.toDto()))
                }
            }
        }
    }

}