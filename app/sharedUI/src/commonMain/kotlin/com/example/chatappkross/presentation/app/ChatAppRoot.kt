package com.example.chatappkross.presentation.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.chatappkross.presentation.chatting.chat.ChatViewModel
import com.example.chatappkross.presentation.session.SessionState
import com.example.chatappkross.model.UserDto
import com.example.chatappkross.presentation.SplashGate
import com.example.chatappkross.presentation.SplashScreen
import com.example.chatappkross.presentation.auth.login.LoginRoute
import com.example.chatappkross.presentation.auth.register.RegisterRoute
import com.example.chatappkross.presentation.chatting.chat.ChatRoute
import com.example.chatappkross.presentation.chatting.list.ChatListRoute
import com.example.chatappkross.presentation.chatting.list.ChatListViewModel
import com.example.chatappkross.presentation.common.ViewModelFactory
import com.example.chatappkross.presentation.common.chatViewModelFactory
import kotlinx.serialization.Serializable

private const val ROUTE_LOGIN = "login"
private const val ROUTE_REGISTER = "register"

@Serializable
private data object HomeDestination

@Serializable
private data class ChatDestination(
    val conversationId: String,
    val peerId: String,
    val peerUsername: String,
)


@Composable
fun ChatAppRoot(container: AppContainer) {
    val factory = remember(container) { ViewModelFactory(container) }
    val session by container.sessionManager.state.collectAsStateWithLifecycle()

    when (val current = session) {
        SessionState.Loading -> SplashScreen()


        SessionState.LoggedOut -> {
            AuthNavHost(factory)
        }

        is SessionState.LoggedIn -> key(current.user.id) {
            LoggedInNavHost(user = current.user, container = container, factory = factory)
        }
    }
}

@Composable
private fun LoggedInNavHost(user: UserDto, container: AppContainer, factory: ViewModelFactory) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeDestination) {
        composable<HomeDestination> {
            ChatListRoute(
                user = user,
                viewModel = viewModel<ChatListViewModel>(factory = factory),
                onOpenChat = { conversationId, peerId, peerUsername ->
                    navController.navigate(ChatDestination(conversationId, peerId, peerUsername))
                },
            )
        }
        composable<ChatDestination> { entry ->
            val chat = entry.toRoute<ChatDestination>()
            ChatRoute(
                viewModel = viewModel<ChatViewModel>(
                    factory = chatViewModelFactory(
                        container.shared,
                        user.id,
                        chat.conversationId,
                        chat.peerId,
                        chat.peerUsername
                    ),
                ),
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun AuthNavHost(factory: ViewModelFactory) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ROUTE_LOGIN) {
        composable(ROUTE_LOGIN) {
            LoginRoute(
                viewModel = viewModel(factory = factory),
                onNavigateToRegister = { navController.navigate(ROUTE_REGISTER) },
            )
        }
        composable(ROUTE_REGISTER) {
            RegisterRoute(
                viewModel = viewModel(factory = factory),
                onNavigateToLogin = { navController.popBackStack() },
            )
        }
    }
}