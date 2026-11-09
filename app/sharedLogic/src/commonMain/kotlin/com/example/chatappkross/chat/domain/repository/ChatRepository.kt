package com.example.chatappkross.chat.domain.repository

import com.example.chatappkross.chat.domain.ChatState
import com.example.chatappkross.chat.domain.util.ConversationResults
import com.example.chatappkross.chat.domain.util.HistoryResult
import com.example.chatappkross.chat.domain.util.OpenConversationResult
import kotlinx.coroutines.flow.StateFlow

//interface ChatRepository {
//    suspend fun conversations(): ConversationResults
//    suspend fun openConversation(userId: String): OpenConversationResult
//    suspend fun history(conversationId: String, beforeMessageId: String?, limit: Int): HistoryResult
//}



interface ChatRepository {
    val state: StateFlow<ChatState>

    fun refreshConversations()

    /** Start or reopen the chat with someone (for example after tapping them in the contact list). */
    suspend fun openConversation(userId: String): OpenConversationResult

    /** Loads the newest messages. Safe to call again: it merges, so nothing shown is lost or doubled. */
    fun loadThread(conversationId: String)

    /** Loads the page before the oldest confirmed message. Does nothing if one is already loading or there is no more. */
    fun loadOlder(conversationId: String)

    /**
     * Shows the message at once as SENDING and sends it. It becomes SENT when the server confirms, or FAILED.
     * Returns false (and does nothing) if the text is blank or too long, or nobody is logged in.
     */
    fun send(conversationId: String, text: String): Boolean

    /** Sends a FAILED message again with the same id, so the server never stores it twice. */
    fun retry(conversationId: String, messageId: String)

    /** Removes a FAILED message the user doesn't want to retry. */
    fun discard(conversationId: String, messageId: String)

    fun setUser(userId: String?)
}
