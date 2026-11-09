package com.example.chatappkross.chat.data.reposiotry

import com.example.chatappkross.chat.data.fromDto
import com.example.chatappkross.chat.domain.util.ChatResult
import com.example.chatappkross.chat.domain.util.UserResult
import com.example.chatappkross.chat.domain.repository.UserRepository
import com.example.chatappkross.model.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

class UsersRepositoryImpl(private val client: HttpClient) : UserRepository {

    override suspend fun fetchUsers(): UserResult =
        try {
            val response = client.get("users")
            if (response.status.isSuccess()) {
                ChatResult.Success(response.body<List<UserDto>>().map { it.fromDto() })
            } else {
                ChatResult.Failure("Couldn't load users (${response.status.value})")
            }
        } catch (e: CancellationException) {
            throw e // never swallow coroutine cancellation
        } catch (e: Exception) {
            ChatResult.Failure("Couldn't load users. Check your connection.")
        }

}
