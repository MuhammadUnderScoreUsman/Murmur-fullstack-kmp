package com.example.chatappkross.auth.presentation

import com.example.chatappkross.auth.domain.AuthRepository
import com.example.chatappkross.auth.domain.AuthResult
import com.example.chatappkross.auth.presentation.common.SharedViewModel
import com.example.chatappkross.auth.presentation.common.vmScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class SharedLoginViewModel  constructor(
    private val repository: AuthRepository,
    private val onAuthenticated: (userId: String, username: String) -> Unit,
    scope: CoroutineScope = vmScope(),
) : SharedViewModel(scope) {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onUsernameChange(value: String) = _state.update { it.copy(username = value, errorMessage = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, errorMessage = null) }

    fun login() {
        val current = _state.value
        if (current.isLoading) return

        val username = current.username.trim()
        if (username.isEmpty() || current.password.isEmpty()) {
            _state.update { it.copy(errorMessage = "Enter your username and password") }
            return
        }

        _state.update { it.copy(isLoading = true, errorMessage = null) }
        scope.launch {
            when (val result = repository.login(username, current.password)) {
                // Stays "loading" on success: the screen is about to be replaced, so the button doesn't flicker back
                is AuthResult.Success -> onAuthenticated(result.user.id, result.user.username)
                is AuthResult.Failure ->
                    _state.update { it.copy(isLoading = false, errorMessage = result.failure.message) }
            }
        }
    }

    /** For Swift: calls [onState] now and on every change. Dispose the handle when the screen goes away. */
    fun watch(onState: (LoginUiState) -> Unit): DisposableHandle = observe(state, onState)
}
