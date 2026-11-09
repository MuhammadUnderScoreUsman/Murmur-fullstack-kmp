package com.example.chatappkross.chat.data

import com.example.chatappkross.chat.domain.model.MessageRecord
import com.example.chatappkross.model.MessageDto


fun MessageRecord.toDto() = MessageDto(id, conversationId, senderId, text, createdAt)
