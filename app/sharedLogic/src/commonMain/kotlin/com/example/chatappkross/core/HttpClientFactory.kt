package com.example.chatappkross.core

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createHttpClient(baseUrl: String, engine: HttpClientEngine? = null): HttpClient {
    val config: HttpClientConfig<*>.() -> Unit = {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 15_000
        }
        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
        }
    }
    return if (engine != null) HttpClient(engine, config) else HttpClient(config)
}



/**
 * A separate client for the WebSocket. It deliberately has no HttpTimeout: a request timeout
 * would cut a healthy socket after 15 seconds. The connect time limit lives in RealtimeClient.
 */
fun createRealtimeHttpClient(engine: HttpClientEngine? = null): HttpClient {
    val config: HttpClientConfig<*>.() -> Unit = {
        install(WebSockets)
    }
    return if (engine != null) HttpClient(engine, config) else HttpClient(config)
}

/** "http://host:8080/" -> "ws://host:8080/", "https://..." -> "wss://...". */
fun toWebSocketBaseUrl(httpBaseUrl: String): String {
    val withSlash = if (httpBaseUrl.endsWith("/")) httpBaseUrl else "$httpBaseUrl/"
    return when {
        withSlash.startsWith("https://") -> "wss://" + withSlash.removePrefix("https://")
        withSlash.startsWith("http://") -> "ws://" + withSlash.removePrefix("http://")
        else -> throw IllegalArgumentException("Base URL must start with http:// or https://: $httpBaseUrl")
    }
}
