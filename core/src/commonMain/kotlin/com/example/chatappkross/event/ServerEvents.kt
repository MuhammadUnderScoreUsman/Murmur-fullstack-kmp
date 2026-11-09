package com.example.chatappkross.event

import com.example.chatappkross.model.ErrorCode
import com.example.chatappkross.model.MessageDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ---------- Server -> client ----------

@Serializable
sealed interface ServerEvent

@Serializable
@SerialName("presence_snapshot")
data class PresenceSnapshot(val onlineUserIds: List<String>) : ServerEvent

/** Sent whenever someone goes online or offline after that. */
@Serializable
@SerialName("presence_changed")
data class PresenceChanged(val userId: String, val isOnline: Boolean) : ServerEvent

/**
 * A message was stored. Sent to every open connection of everyone in the conversation, including
 * the sender's: that echo is the confirmation that the send worked (match it by message id).
 */
@Serializable
@SerialName("message_received")
data class MessageReceived(val message: MessageDto) : ServerEvent

/** A send was refused. [messageId] says which one, or is null if the frame itself wasn't understood. */
@Serializable
@SerialName("chat_error")
data class ChatError(
    val messageId: String? = null,
    val code: ErrorCode,
    val message: String,
) : ServerEvent

// ---------- Client -> server ----------

@Serializable
sealed interface ClientEvent

/** The sender isn't included: the server knows who is connected. */
@Serializable
@SerialName("send_message")
data class SendMessage(
    val messageId: String,
    val conversationId: String,
    val text: String,
) : ClientEvent

// ---------- Encoding ----------

/** The one Json configuration for WebSocket frames, used by the server and the clients. */
val ChatJson: Json = Json { ignoreUnknownKeys = true }

/**
 * Encode and decode live here, in the module that has the serialization plugin,
 * so the server and the apps never need to reference serializers themselves.
 * Wire format: {"type":"send_message","messageId":"...","conversationId":"...","text":"hi"}
 */
fun encodeServerEvent(event: ServerEvent): String =
    ChatJson.encodeToString(ServerEvent.serializer(), event)

/** Throws an IllegalArgumentException (SerializationException) if the text isn't a known event. */
fun decodeServerEvent(text: String): ServerEvent =
    ChatJson.decodeFromString(ServerEvent.serializer(), text)

fun encodeClientEvent(event: ClientEvent): String =
    ChatJson.encodeToString(ClientEvent.serializer(), event)

/** Throws an IllegalArgumentException (SerializationException) if the text isn't a known event. */
fun decodeClientEvent(text: String): ClientEvent =
    ChatJson.decodeFromString(ClientEvent.serializer(), text)
