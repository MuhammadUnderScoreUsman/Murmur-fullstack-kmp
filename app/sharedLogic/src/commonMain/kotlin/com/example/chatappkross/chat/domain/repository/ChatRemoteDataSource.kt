package com.example.chatappkross.chat.domain.repository

import com.example.chatappkross.chat.domain.util.ConversationResults
import com.example.chatappkross.chat.domain.util.HistoryResult
import com.example.chatappkross.chat.domain.util.OpenConversationResult

internal interface ChatRemoteDataSource {
    suspend fun conversations(): ConversationResults
    suspend fun openConversation(userId: String): OpenConversationResult
    suspend fun history(conversationId: String, beforeMessageId: String?, limit: Int): HistoryResult
}
