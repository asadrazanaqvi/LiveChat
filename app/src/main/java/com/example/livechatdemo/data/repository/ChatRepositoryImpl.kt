package com.example.livechatdemo.data.repository

import androidx.work.WorkManager
import com.example.livechatdemo.data.local.ChatDao
import com.example.livechatdemo.data.worker.MessageRetryWorker
import com.example.livechatdemo.domain.model.Chat
import com.example.livechatdemo.domain.model.Message
import com.example.livechatdemo.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import com.example.livechatdemo.data.local.ChatEntity
import com.example.livechatdemo.data.local.MessageEntity
import androidx.room.withTransaction
import com.example.livechatdemo.data.remote.WebSocketClient
import kotlinx.coroutines.flow.first

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val webSocketClient: WebSocketClient,
    private val workManager: WorkManager
) : ChatRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        initializeDefaultChats()
        setupWebSocketListener()
    }

    private fun initializeDefaultChats() {
        scope.launch {
            if (chatDao.getChatCount() == 0) {
                listOf(
                    //ChatEntity("supportBot", "Support Bot", "How can I help?", System.currentTimeMillis(), false),
                    //ChatEntity("salesBot", "Sales Bot", "Check our offers!", System.currentTimeMillis(), false),
                    ChatEntity("faqBot", "FAQ Bot", "Common questions", System.currentTimeMillis(), false)
                ).forEach {
                    println("Inserting default chat: ${it.botName}")
                    chatDao.insertChat(it)
                }
            }
        }
    }

    private fun setupWebSocketListener() {
        scope.launch {
            webSocketClient.messages.collect { message ->
                println("Repository received message: chatId=${message.chatId}, content=${message.content}, id=${message.id}")
                //chatDao.withTransaction {
                    chatDao.insertMessage(message.toEntity())
                    chatDao.updateChatLatestMessage(
                        message.chatId,
                        message.content,
                        message.timestamp
                    )
                //}
                println("Inserted message into Room: id=${message.id}, chatId=${message.chatId}")
                // Debug: Query Room to confirm insertion
                val messages = chatDao.getMessagesForChat(message.chatId).first()
                println("Room contains ${messages.size} messages for chatId=${message.chatId}")
            }
        }
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
            println("Room emitted ${entities.size} messages for chatId=$chatId")
            entities.map { it.toDomain() }
        }
    }

    override suspend fun sendMessage(message: Message) {
        val entity = message.toEntity().copy(isSent = false)
        println("Storing message: id=${entity.id}, content=${entity.content}, isSent=${entity.isSent}")
        //chatDao.withTransaction {
            chatDao.insertMessage(entity)
        //}
        try {
            webSocketClient.sendMessage(message)
            println("Message sent successfully, marking as sent: id=${message.id}")
            chatDao.markMessageAsSent(message.id)
        } catch (e: Exception) {
            println("Failed to send message: ${e.message}")
            scheduleRetry()
            throw e
        }
    }

    override suspend fun retryFailedMessages() {
        val unsent = chatDao.getUnsentMessages()
        println("Retrying ${unsent.size} unsent messages")
        unsent.forEach { message ->
            try {
                webSocketClient.sendMessage(message.toDomain())
                chatDao.markMessageAsSent(message.id)
                println("Retry succeeded for message: id=${message.id}")
            } catch (e: Exception) {
                println("Retry failed for message id=${message.id}: ${e.message}")
            }
        }
    }

    private fun scheduleRetry() {
        println("Scheduling retry with WorkManager")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<MessageRetryWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueue(workRequest)
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