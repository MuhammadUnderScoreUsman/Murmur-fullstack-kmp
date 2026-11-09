package com.example.chatappkross.presentation.auth.register

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chatappkross.auth.presentation.RegisterUiState
import com.example.chatappkross.presentation.auth.component.AuthLayout
import com.example.chatappkross.presentation.auth.component.ErrorText
import com.example.chatappkross.presentation.auth.component.LoadingButton
import com.example.chatappkross.presentation.auth.component.PasswordField
import com.example.chatappkross.presentation.auth.component.UsernameField

@Composable
fun RegisterRoute(
    viewModel: RegisterViewModel,
    onNavigateToLogin: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RegisterScreen(
        state = state,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onRegisterClick = viewModel::register,
        onLoginClick = onNavigateToLogin,
    )
}

@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
) {
    AuthLayout(title = "Create account", subtitle = "Pick a username and password") {
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
            imeAction = ImeAction.Next,
        )
        PasswordField(
            value = state.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Confirm password",
            enabled = !state.isLoading,
            imeAction = ImeAction.Done,
            onDone = onRegisterClick,
        )
        ErrorText(state.errorMessage)
        LoadingButton(text = "Create account", isLoading = state.isLoading, onClick = onRegisterClick)
        TextButton(onClick = onLoginClick, enabled = !state.isLoading) {
            Text("Already have an account? Log in")
        }
    }
}
