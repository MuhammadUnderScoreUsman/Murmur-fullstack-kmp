package com.example.chatappkross.chat.data.client

import com.example.chatappkross.event.ClientEvent
import com.example.chatappkross.event.ServerEvent
import com.example.chatappkross.event.decodeServerEvent
import com.example.chatappkross.event.encodeClientEvent
import com.example.chatappkross.chat.domain.realtime.ConnectionState
import com.example.chatappkross.chat.domain.realtime.RealtimeState
import com.example.chatappkross.chat.domain.realtime.reduce
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.parameter
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random

/**
 * Owns the one WebSocket to the server. It turns presence into [state], hands every event the server
 * sends to [events], and sends the app's own events with [send].
 *
 * Usage: call [connect] after login (or when a saved session is restored) and [disconnect] on logout.
 * Both are safe to call repeatedly and from any thread. While a user is set, the client reconnects
 * by itself with a growing delay.
 *
 * Android: collect [state] directly. iOS: use [watch].
 */
class RealtimeClient(
    private val client: HttpClient,
    /** "ws://host:8080/" (use toWebSocketBaseUrl to derive it from the http base URL). */
    private val webSocketBaseUrl: String,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(RealtimeState())
    val state: StateFlow<RealtimeState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ServerEvent>(extraBufferCapacity = EVENT_BUFFER)

    /**
     * Every event the server sends, in order. Live only: someone who isn't collecting misses what
     * arrives in the meantime, which is why ChatRepositoryImpl collects from the start and reloads after reconnects.
     */
    internal val events: SharedFlow<ServerEvent> = _events.asSharedFlow()

    /** Where [send] puts events. Only exists while a socket is open. */
    private val outbox = MutableStateFlow<SendChannel<ClientEvent>?>(null)

    /** Who we should be connected as. null = stay disconnected. */
    private val wantedUserId = MutableStateFlow<String?>(null)

    init {
        scope.launch {
            // collectLatest cancels the previous connection (and waits for it to close) before
            // starting the next one, so switching users or disconnecting can't overlap sockets.
            wantedUserId.collectLatest { userId ->
                if (userId == null) {
                    _state.value = RealtimeState()
                } else {
                    keepConnected(userId)
                }
            }
        }
    }

    fun connect(userId: String) {
        wantedUserId.value = userId // same id again is a no-op
    }

    fun disconnect() {
        wantedUserId.value = null
    }

    /**
     * Queues an event for the server. Returns false right away if the socket isn't open (nothing is
     * queued), so the caller can show a failure instead of waiting. True only means "handed to the
     * socket": the server's reply (for a message, `message_received` or `chat_error`) is the real answer.
     */
    internal fun send(event: ClientEvent): Boolean {
        val queue = outbox.value ?: return false
        return queue.trySend(event).isSuccess
    }

    /**
     * For Swift: calls [onState] now and on every change, on the main thread.
     * Keep the handle and call dispose() when the view model goes away.
     */
    fun watch(onState: (RealtimeState) -> Unit): DisposableHandle {
        val job = scope.launch(Dispatchers.Main) { state.collect { onState(it) } }
        return DisposableHandle { job.cancel() }
    }

    /** Stops everything for good. Call when the whole app container is torn down. */
    fun close() {
        scope.cancel()
    }

    // ---- connection loop ----

    private enum class Outcome { COULD_NOT_CONNECT, DROPPED, REJECTED }

    private suspend fun keepConnected(userId: String) {
        var failures = 0
        while (true) {
            _state.value = RealtimeState(ConnectionState.CONNECTING)
            val outcome = runOneSession(userId)

            when (outcome) {
                // The server refused this user (unknown id). Retrying can't help.
                Outcome.REJECTED -> {
                    _state.value = RealtimeState()
                    return
                }
                // It worked and then dropped: start the backoff over
                Outcome.DROPPED -> failures = 0
                Outcome.COULD_NOT_CONNECT -> Unit
            }

            _state.value = RealtimeState(ConnectionState.DISCONNECTED)
            delay(backoffMillis(failures))
            if (outcome == Outcome.COULD_NOT_CONNECT) failures++
        }
    }

    /** Opens the socket, reads until it ends, and reports why it ended. Never throws except for cancellation. */
    private suspend fun runOneSession(userId: String): Outcome {
        val session = try {
            // Time limit applies to the handshake only, not to the life of the socket
            withTimeoutOrNull(CONNECT_TIMEOUT_MILLIS) {
                client.webSocketSession(urlString = "${webSocketBaseUrl}ws") {
                    parameter("userId", userId)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (session == null) return Outcome.COULD_NOT_CONNECT

        try {
            coroutineScope {
                // One writer, so events leave in the order they were queued
                val queue = Channel<ClientEvent>(OUTBOX_CAPACITY)
                outbox.value = queue
                _state.value = RealtimeState(ConnectionState.CONNECTED)

                val writer = launch {
                    for (event in queue) session.send(Frame.Text(encodeClientEvent(event)))
                }
                try {
                    for (frame in session.incoming) {
                        if (frame is Frame.Text) handle(frame.readText())
                    }
                } finally {
                    outbox.value = null
                    queue.close()
                    writer.cancel()
                }
            }
            // The server closed the socket normally. Find out whether it refused us.
            val reason = withTimeoutOrNull(2_000) { session.closeReason.await() }
            return if (reason?.knownReason == CloseReason.Codes.VIOLATED_POLICY) Outcome.REJECTED else Outcome.DROPPED
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Outcome.DROPPED // network error mid-session
        } finally {
            // NonCancellable so the socket is still closed when we're being cancelled
            withContext(NonCancellable) { session.close() }
        }
    }

    private suspend fun handle(text: String) {
        val event = try {
            decodeServerEvent(text)
        } catch (e: IllegalArgumentException) {
            return // an event this app version doesn't know: ignore it
        }
        _state.update { it.reduce(event) }
        _events.emit(event)
    }

    /** 1s, 2s, 4s ... capped at 30s, plus a little randomness so many clients don't retry in lockstep. */
    private fun backoffMillis(failures: Int): Long {
        val base = (1_000L shl failures.coerceAtMost(5)).coerceAtMost(30_000L)
        return base + Random.nextLong(500)
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 10_000L
        const val OUTBOX_CAPACITY = 64
        const val EVENT_BUFFER = 128
    }
}