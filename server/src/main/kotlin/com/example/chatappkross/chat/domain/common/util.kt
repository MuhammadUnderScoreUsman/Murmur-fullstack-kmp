package com.example.chatappkross.chat.domain.common

import com.example.chatappkross.auth.data.common.AuthException
import com.example.chatappkross.chat.domain.model.ConversationRecord
import com.example.chatappkross.chat.domain.service.ConversationService
import com.example.chatappkross.model.ErrorCode
import io.ktor.http.HttpStatusCode


suspend fun ConversationService.requireMember(userId: String, conversationId: String): ConversationRecord {
    val conversation = this.findById(conversationId) ?: throw notFound("Conversation not found")
    if (userId !in conversation.participantIds) throw forbidden("You are not part of this conversation")
    return conversation
}

fun invalid(message: String) = ChatException(ErrorCode.INVALID_INPUT, message, HttpStatusCode.BadRequest)
fun notFound(message: String) = ChatException(ErrorCode.NOT_FOUND, message, HttpStatusCode.NotFound)
private fun forbidden(message: String) = ChatException(ErrorCode.FORBIDDEN, message, HttpStatusCode.Forbidden)


typealias ChatException = AuthException
