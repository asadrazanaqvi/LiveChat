package com.example.livechatdemo.data.remote

import com.example.livechatdemo.domain.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject

class WebSocketClient @Inject constructor(
    private val client: OkHttpClient
) {
    private var webSocket: WebSocket? = null
    private val _messages = MutableSharedFlow<Message>()
    val messages: Flow<Message> = _messages

    fun connect(url: String) {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                // Simulate parsing incoming message
                val message = Message(
                    id = "remote_${System.currentTimeMillis()}",
                    chatId = "supportBot",
                    content = text,
                    isSent = true,
                    timestamp = System.currentTimeMillis()
                )
                _messages.tryEmit(message)
            }
        })
    }

    fun sendMessage(message: Message) {
        webSocket?.send(message.content)
    }

    fun disconnect() {
        webSocket?.close(1000, "Disconnect")
    }
}