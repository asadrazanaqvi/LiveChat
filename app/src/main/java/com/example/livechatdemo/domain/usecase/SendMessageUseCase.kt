package com.example.livechatdemo.domain.usecase

import com.example.livechatdemo.domain.repository.ChatRepository
import com.example.livechatdemo.domain.model.Message
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(message: Message) {
        repository.sendMessage(message)
    }
}