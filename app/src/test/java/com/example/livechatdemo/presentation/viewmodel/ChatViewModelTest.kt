package com.example.livechatdemo.presentation.viewmodel

import com.example.livechatdemo.data.remote.WebSocketClient
import com.example.livechatdemo.domain.model.Chat
import com.example.livechatdemo.domain.model.Message
import com.example.livechatdemo.domain.usecase.GetChatsUseCase
import com.example.livechatdemo.domain.usecase.GetMessagesUseCase
import com.example.livechatdemo.domain.usecase.SendMessageUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private lateinit var viewModel: ChatViewModel
    private lateinit var getChatsUseCase: GetChatsUseCase
    private lateinit var getMessagesUseCase: GetMessagesUseCase
    private lateinit var sendMessageUseCase: SendMessageUseCase
    private lateinit var webSocketClient: WebSocketClient
    private lateinit var messagesFlow: MutableSharedFlow<Message>
    private lateinit var connectionStateFlow: MutableStateFlow<Boolean>
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getChatsUseCase = mockk()
        getMessagesUseCase = mockk()
        sendMessageUseCase = mockk()
        webSocketClient = mockk()
        messagesFlow = MutableSharedFlow()
        connectionStateFlow = MutableStateFlow(false)
        every { webSocketClient.messages } returns messagesFlow
        every { webSocketClient.connectionState } returns connectionStateFlow
        coEvery { getChatsUseCase() } returns flowOf(
            listOf(
                Chat(
                    id = "supportBot",
                    botName = "Support Bot",
                    latestMessage = "How can I help?",
                    timestamp = 123L,
                    isUnread = false
                ),
                Chat(
                    id = "salesBot",
                    botName = "Sales Bot",
                    latestMessage = "Check our offers!",
                    timestamp = 124L,
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
                    timestamp = 123L
                )
            )
        )
        coEvery { getMessagesUseCase("salesBot") } returns flowOf(
            listOf(
                Message(
                    id = "msg2",
                    chatId = "salesBot",
                    content = "Offer!",
                    isSent = false,
                    timestamp = 124L
                )
            )
        )
        coEvery { sendMessageUseCase(any()) } returns Unit
        viewModel = ChatViewModel(getChatsUseCase, getMessagesUseCase, sendMessageUseCase, webSocketClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadChats should update uiState with chats`() = testScope.runTest {
        // Act
        viewModel.loadChats()

        // Assert
        val uiState = viewModel.uiState.value
        assertEquals(2, uiState.chats.size)
        assertEquals("Support Bot", uiState.chats[0].botName)
        assertEquals("Sales Bot", uiState.chats[1].botName)
        assertFalse(uiState.isLoading)
        assertEquals(null, uiState.error)
    }

    @Test
    fun `loadChats should set error on failure`() = testScope.runTest {
        // Arrange
        coEvery { getChatsUseCase() } returns flow { throw Exception("Network error") }

        // Act
        viewModel.loadChats()

        // Assert
        val uiState = viewModel.uiState.value
        assertEquals(0, uiState.chats.size)
        assertFalse(uiState.isLoading)
        assertEquals("Network error", uiState.error)
    }

    @Test
    fun `loadMessages should update uiState with messages`() = testScope.runTest {
        // Act
        viewModel.loadMessages("supportBot")

        // Assert
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.messages.size)
        assertEquals("Hi there!", uiState.messages[0].content)
        assertEquals("supportBot", uiState.messages[0].chatId)
        assertFalse(uiState.messages[0].isSent)
        assertFalse(uiState.isLoading)
        assertEquals(null, uiState.error)
    }

    @Test
    fun `loadMessages should set error on failure`() = testScope.runTest {
        // Arrange
        coEvery { getMessagesUseCase("supportBot") } returns flow { throw Exception("DB error") }

        // Act
        viewModel.loadMessages("supportBot")

        // Assert
        val uiState = viewModel.uiState.value
        assertEquals(0, uiState.messages.size)
        assertFalse(uiState.isLoading)
        assertEquals("DB error", uiState.error)
    }

    @Test
    fun `sendMessage should invoke sendMessageUseCase and update uiState`() = testScope.runTest {
        // Arrange
        val content = "Test message"
        val slot = slot<Message>()
        coEvery { sendMessageUseCase(capture(slot)) } returns Unit

        // Act
        viewModel.sendMessage(content)

        // Assert
        coVerify { sendMessageUseCase(any()) }
        assertEquals("supportBot", slot.captured.chatId)
        assertEquals(content, slot.captured.content)
        assertTrue(slot.captured.isSent)
        val uiState = viewModel.uiState.value
        assertEquals(2, uiState.messages.size) // Existing + new message
        assertEquals(content, uiState.messages.last().content)
        assertTrue(uiState.messages.last().isSent)
        assertEquals(null, uiState.error)
    }

    @Test
    fun `sendMessage should set error on failure`() = testScope.runTest {
        // Arrange
        val content = "Test message"
        coEvery { sendMessageUseCase(any()) } throws Exception("Send failed")

        // Act
        viewModel.sendMessage(content)

        // Assert
        val uiState = viewModel.uiState.value
        assertEquals(2, uiState.messages.size) // Existing + optimistic update
        assertEquals(content, uiState.messages.last().content)
        assertFalse(uiState.messages.last().isSent)
        assertEquals("Message queued - will send when online", uiState.error)
    }

    @Test
    fun `setSelectedChatId should update selectedChatId and load messages`() = testScope.runTest {
        // Act
        viewModel.setSelectedChatId("salesBot")

        // Assert
        assertEquals("salesBot", viewModel.selectedChatId.value)
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.messages.size)
        assertEquals("Offer!", uiState.messages[0].content)
        assertEquals("salesBot", uiState.messages[0].chatId)
        assertFalse(uiState.isLoading)
        assertEquals(null, uiState.error)
    }

    @Test
    fun `observeConnectionState should update isConnected and load messages`() = testScope.runTest {
        // Act
        connectionStateFlow.emit(true)

        // Assert
        val uiState = viewModel.uiState.value
        assertTrue(uiState.isConnected)
        assertEquals(1, uiState.messages.size)
        assertEquals("Hi there!", uiState.messages[0].content)
        assertFalse(uiState.isLoading)
        assertEquals(null, uiState.error)
    }

    @Test
    fun `observeWebSocketMessages should add message for selected chat`() = testScope.runTest {
        // Arrange
        val message = Message(
            id = "msg2",
            chatId = "supportBot",
            content = "New message",
            isSent = false,
            timestamp = 125L
        )

        // Act
        messagesFlow.emit(message)

        // Assert
        val uiState = viewModel.uiState.value
        assertEquals(2, uiState.messages.size) // Existing + new message
        assertEquals("New message", uiState.messages.last().content)
        assertEquals("supportBot", uiState.messages.last().chatId)
        assertFalse(uiState.messages.last().isSent)
    }

    @Test
    fun `observeWebSocketMessages should ignore message for unselected chat`() = testScope.runTest {
        // Arrange
        val message = Message(
            id = "msg2",
            chatId = "salesBot",
            content = "New message",
            isSent = false,
            timestamp = 125L
        )

        // Act
        messagesFlow.emit(message)

        // Assert
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.messages.size) // Only existing message
        assertEquals("Hi there!", uiState.messages[0].content)
    }
}