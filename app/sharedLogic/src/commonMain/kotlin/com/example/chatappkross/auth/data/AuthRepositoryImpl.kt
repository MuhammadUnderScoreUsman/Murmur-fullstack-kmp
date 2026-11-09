package com.example.chatappkross.auth.data

import com.example.chatappkross.auth.domain.AuthFailure
import com.example.chatappkross.auth.domain.AuthRepository
import com.example.chatappkross.auth.domain.AuthResult
import com.example.chatappkross.model.ApiError
import com.example.chatappkross.model.AuthResponse
import com.example.chatappkross.model.ErrorCode
import com.example.chatappkross.model.LoginRequest
import com.example.chatappkross.model.RegisterRequest
import com.example.chatappkross.model.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

class AuthRepositoryImpl(
    private val client: HttpClient,
    private val session: SessionStore,
) : AuthRepository {

    override suspend fun restoreSession(): UserDto? = session.load()

    override suspend fun register(username: String, password: String): AuthResult =
        send("auth/register", RegisterRequest(username.trim(), password))

    override suspend fun login(username: String, password: String): AuthResult =
        send("auth/login", LoginRequest(username.trim(), password))

    override suspend fun logout() = session.clear()

    private suspend inline fun <reified T : Any> send(path: String, body: T): AuthResult =
        try {
            val response = client.post(path) { setBody(body) }
            if (response.status.isSuccess()) {
                val user = response.body<AuthResponse>().user
                session.save(user)
                AuthResult.Success(user)
            } else {
                val error = runCatching { response.body<ApiError>() }.getOrNull()
                AuthResult.Failure(
                    AuthFailure.Server(
                        code = error?.code ?: ErrorCode.INTERNAL,
                        message = error?.message ?: "Request failed (${response.status.value})",
                    ),
                )
            }
        } catch (e: CancellationException) {
            throw e // never swallow coroutine cancellation
        } catch (e: Exception) {
            AuthResult.Failure(AuthFailure.Network())
        }
}
