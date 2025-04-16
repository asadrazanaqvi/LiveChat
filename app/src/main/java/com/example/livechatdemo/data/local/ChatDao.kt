package com.example.livechatdemo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chats ORDER BY timestamp DESC")
    fun getChats(): Flow<List<ChatEntity>>

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getChatCount(): Int

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Insert
    suspend fun insertChat(chat: ChatEntity)

    @Insert
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE isSent = 0")
    suspend fun getUnsentMessages(): List<MessageEntity>

    @Query("UPDATE messages SET isSent = 1 WHERE id = :messageId")
    suspend fun markMessageAsSent(messageId: String)

    @Query("UPDATE chats SET latestMessage = :message, timestamp = :timestamp WHERE id = :chatId")
    suspend fun updateChatLatestMessage(chatId: String, message: String, timestamp: Long)
}