package com.example.livechatdemo.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.example.livechatdemo.data.local.ChatDao
import com.example.livechatdemo.data.local.ChatDatabase
import com.example.livechatdemo.data.remote.WebSocketClient
import com.example.livechatdemo.data.repository.ChatRepositoryImpl
import com.example.livechatdemo.domain.repository.ChatRepository
import com.example.livechatdemo.domain.usecase.GetChatsUseCase
import com.example.livechatdemo.domain.usecase.GetMessagesUseCase
import com.example.livechatdemo.domain.usecase.SendMessageUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideChatDatabase(@ApplicationContext context: Context): ChatDatabase {
        return Room.databaseBuilder(
            context,
            ChatDatabase::class.java,
            "chat_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideChatDao(database: ChatDatabase): ChatDao = database.chatDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideWebSocketClient(
        @ApplicationContext context: Context,
        workManager: WorkManager
    ): WebSocketClient {
        return WebSocketClient(context, workManager)
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        chatDao: ChatDao,
        webSocketClient: WebSocketClient,
        workManager: WorkManager
    ): ChatRepository {
        return ChatRepositoryImpl(chatDao, webSocketClient, workManager)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideGetChatsUseCase(repository: ChatRepository): GetChatsUseCase {
        return GetChatsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetMessagesUseCase(repository: ChatRepository): GetMessagesUseCase {
        return GetMessagesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSendMessageUseCase(repository: ChatRepository): SendMessageUseCase {
        return SendMessageUseCase(repository)
    }
}