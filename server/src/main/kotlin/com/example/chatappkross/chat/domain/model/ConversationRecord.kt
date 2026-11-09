package com.example.chatappkross.chat.domain.model


data class ConversationRecord(
    val id: String,
    val participantIds: List<String>,
    val createdAt: Long,
)