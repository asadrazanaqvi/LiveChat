package com.example.livechatdemo.presentation.viewmodel

import com.example.livechatdemo.data.remote.WebSocketClient
import com.example.livechatdemo.domain.model.Chat
import com.example.livechatdemo.domain.model.Message
import com.example.livechatdemo.domain.usecase.GetChatsUseCase
import com.example.livechatdemo.domain.usecase.GetMessagesUseCase
import com.example.livechatdemo.domain.usecase.SendMessageUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    private val webSocketClient: WebSocketClient = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getChatsUseCase() } returns flowOf(
            listOf(
                Chat(
                    id = "supportBot",
                    botName = "Support Bot",
                    latestMessage = "How can I help?",
                    timestamp = System.currentTimeMillis(),
                    isUnread = false
                )
            )
        )
        coEvery { getMessagesUseCase("supportBot") } returns flowOf(
            listOf(
                Message(
                    id = "msg1",
                    chatId = "supportBot",
                    content = "Hi there!",
                    isSent = false,
                    timestamp = System.currentTimeMillis()
                )
            )
        )
        coEvery { sendMessageUseCase(any()) } just Runs
       // coEvery { webSocketClient.connectionState } returns flowOf(true)
       // coEvery { webSocketClient.messages } returns flowOf()
        viewModel = ChatViewModel(getChatsUseCase, getMessagesUseCase, sendMessageUseCase, webSocketClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadChats should update uiState with chats`() = testScope.runTest {
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.chats.size)
        assertEquals("Support Bot", uiState.chats[0].botName)
        assertEquals(false, uiState.isLoading)
    }

    @Test
    fun `loadMessages should update uiState with messages`() = testScope.runTest {
        viewModel.loadMessages("supportBot")
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.messages.size)
        assertEquals("Hi there!", uiState.messages[0].content)
        assertEquals(false, uiState.messages[0].isSent)
    }

    @Test
    fun `sendMessage should invoke sendMessageUseCase and update uiState`() = testScope.runTest {
        val content = "Test message"
        val slot = slot<Message>()
        coEvery { sendMessageUseCase(capture(slot)) } just Runs
        viewModel.sendMessage(content)
        coVerify { sendMessageUseCase(any()) }
        assertEquals("supportBot", slot.captured.chatId)
        assertEquals(content, slot.captured.content)
        assertEquals(true, slot.captured.isSent)
        val uiState = viewModel.uiState.value
        assertEquals(2, uiState.messages.size) // Optimistic update + existing message
        assertEquals(content, uiState.messages.last().content)
    }

    @Test
    fun `loadMessages with error should update uiState with error`() = testScope.runTest {
        coEvery { getMessagesUseCase("supportBot") } throws Exception("Network error")
        viewModel.loadMessages("supportBot")
        val uiState = viewModel.uiState.value
        assertEquals("Network error", uiState.error)
    }
}