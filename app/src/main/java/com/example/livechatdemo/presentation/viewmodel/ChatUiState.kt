package com.example.livechatdemo.presentation.viewmodel

import com.example.livechatdemo.domain.model.Chat
import com.example.livechatdemo.domain.model.Message

data class ChatUiState(
    val chats: List<Chat> = emptyList(),
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = false
)
