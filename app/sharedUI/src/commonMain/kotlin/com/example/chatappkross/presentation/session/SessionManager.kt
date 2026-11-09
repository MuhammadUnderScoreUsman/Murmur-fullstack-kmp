package com.example.chatappkross.presentation.session

import com.example.chatappkross.auth.domain.AuthRepository
import com.example.chatappkross.model.UserDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val user: UserDto) : SessionState
}

class SessionManager(
    private val repository: AuthRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    // for Splash
    private val minLoadingMillis: Long = 2000

    init {
        scope.launch {
            val saved = repository.restoreSession()

            //for splash only
            delay(minLoadingMillis.milliseconds)
            _state.value = if (saved != null) SessionState.LoggedIn(saved) else SessionState.LoggedOut
        }
    }

    fun onAuthenticated(user: UserDto) {
        _state.value = SessionState.LoggedIn(user)
    }

    fun logout() {
        scope.launch {
            repository.logout()
            _state.value = SessionState.LoggedOut
        }
    }
}
