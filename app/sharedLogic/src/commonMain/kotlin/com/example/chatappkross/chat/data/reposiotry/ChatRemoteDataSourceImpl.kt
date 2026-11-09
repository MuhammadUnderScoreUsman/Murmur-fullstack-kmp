package com.example.chatappkross.chat.data.reposiotry

import com.example.chatappkross.auth.data.SessionStore
import com.example.chatappkross.chat.data.fromDto
import com.example.chatappkross.chat.data.mapData
import com.example.chatappkross.chat.data.toItem
import com.example.chatappkross.chat.domain.repository.ChatRemoteDataSource
import com.example.chatappkross.chat.domain.util.ChatResult
import com.example.chatappkross.chat.domain.util.ConversationResults
import com.example.chatappkross.chat.domain.util.HistoryResult
import com.example.chatappkross.chat.domain.util.OpenConversationResult
import com.example.chatappkross.model.ApiError
import com.example.chatappkross.model.ConversationDto
import com.example.chatappkross.model.MessageDto
import com.example.chatappkross.model.OpenConversationRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

/**
 * The HTTP side of chat: history, conversation list, opening a conversation. Sending messages is not here,
 * it goes over the WebSocket. This is a data source (it speaks the server's language and returns domain models),
 * not the repository: ChatRepositoryImpl is the one that merges it with the live socket.
 *
 * Internal: the apps never see it. It exists as an interface so ChatRepositoryImpl can be tested with a fake.
 */

class ChatRemoteDataSourceImpl(
    private val client: HttpClient,
    private val session: SessionStore,
) : ChatRemoteDataSource {

    override suspend fun conversations(): ConversationResults =
        execute(
            failureText = "Couldn't load chats",
            request = { me -> client.get("conversations") { header(USER_ID_HEADER, me) } },
            parse = { it.body<List<ConversationDto>>() },
        ).mapData { dtos -> dtos.map { it.toItem() } }

    override suspend fun openConversation(userId: String): OpenConversationResult =
        execute(
            failureText = "Couldn't open the chat",
            request = { me ->
                client.post("conversations") {
                    header(USER_ID_HEADER, me)
                    setBody(OpenConversationRequest(userId))
                }
            },
            parse = { it.body<ConversationDto>() },
        ).mapData { it.toItem() }

    override suspend fun history(conversationId: String, beforeMessageId: String?, limit: Int): HistoryResult =
        execute(
            failureText = "Couldn't load messages",
            request = { me ->
                client.get("conversations/$conversationId/messages") {
                    header(USER_ID_HEADER, me)
                    parameter("limit", limit)
                    if (beforeMessageId != null) parameter("before", beforeMessageId)
                }
            },
            parse = { it.body<List<MessageDto>>() },
        ).mapData { dtos -> dtos.map { it.fromDto() } }

    /**
     * Runs one authenticated request. The "who am I" header is added here and nowhere else:
     * when login returns a token, this is the single place to switch to an Authorization header.
     */
    private suspend fun <T> execute(
        failureText: String,
        request: suspend (userId: String) -> HttpResponse,
        parse: suspend (HttpResponse) -> T,
    ): ChatResult<T> {
        val me = session.load()?.id ?: return ChatResult.Failure("You're not logged in")
        return try {
            val response = request(me)
            if (response.status.isSuccess()) {
                ChatResult.Success(parse(response))
            } else {
                ChatResult.Failure(response.apiErrorMessage() ?: "$failureText (${response.status.value})")
            }
        } catch (e: CancellationException) {
            throw e // never swallow coroutine cancellation
        } catch (e: Exception) {
            ChatResult.Failure("$failureText. Check your connection.")
        }
    }

    private suspend fun HttpResponse.apiErrorMessage(): String? =
        try {
            body<ApiError>().message
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    private companion object {
        /** Temporary identity header, matching the server's Identity.kt. */
        const val USER_ID_HEADER = "X-User-Id"
    }
}