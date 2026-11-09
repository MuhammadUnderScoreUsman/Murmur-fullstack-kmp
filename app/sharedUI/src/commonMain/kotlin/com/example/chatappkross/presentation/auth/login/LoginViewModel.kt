package com.example.chatappkross.presentation.auth.login

import androidx.lifecycle.ViewModel
import com.example.chatappkross.auth.presentation.LoginUiState
import com.example.chatappkross.auth.presentation.SharedLoginViewModel
import kotlinx.coroutines.flow.StateFlow


class LoginViewModel(private val shared: SharedLoginViewModel) : ViewModel() {
    val state: StateFlow<LoginUiState> = shared.state

    fun onUsernameChange(value: String) = shared.onUsernameChange(value)
    fun onPasswordChange(value: String) = shared.onPasswordChange(value)
    fun login() = shared.login()

    override fun onCleared() = shared.clear()
}
