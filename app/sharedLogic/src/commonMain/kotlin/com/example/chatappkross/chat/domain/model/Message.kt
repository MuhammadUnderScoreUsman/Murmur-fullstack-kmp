package com.example.chatappkross.chat.domain.model

import com.example.chatappkross.chat.domain.SendStatus

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val createdAt: Long,
    val status: SendStatus,
    val failureReason: String? = null
)