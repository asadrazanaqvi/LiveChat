package com.example.livechatdemo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val botName: String,
    val latestMessage: String,
    val timestamp: Long,
    val isUnread: Boolean
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val content: String,
    val isSent: Boolean,
    val timestamp: Long
)
