package com.example.livechatdemo.domain.model

data class Chat(
    val id: String,
    val botName: String,
    val latestMessage: String,
    val timestamp: Long,
    val isUnread: Boolean
)

data class Message(
    val id: String,
    val chatId: String,
    val content: String,
    val isSent: Boolean,
    val timestamp: Long
)