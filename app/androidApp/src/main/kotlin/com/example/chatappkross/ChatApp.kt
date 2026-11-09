package com.example.chatappkross

import android.app.Application
import com.example.chatappkross.presentation.app.AppContainer

class ChatApp : Application() {

    val container: AppContainer by lazy { AppContainer(BASE_URL) }

    companion object {
        const val BASE_URL = "see setup, your local ip goes here"
    }
}