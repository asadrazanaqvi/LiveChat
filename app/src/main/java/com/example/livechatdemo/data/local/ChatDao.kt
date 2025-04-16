package com.example.livechatdemo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats")
    fun getChats(): Flow<List<ChatEntity>>

    @Insert
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE isSent = 0")
    suspend fun getUnsentMessages(): List<MessageEntity>

    @Query("UPDATE messages SET isSent = 1 WHERE id = :messageId")
    suspend fun markMessageAsSent(messageId: String)
}
