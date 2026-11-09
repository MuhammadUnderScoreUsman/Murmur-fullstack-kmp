package com.example.chatappkross.auth.presentation

import com.example.chatappkross.auth.domain.AuthRepository
import com.example.chatappkross.auth.domain.AuthResult
import com.example.chatappkross.auth.presentation.common.SharedViewModel
import com.example.chatappkross.auth.presentation.common.vmScope
import com.example.chatappkross.model.AuthValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

/** The registration screen's logic, shared by Compose and SwiftUI. See [com.example.chatappkross.auth.presentation.SharedLoginViewModel] for [onAuthenticated]. */
class SharedRegisterViewModel constructor(
    private val repository: AuthRepository,
    private val onAuthenticated: (userId: String, username: String) -> Unit,
    scope: CoroutineScope = vmScope(),
) : SharedViewModel(scope) {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onUsernameChange(value: String) = _state.update { it.copy(username = value, errorMessage = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, errorMessage = null) }

    fun onConfirmPasswordChange(value: String) = _state.update { it.copy(confirmPassword = value, errorMessage = null) }

    fun register() {
        val current = _state.value
        if (current.isLoading) return

        // The same rules the server enforces (from core), checked first for instant feedback
        val username = current.username.trim()
        val validationError = AuthValidator.validateUsername(username)
            ?: AuthValidator.validatePassword(current.password)
            ?: if (current.password != current.confirmPassword) "Passwords don't match" else null
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }

        _state.update { it.copy(isLoading = true, errorMessage = null) }
        scope.launch {
            when (val result = repository.register(username, current.password)) {
                is AuthResult.Success -> onAuthenticated(result.user.id, result.user.username)
                is AuthResult.Failure ->
                    _state.update { it.copy(isLoading = false, errorMessage = result.failure.message) }
            }
        }
    }

    /** For Swift: calls [onState] now and on every change. Dispose the handle when the screen goes away. */
    fun watch(onState: (RegisterUiState) -> Unit): DisposableHandle = observe(state, onState)
}
