package com.example.livechatdemo.data.repository

import com.example.livechatdemo.data.local.ChatDao
import com.example.livechatdemo.data.local.MessageEntity
import com.example.livechatdemo.data.remote.WebSocketClient
import com.example.livechatdemo.domain.model.Chat
import com.example.livechatdemo.domain.model.Message
import com.example.livechatdemo.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val webSocketClient: WebSocketClient
) : ChatRepository {
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        initializeChats()
        setupWebSocketListener()
    }

    private fun initializeChats() {
        scope.launch {
            if (chatDao.getChatCount() == 0) {
                val defaultChats = listOf(
                    createDefaultChat("supportBot", "Support Bot"),
                    createDefaultChat("salesBot", "Sales Bot"),
                    createDefaultChat("faqBot", "FAQ Bot")
                )
                defaultChats.forEach { chatDao.insertMessage(it) }
            }
        }
    }

    private fun createDefaultChat(chatId: String, botName: String) = MessageEntity(
        id = chatId,
        chatId = chatId,
        content = "Welcome to $botName!",
        isSent = true,
        timestamp = System.currentTimeMillis()
    )

    private fun setupWebSocketListener() {
        scope.launch {
            webSocketClient.messages.collect { message ->
                println("ChatRepositoryImpl ${message}")
                handleIncomingMessage(message)
            }
        }
    }

    private suspend fun handleIncomingMessage(message: Message) {
        chatDao.insertMessage(message.toEntity())
        chatDao.updateChatLatestMessage(
            chatId = message.chatId,
            message = message.content,
            timestamp = message.timestamp
        )
    }

    override fun getChats(): Flow<List<Chat>> {
        return chatDao.getChats().map { entities ->
            entities.map { entity ->
                Chat(
                    id = entity.id,
                    botName = entity.botName,
                    latestMessage = entity.latestMessage,
                    timestamp = entity.timestamp,
                    isUnread = entity.isUnread
                )
            }
        }
    }

    override fun getMessagesForChat(chatId: String): Flow<List<Message>> {
        return chatDao.getMessagesForChat(chatId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun sendMessage(message: Message) {
        // Save to local DB first
        chatDao.insertMessage(message.toEntity().copy(isSent = false))

        try {
            webSocketClient.sendMessage(message)
            chatDao.markMessageAsSent(message.id)
        } catch (e: Exception) {
            println("Failed to send message, will retry later: ${e.message}")
            // Message remains marked as unsent in DB
        }
    }

    private suspend fun retryUnsentMessages() {
        chatDao.getUnsentMessages().forEach { unsentMessage ->
            try {
                webSocketClient.sendMessage(unsentMessage.toDomain())
                chatDao.markMessageAsSent(unsentMessage.id)
            } catch (e: Exception) {
                println("Failed to retry message ${unsentMessage.id}: ${e.message}")
            }
        }
    }

    private fun MessageEntity.toDomain() = Message(
        id = id,
        chatId = chatId,
        content = content,
        isSent = isSent,
        timestamp = timestamp
    )

    private fun Message.toEntity() = MessageEntity(
        id = id,
        chatId = chatId,
        content = content,
        isSent = isSent,
        timestamp = timestamp
    )
}