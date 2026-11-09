package com.example.chatappkross.presentation.common

import com.example.chatappkross.chat.data.client.RealtimeClient
import com.example.chatappkross.presentation.session.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch


class RealtimeController(
    realtimeClient: RealtimeClient,
    sessionState: StateFlow<SessionState>,
    isForeground: StateFlow<Boolean>,
    scope: CoroutineScope,
) {
    init {
        scope.launch {
            combine(sessionState, isForeground) { session, foreground ->
                if (foreground) (session as? SessionState.LoggedIn)?.user?.id else null
            }
                .distinctUntilChanged()
                .collect { userId ->
                    if (userId != null) realtimeClient.connect(userId) else realtimeClient.disconnect()
                }
        }
    }
}