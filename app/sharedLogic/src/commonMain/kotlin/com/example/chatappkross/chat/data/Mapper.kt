package com.example.chatappkross.chat.data

import com.example.chatappkross.chat.domain.ChatState
import com.example.chatappkross.chat.domain.ConversationItem
import com.example.chatappkross.chat.domain.ConversationsState
import com.example.chatappkross.chat.domain.SendStatus
import com.example.chatappkross.chat.domain.ThreadState
import com.example.chatappkross.chat.domain.model.Message
import com.example.chatappkross.chat.domain.model.User
import com.example.chatappkross.chat.domain.util.ChatResult
import com.example.chatappkross.model.ConversationDto
import com.example.chatappkross.model.MessageDto
import com.example.chatappkross.model.UserDto
import kotlin.collections.plus

fun MessageDto.fromDto(): Message {
    return Message(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        text = text,
        createdAt = createdAt,
        status = SendStatus.SENT
    )
}


fun Message.toDto(): MessageDto {
    return MessageDto(id, conversationId, senderId, text, createdAt)
}

fun User.toDto(): UserDto {
    return UserDto(id, username)
}

fun UserDto.fromDto(): User {
    return User(id, username)
}

fun ConversationDto.toItem() = ConversationItem(
    id = id,
    peerId = participant.id,
    peerUsername = participant.username,
    lastMessage = lastMessage?.fromDto(),
)


// ---------- reducers ----------


/**
 * Adds messages the server has stored (history pages, or a live message).
 * The same id is never shown twice, the server's version wins, and a message of ours that was still
 * waiting is replaced by its confirmed copy.
 */

internal fun ThreadState.mergeServerMessages(incoming: List<MessageDto>): ThreadState {
    if (incoming.isEmpty()) return this
    val incomingIds = incoming.mapTo(HashSet()) { it.id }
    val confirmed =
        (messages.filter { it.status == SendStatus.SENT && it.id !in incomingIds } + incoming.map { it.fromDto() })
            .sortedBy { it.createdAt } // sortedBy is stable, so messages with the same time keep their order
    val unconfirmed = messages.filter { it.status != SendStatus.SENT && it.id !in incomingIds }
    return copy(messages = confirmed + unconfirmed)
}

internal fun ThreadState.addPending(item: Message): ThreadState =
    copy(messages = messages + item)

/** FAILED -> SENDING (a retry). Anything else is left alone. */
internal fun ThreadState.markSendingAgain(messageId: String): ThreadState = mapMessage(messageId) {
    if (it.status == SendStatus.FAILED) it.copy(
        status = SendStatus.SENDING,
        failureReason = null
    ) else it
}

/** SENDING -> FAILED. A message that was already confirmed can't fail afterwards. */
internal fun ThreadState.failIfSending(messageId: String, reason: String): ThreadState =
    mapMessage(messageId) {
        if (it.status == SendStatus.SENDING) it.copy(
            status = SendStatus.FAILED,
            failureReason = reason
        ) else it
    }

/** Removes a failed message the user gave up on. Confirmed messages are never removed. */
internal fun ThreadState.discardFailed(messageId: String): ThreadState =
    copy(messages = messages.filterNot { it.id == messageId && it.status == SendStatus.FAILED })

private fun ThreadState.mapMessage(
    messageId: String,
    change: (Message) -> Message
): ThreadState =
    copy(messages = messages.map { if (it.id == messageId) change(it) else it })

internal fun ChatState.updateThread(
    conversationId: String,
    change: (ThreadState) -> ThreadState
): ChatState =
    copy(threads = threads + (conversationId to change(thread(conversationId))))

/** A server error names the message it is about, and that message could be in any open thread. */
internal fun ChatState.failPending(messageId: String, reason: String): ChatState =
    copy(threads = threads.mapValues { (_, thread) -> thread.failIfSending(messageId, reason) })

/** Puts the new message on its conversation's row and moves that row to the top. Unknown conversations are left alone. */
internal fun ConversationsState.withLastMessage(message: MessageDto): ConversationsState {
    val current = items.firstOrNull { it.id == message.conversationId } ?: return this
    if ((current.lastMessage?.createdAt
            ?: 0L) > message.createdAt
    ) return this // an older message arrived late
    return copy(items = listOf(current.copy(lastMessage = message.fromDto())) + items.filter { it.id != current.id })
}


/** Changes the data inside a success and passes a failure through untouched. */
internal inline fun <T, R> ChatResult<T>.mapData(transform: (T) -> R): ChatResult<R> = when (this) {
    is ChatResult.Success -> ChatResult.Success(transform(data))
    is ChatResult.Failure -> ChatResult.Failure(message)
}
