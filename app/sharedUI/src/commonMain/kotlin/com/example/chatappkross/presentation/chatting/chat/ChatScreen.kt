package com.example.chatappkross.presentation.chatting.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chatappkross.chat.domain.SendStatus
import com.example.chatappkross.chat.presentation.ChatUiState
import com.example.chatappkross.chat.presentation.MessageUi
import com.example.chatappkross.presentation.chatting.component.AvatarWithStatus
import com.example.chatappkross.presentation.chatting.component.ConnectionBanner
import com.example.chatappkross.presentation.chatting.component.MessageState
import com.example.chatappkross.presentation.chatting.component.OnlineGreen

private const val MAX_MESSAGE_LENGTH = 2000

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        onBack = onBack,
        onSend = viewModel::send,
        onRetry = viewModel::retry,
        onDiscard = viewModel::discard,
        onLoadOlder = viewModel::loadOlder,
        onReload = viewModel::reload,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onBack: () -> Unit,
    onSend: (String) -> Boolean,
    onRetry: (String) -> Unit,
    onDiscard: (String) -> Unit,
    onLoadOlder: () -> Unit,
    onReload: () -> Unit,
) {

    val errorMessage = remember { mutableStateOf(state.errorMessage) }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AvatarWithStatus(name = state.peerUsername, isOnline = state.peerOnline, size = 36.dp)
                        Column {
                            Text(state.peerUsername, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (state.peerOnline) "Online" else "Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.peerOnline) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        // consumeWindowInsets + imePadding: the input bar rides on top of the keyboard
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            ConnectionBanner(state.connection)

            if (state.errorMessage != null && state.messages.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            state.errorMessage ?: "UnknownError",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onReload) { Text("Retry") }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading && state.messages.isEmpty() ->
                        CircularProgressIndicator(Modifier.align(Alignment.Center))

                    state.errorMessage != null && state.messages.isEmpty() ->
                        MessageState(
                            title = state.errorMessage ?: "UnknownError",
                            action = { Button(onClick = onReload) { Text("Retry") } },
                        )

                    state.messages.isEmpty() ->
                        MessageState(
                            title = "No messages yet",
                            body = "Say hello to @${state.peerUsername}.",
                        )

                    else -> MessageList(
                        state = state,
                        onRetry = onRetry,
                        onDiscard = onDiscard,
                        onLoadOlder = onLoadOlder,
                    )
                }
            }

            MessageInput(onSend = onSend)
        }
    }
}

@Composable
private fun MessageList(
    state: ChatUiState,
    onRetry: (String) -> Unit,
    onDiscard: (String) -> Unit,
    onLoadOlder: () -> Unit,
) {
    // Newest first + reverseLayout: the newest message sits at the bottom and the list opens there
    val newestFirst = remember(state.messages) { state.messages.asReversed() }
    val listState = rememberLazyListState()

    // Reached the top of what is loaded: ask for the previous page (the client ignores it if there is none)
    val nearTop by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearTop, newestFirst.size) {
        if (nearTop) onLoadOlder()
    }

    // Follow new messages, but don't yank the view if the user has scrolled up to read; their own sends always scroll
    val newest = newestFirst.first()
    LaunchedEffect(newest.id) {
        if (newest.isMine || listState.firstVisibleItemIndex <= 2) listState.animateScrollToItem(0)
    }

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(newestFirst, key = { it.id }) { message ->
            MessageBubble(
                message = message,
                onRetry = { onRetry(message.id) },
                onDiscard = { onDiscard(message.id) },
            )
        }
        if (state.isLoadingOlder) {
            item(key = "loading-older") {
                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: MessageUi,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
) {
    val failed = message.status == SendStatus.FAILED
    val colors = MaterialTheme.colorScheme
    val shape = if (message.isMine) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
    ) {
        Surface(
            shape = shape,
            color = when {
                failed -> colors.errorContainer
                message.isMine -> colors.primaryContainer
                else -> colors.surfaceVariant
            },
            contentColor = when {
                failed -> colors.onErrorContainer
                message.isMine -> colors.onPrimaryContainer
                else -> colors.onSurfaceVariant
            },
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }

        when {
            failed -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = message.failureReason ?: "Not sent",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.error,
                )
                TextButton(onClick = onRetry) { Text("Retry") }
                TextButton(onClick = onDiscard) { Text("Delete") }
            }

            message.status == SendStatus.SENDING -> Text(
                text = "Sending…",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            message.time != null -> Text(
                text = message.time!!,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun MessageInput(onSend: (String) -> Boolean) {
    var text by rememberSaveable { mutableStateOf("") }

    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(MAX_MESSAGE_LENGTH) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message") },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            IconButton(
                enabled = text.isNotBlank(),
                onClick = { if (onSend(text)) text = "" },
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}