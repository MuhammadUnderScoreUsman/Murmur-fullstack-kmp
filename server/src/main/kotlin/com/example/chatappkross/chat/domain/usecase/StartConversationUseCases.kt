package com.example.chatappkross.chat.domain.usecase

class StartConversationUseCases(
    val conversationUseCase: ConversationUseCase,
    val historyUseCase: HistoryUseCase,
    val openChatUseCase: OpenChatUseCase,
)