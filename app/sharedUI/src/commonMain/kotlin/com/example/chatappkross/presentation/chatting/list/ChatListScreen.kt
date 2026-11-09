package com.example.chatappkross.presentation.chatting.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chatappkross.chat.presentation.ChatListUiState
import com.example.chatappkross.chat.presentation.ContactItem
import com.example.chatappkross.model.UserDto
import com.example.chatappkross.presentation.chatting.component.AvatarWithStatus
import com.example.chatappkross.presentation.chatting.component.ConnectionBanner
import com.example.chatappkross.presentation.chatting.component.MessageState
import com.example.chatappkross.presentation.chatting.component.OnlineGreen

@Composable
fun ChatListRoute(
    user: UserDto,
    viewModel: ChatListViewModel,
    onOpenChat: (conversationId: String, peerId: String, peerUsername: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnOpenChat by rememberUpdatedState(onOpenChat)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatListEvent.OpenChat -> currentOnOpenChat(event.conversationId, event.peerId, event.peerUsername)
                is ChatListEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    ChatListScreen(
        user = user,
        state = state,
        snackbarHostState = snackbarHostState,
        onContactClick = viewModel::onContactClick,
        onLogoutClick = viewModel::logout,
        onRetryClick = viewModel::refresh,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    user: UserDto,
    state: ChatListUiState,
    snackbarHostState: SnackbarHostState,
    onContactClick: (ContactItem) -> Unit,
    onLogoutClick: () -> Unit,
    onRetryClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats") },
                actions = { TextButton(onClick = onLogoutClick) { Text("Log out") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ConnectionBanner(state.connection)

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading && state.contacts.isEmpty() ->
                        CircularProgressIndicator(Modifier.align(Alignment.Center))

                    state.errorMessage != null && state.contacts.isEmpty() ->
                        MessageState(
                            title = state.errorMessage?: "UnknownError",
                            action = { Button(onClick = onRetryClick) { Text("Retry") } },
                        )

                    state.contacts.isEmpty() ->
                        MessageState(
                            title = "Signed in as @${user.username}",
                            body = "No one else is here yet. Register another account to see them appear.",
                        )

                    else -> LazyColumn {
                        items(
                            state.contacts,
                            key = { it.id }) { contact ->
                            ContactRow(
                                contact = contact,
                                isOpening = state.openingUserId == contact.id,
                                enabled = state.openingUserId == null,
                                onClick = { onContactClick(contact) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: ContactItem,
    isOpening: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        leadingContent = { AvatarWithStatus(name = contact.username, isOnline = contact.isOnline) },
        headlineContent = { Text(contact.username, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            if (contact.lastMessagePreview != null) {
                Text(
                    text = contact.lastMessagePreview ?: "",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = if (contact.isOnline) "Online" else "Offline",
                    color = if (contact.isOnline) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            when {
                isOpening -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                contact.lastMessageTime != null -> Text(
                    text = contact.lastMessageTime ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}