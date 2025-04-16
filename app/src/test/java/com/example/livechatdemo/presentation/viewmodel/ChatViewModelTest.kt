package com.example.livechatdemo.presentation.viewmodel

import com.example.livechatdemo.domain.model.Chat
import com.example.livechatdemo.domain.usecase.GetChatsUseCase
import com.example.livechatdemo.domain.usecase.GetMessagesUseCase
import com.example.livechatdemo.domain.usecase.SendMessageUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private lateinit var viewModel: ChatViewModel
    private val getChatsUseCase: GetChatsUseCase = mockk()
    private val getMessagesUseCase: GetMessagesUseCase = mockk()
    private val sendMessageUseCase: SendMessageUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        coEvery { getChatsUseCase() } returns flowOf(
            listOf(
                Chat(
                    id = "1",
                    botName = "SupportBot",
                    latestMessage = "Hello!",
                    timestamp = System.currentTimeMillis(),
                    isUnread = true
                )
            )
        )
        viewModel = ChatViewModel(getChatsUseCase,getMessagesUseCase,sendMessageUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadChats should update uiState with chats`() = runTest {
        // Act: Wait for flow collection
        val uiState = viewModel.uiState.value

        // Assert
        assertEquals(1, uiState.chats.size)
        assertEquals("SupportBot", uiState.chats[0].botName)
    }
}