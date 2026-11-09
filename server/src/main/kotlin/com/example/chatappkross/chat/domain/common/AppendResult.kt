package com.example.chatappkross.chat.domain.common

import com.example.chatappkross.chat.domain.model.MessageRecord


sealed interface AppendResult {
    data class Created(val message: MessageRecord) : AppendResult
    data class Duplicate(val existing: MessageRecord) : AppendResult
}
