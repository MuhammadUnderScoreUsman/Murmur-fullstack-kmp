package com.example.chatappkross.model


import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    /** Created by the sending app (a UUID), so a retried send can be recognised and not stored twice. */
    val id: String,
    val conversationId: String,
    /** Always set by the server from who is connected, never taken from what the client claims. */
    val senderId: String,
    val text: String,
    /** Server time, epoch milliseconds. Messages in a conversation are ordered by when the server stored them. */
    val createdAt: Long,
)

@Serializable
data class ConversationDto(
    val id: String,
    /** The other person in this 1:1 conversation, from the point of view of whoever is asking. */
    val participant: UserDto,
    val lastMessage: MessageDto? = null,
)

/** Body of POST /conversations. Opens the chat with this user, or returns the one that already exists. */
@Serializable
data class OpenConversationRequest(val userId: String)

/** Pure Kotlin rules, usable on the server and in both apps. Return an error message, or null if valid. */
object ChatValidator {
    const val MAX_MESSAGE_LENGTH = 2000
    const val MAX_MESSAGE_ID_LENGTH = 64

    fun validateMessageId(id: String): String? = when {
        id.isBlank() -> "Message id is missing"
        id.length > MAX_MESSAGE_ID_LENGTH -> "Message id is too long"
        else -> null
    }

    /** Call with the text already trimmed. */
    fun validateMessageText(text: String): String? = when {
        text.isEmpty() -> "Message can't be empty"
        text.length > MAX_MESSAGE_LENGTH -> "Message is too long (max $MAX_MESSAGE_LENGTH characters)"
        else -> null
    }
}
