package com.example.livechatdemo.domain.repository

import com.example.livechatdemo.domain.model.Chat
import com.example.livechatdemo.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChats(): Flow<List<Chat>>
    fun getMessagesForChat(chatId: String): Flow<List<Message>>
    suspend fun sendMessage(message: Message)
}