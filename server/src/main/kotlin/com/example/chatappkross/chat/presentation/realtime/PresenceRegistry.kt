package com.example.chatappkross.chat.presentation.realtime

import com.example.chatappkross.event.PresenceChanged
import com.example.chatappkross.event.PresenceSnapshot
import com.example.chatappkross.event.ServerEvent
import kotlinx.coroutines.channels.Channel
import kotlin.collections.iterator


class Connection(val userId: String) {
    val outbound = Channel<ServerEvent>(capacity = 64)
}


class PresenceRegistry {
    private val lock = Any()
    private val connections = HashMap<String, MutableList<Connection>>() // userId -> open connections

    fun connect(connection: Connection) {
        synchronized(lock) {
            val mine = connections.getOrPut(connection.userId) { mutableListOf() }
            val cameOnline = mine.isEmpty()
            mine += connection

            // Queued first, so the snapshot is the first thing this connection receives
            deliver(connection, PresenceSnapshot(connections.keys.sorted()))
            if (cameOnline) {
                broadcast(PresenceChanged(connection.userId, isOnline = true), exceptUserId = connection.userId)
            }
        }
    }

    
    fun disconnect(connection: Connection) {
        connection.outbound.close()
        synchronized(lock) {
            val mine = connections[connection.userId] ?: return
            if (!mine.remove(connection)) return
            if (mine.isEmpty()) {
                connections.remove(connection.userId)
                broadcast(PresenceChanged(connection.userId, isOnline = false), exceptUserId = connection.userId)
            }
        }
    }

    fun onlineUserIds(): Set<String> = synchronized(lock) { connections.keys.toSet() }

    
    fun send(connection: Connection, event: ServerEvent) {
        deliver(connection, event)
    }

    
    fun sendToUser(userId: String, event: ServerEvent) {
        synchronized(lock) {
            connections[userId]?.forEach { deliver(it, event) }
        }
    }

    private fun broadcast(event: ServerEvent, exceptUserId: String) {
        for ((userId, userConnections) in connections) {
            if (userId == exceptUserId) continue
            userConnections.forEach { deliver(it, event) }
        }
    }

    private fun deliver(connection: Connection, event: ServerEvent) {
        // trySend never suspends. A full buffer means the client isn't reading: close its queue,
        // which makes the route close the socket. The app reconnects and gets a fresh snapshot.
        if (connection.outbound.trySend(event).isFailure) connection.outbound.close()
    }
}