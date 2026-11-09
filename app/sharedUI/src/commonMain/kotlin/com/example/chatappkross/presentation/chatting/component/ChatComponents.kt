package com.example.chatappkross.presentation.chatting.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.chatappkross.chat.domain.realtime.ConnectionState

// Small pieces used by both the contact list and the chat screen.

internal val OnlineGreen = Color(0xFF34C759)


@Composable
internal fun ConnectionBanner(connection: ConnectionState) {
    if (connection == ConnectionState.CONNECTED) return
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (connection == ConnectionState.CONNECTING) "Connecting…" else "Not connected. Retrying…",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp),
        )
    }
}

@Composable
internal fun AvatarWithStatus(name: String, isOnline: Boolean, size: Dp = 44.dp) {
    Box {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        if (isOnline) {
            // Outer circle in the surface colour makes a ring around the green dot
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size / 2.75f)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(size / 14)
                    .background(OnlineGreen, CircleShape),
            )
        }
    }
}

@Composable
internal fun MessageState(
    title: String,
    body: String? = null,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (body != null) {
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        action?.invoke()
    }
}
