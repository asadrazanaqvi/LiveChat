package com.example.livechatdemo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ChatEntity::class, MessageEntity::class], version = 1)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}