package com.example.livechatdemo.domain.usecase

import com.example.livechatdemo.domain.repository.ChatRepository
import com.example.livechatdemo.domain.model.Chat
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatsUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(): Flow<List<Chat>> = repository.getChats()
}