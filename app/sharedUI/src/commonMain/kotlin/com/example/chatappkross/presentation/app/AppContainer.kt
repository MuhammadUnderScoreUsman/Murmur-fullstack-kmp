package com.example.chatappkross.presentation.app

import com.example.chatappkross.core.SharedContainer
import com.example.chatappkross.presentation.common.AppForegroundObserver
import com.example.chatappkross.presentation.common.RealtimeController
import com.example.chatappkross.presentation.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(baseUrl: String) {

    val shared = SharedContainer(baseUrl)

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val sessionManager = SessionManager(
        repository = shared.authRepository,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    )


    private val foreground = AppForegroundObserver().also { it.register() }

    // Keeps the socket in step with "logged in and visible"
    @Suppress("unused")
    private val realtimeController = RealtimeController(
        realtimeClient = shared.realtimeClient,
        sessionState = sessionManager.state,
        isForeground = foreground.isForeground,
        scope = appScope,
    )
}
