package com.example.chatappkross.chat.domain.realtime

import com.example.chatappkross.event.PresenceChanged
import com.example.chatappkross.event.PresenceSnapshot
import com.example.chatappkross.event.ServerEvent

enum class ConnectionState {
    /** No user, or waiting before the next retry. */
    DISCONNECTED,

    /** Opening the socket. */
    CONNECTING,

    /** Socket is open. Presence arrives right after (the snapshot). */
    CONNECTED,
}

/**
 * Everything the UI needs from the live connection, as one value.
 * [onlineUserIds] includes the current user. It is empty whenever the connection is not
 * [ConnectionState.CONNECTED], so a dropped connection never leaves stale green dots.
 */
data class RealtimeState(
    val connection: ConnectionState = ConnectionState.DISCONNECTED,
    val onlineUserIds: Set<String> = emptySet(),
) {
    fun isOnline(userId: String): Boolean = userId in onlineUserIds
}

/** Pure function: easy to unit test, no sockets involved. */
internal fun RealtimeState.reduce(event: ServerEvent): RealtimeState = when (event) {
    is PresenceSnapshot -> copy(onlineUserIds = event.onlineUserIds.toSet())
    is PresenceChanged -> copy(
        onlineUserIds = if (event.isOnline) onlineUserIds + event.userId else onlineUserIds - event.userId,
    )
    else -> this

}
