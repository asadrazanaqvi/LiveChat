package com.example.livechatdemo.presentation.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livechatdemo.domain.model.Chat
import com.example.livechatdemo.domain.model.Message
import com.example.livechatdemo.domain.usecase.GetChatsUseCase
import com.example.livechatdemo.domain.usecase.GetMessagesUseCase
import com.example.livechatdemo.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val chats: List<Chat> = emptyList(),
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getChatsUseCase: GetChatsUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {
    private val _uiState = mutableStateOf(ChatUiState())
    val uiState: State<ChatUiState> = _uiState

    private var _selectedChatId = mutableStateOf("supportBot") // Default to support bot
    val selectedChatId: State<String> = _selectedChatId

    init {
        loadChats()
        loadMessages(_selectedChatId.value)
    }

    fun loadChats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            getChatsUseCase()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
                .collect { chats ->
                    _uiState.value = _uiState.value.copy(
                        chats = chats,
                        isLoading = false
                    )
                }
        }
    }

    fun loadMessages(chatId: String) {
        _selectedChatId.value = chatId
        viewModelScope.launch {
            getMessagesUseCase(chatId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
                .collect { messages ->
                    _uiState.value = _uiState.value.copy(messages = messages)
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
            sendMessageUseCase(message)
            // Optimistically update UI
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + message
            )
        }
    }
}