package com.example.chatappkross.presentation.auth.register

import androidx.lifecycle.ViewModel
import com.example.chatappkross.auth.presentation.RegisterUiState
import com.example.chatappkross.auth.presentation.SharedRegisterViewModel
import kotlinx.coroutines.flow.StateFlow

class RegisterViewModel(private val shared: SharedRegisterViewModel) : ViewModel() {
    val state: StateFlow<RegisterUiState> = shared.state

    fun onUsernameChange(value: String) = shared.onUsernameChange(value)
    fun onPasswordChange(value: String) = shared.onPasswordChange(value)
    fun onConfirmPasswordChange(value: String) = shared.onConfirmPasswordChange(value)
    fun register() = shared.register()

    override fun onCleared() = shared.clear()
}
