package com.example.chatappkross.presentation.auth.login

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chatappkross.auth.presentation.LoginUiState
import com.example.chatappkross.presentation.auth.component.AuthLayout
import com.example.chatappkross.presentation.auth.component.ErrorText
import com.example.chatappkross.presentation.auth.component.LoadingButton
import com.example.chatappkross.presentation.auth.component.PasswordField
import com.example.chatappkross.presentation.auth.component.UsernameField


@Composable
fun LoginRoute(
    viewModel: LoginViewModel,
    onNavigateToRegister: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginScreen(
        state = state,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = viewModel::login,
        onRegisterClick = onNavigateToRegister,
    )
}


@Composable
fun LoginScreen(
    state: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
) {
    AuthLayout(title = "Welcome back", subtitle = "Log in to continue") {
        UsernameField(
            value = state.username,
            onValueChange = onUsernameChange,
            enabled = !state.isLoading,
        )
        PasswordField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = "Password",
            enabled = !state.isLoading,
            imeAction = ImeAction.Done,
            onDone = onLoginClick,
        )
        ErrorText(state.errorMessage)
        LoadingButton(text = "Log in", isLoading = state.isLoading, onClick = onLoginClick)
        TextButton(onClick = onRegisterClick, enabled = !state.isLoading) {
            Text("New here? Create an account")
        }
    }
}
