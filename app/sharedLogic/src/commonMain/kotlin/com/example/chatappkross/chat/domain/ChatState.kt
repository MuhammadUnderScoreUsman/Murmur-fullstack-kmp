package com.example.chatappkross.chat.domain

import com.example.chatappkross.chat.domain.model.Message

enum class SendStatus {
    SENDING,
    SENT,
    FAILED,
}

data class ThreadState(
    /** Confirmed messages first (in the order the server stored them), then the ones still being sent or failed. */
    val messages: List<Message> = emptyList(),
    /** First load, with nothing to show yet. */
    val isLoading: Boolean = false,
    val isLoadingOlder: Boolean = false,
    /** False once the server has no older messages to give. */
    val hasMoreOlder: Boolean = true,
    val errorMessage: String? = null,
)

/**
 * One chat in the list. Plain values owned by this module, so the apps (iOS included) never need
 * the `core` wire types: ConversationDto and MessageDto stay inside the shared code.
 */
data class ConversationItem(
    val id: String,
    val peerId: String,
    val peerUsername: String,
    /** Always SENT: a conversation's last message is one the server has stored. */
    val lastMessage: Message? = null,
)

data class ConversationsState(
    /** Only conversations that have messages, most recent first. */
    val items: List<ConversationItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class ChatState(
    val conversations: ConversationsState = ConversationsState(),
    /** Threads the user has opened, by conversation id. Use [thread] to read one. */
    val threads: Map<String, ThreadState> = emptyMap(),
) {
    fun thread(conversationId: String): ThreadState = threads[conversationId] ?: ThreadState()
}