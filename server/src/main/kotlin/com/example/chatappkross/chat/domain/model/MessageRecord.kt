package com.example.chatappkross.chat.domain.model

data class MessageRecord(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val createdAt: Long,
)