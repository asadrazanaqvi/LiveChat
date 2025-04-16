package com.example.livechatdemo.domain.usecase

import com.example.livechatdemo.domain.model.Message
import com.example.livechatdemo.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(chatId: String): Flow<List<Message>> = repository.getMessagesForChat(chatId)
}