package com.example.livechatdemo.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.livechatdemo.domain.model.Chat
import com.example.livechatdemo.domain.model.Message
import com.example.livechatdemo.presentation.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedChatId by viewModel.selectedChatId.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(selectedChatId) {
        println("LaunchedEffect: Loading messages for chatId=$selectedChatId")
        viewModel.loadMessages(selectedChatId)
    }

    LaunchedEffect(uiState.messages) {
        if (uiState.messages.isNotEmpty()) {
            println("Scrolling to latest message, count=${uiState.messages.size}")
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            ChatTopAppBar(
                selectedChatId = selectedChatId,
                chats = uiState.chats,
                onChatSelected = { chatId ->
                    viewModel.setSelectedChatId(chatId)
                },
                connectionState = uiState.isConnected
            )
        },
        bottomBar = {
            MessageInput(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    }
                },
                isConnected = uiState.isConnected
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        if (!uiState.isConnected) {
                            Button(
                                onClick = {
                                    viewModel.loadMessages(selectedChatId)
                                },
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text("Retry Connection")
                            }
                        }
                    }
                }

                uiState.chats.isEmpty() -> {
                    Text(
                        text = "No chats available",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.messages.isEmpty() -> {
                    Text(
                        text = if (uiState.isConnected) "No messages yet" else "Connecting...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    MessageList(
                        messages = uiState.messages,
                        listState = listState
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopAppBar(
    selectedChatId: String,
    chats: List<Chat>,
    onChatSelected: (String) -> Unit,
    connectionState: Boolean
) {
    var showChatSelector by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chats.find { it.id == selectedChatId }?.botName ?: "Chat",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (connectionState) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = "Connection status",
                    tint = if (connectionState) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        actions = {
            IconButton(onClick = { showChatSelector = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Select chat")
            }
            DropdownMenu(
                expanded = showChatSelector,
                onDismissRequest = { showChatSelector = false }
            ) {
                chats.forEach { chat ->
                    DropdownMenuItem(
                        text = { Text(chat.botName) },
                        onClick = {
                            println("Chat selected: ${chat.id}")
                            onChatSelected(chat.id)
                            showChatSelector = false
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun MessageList(
    messages: List<Message>,
    listState: LazyListState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            MessageItem(message = message)
        }
    }
}

@Composable
fun MessageItem(message: Message) {
    val isUserMessage = message.isSent
    val bubbleColor = if (isUserMessage) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUserMessage) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isUserMessage) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isUserMessage) Alignment.End else Alignment.Start
        ) {
            Text(
                text = if (isUserMessage) "You" else "Bot",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = bubbleColor),
                shape = when {
                    isUserMessage -> RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp)
                    else -> RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)
                }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = message.content,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = message.timestamp.toTimeFormat(),
                        color = textColor.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

fun Long.toTimeFormat(): String {
    val date = Date(this)
    val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return format.format(date)
}

@Composable
fun MessageInput(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isConnected: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column {
            if (!isConnected) {
                Text(
                    text = "Offline - messages will be sent when connection is restored",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(4.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageText,
                    onValueChange = onMessageChange,
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface),
                    placeholder = { Text("Type a message") },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled = true // Always allow typing, queue if offline
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSendClick,
                    enabled = messageText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (messageText.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}