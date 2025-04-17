package com.example.livechatdemo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livechatdemo.data.remote.WebSocketClient
import com.example.livechatdemo.domain.model.Chat
import com.example.livechatdemo.domain.model.Message
import com.example.livechatdemo.domain.usecase.GetChatsUseCase
import com.example.livechatdemo.domain.usecase.GetMessagesUseCase
import com.example.livechatdemo.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getChatsUseCase: GetChatsUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val webSocketClient: WebSocketClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _selectedChatId = MutableStateFlow("supportBot")
    val selectedChatId: StateFlow<String> = _selectedChatId.asStateFlow()

    init {
        loadChats()
        loadMessages(_selectedChatId.value)
        observeConnectionState()
        observeWebSocketMessages()
    }

    private fun observeWebSocketMessages() {
        viewModelScope.launch {
            webSocketClient.messages.collect { message ->
                println("ViewModel received message: chatId=${message.chatId}, content=${message.content}")
                if (message.chatId == _selectedChatId.value) {
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + message
                    )
                }
            }
        }}
    fun setSelectedChatId(chatId: String) {
        println("Selected chatId: $chatId")
        _selectedChatId.value = chatId
        loadMessages(chatId)
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            webSocketClient.connectionState.collect { isConnected ->
                println("Connection state changed: isConnected=$isConnected")
                _uiState.value = _uiState.value.copy(isConnected = isConnected)
                if (isConnected) {
                    loadMessages(_selectedChatId.value)
                }
            }
        }
    }

    fun loadChats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            getChatsUseCase()
                .catch { e ->
                    println("Error loading chats: ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
                .collect { chats ->
                    println("Loaded ${chats.size} chats")
                    _uiState.value = _uiState.value.copy(
                        chats = chats,
                        isLoading = false
                    )
                }
        }
    }

    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            getMessagesUseCase(chatId)
                .catch { e ->
                    println("Error loading messages for chatId=$chatId: ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
                .collect { messages ->
                    println("UI state updated with ${messages.size} messages for chatId=$chatId")
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        isLoading = false
                    )
                }
        }
    }

    fun sendMessage(content: String) {
        viewModelScope.launch {
            val message = Message(
                id = "msg_${System.currentTimeMillis()}",
                chatId = _selectedChatId.value,
                content = content,
                isSent = true,
                timestamp = System.currentTimeMillis()
            )
            println("Sending message: id=${message.id}, content=${message.content}")

            // Optimistic update
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + message.copy(isSent = false)
            )

            try {
                sendMessageUseCase(message)
                println("Message sent successfully: id=${message.id}")
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages.map {
                        if (it.id == message.id) it.copy(isSent = true) else it
                    }
                )
            } catch (e: Exception) {
                println("Error sending message: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = "Message queued - will send when online"
                )
            }
        }
    }
}