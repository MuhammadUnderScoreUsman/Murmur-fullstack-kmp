package com.example.chatappkross.chat.domain.util

import com.example.chatappkross.chat.domain.ConversationItem
import com.example.chatappkross.chat.domain.model.Message
import com.example.chatappkross.chat.domain.model.User

typealias ConversationResults = ChatResult<List<ConversationItem>>
typealias OpenConversationResult = ChatResult<ConversationItem>
typealias HistoryResult = ChatResult<List<Message>>
typealias UserResult = ChatResult<List<User>>

sealed interface ChatResult<out T> {
    data class Success<T>(val data: T) : ChatResult<T>
    data class Failure(val message: String) : ChatResult<Nothing>
}
