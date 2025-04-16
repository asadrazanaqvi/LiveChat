package com.example.livechatdemo.data.remote

import com.example.livechatdemo.domain.model.Message
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI
import javax.inject.Inject

class WebSocketClient @Inject constructor() : WebSocketClient(
    URI("wss://s14465.blr1.piesocket.com/v3/1?api_key=JPQrNpeEViEtk9oHHd8CBaE8F4gAZSQenJIt7kcW&notify_self=true")
) {
    private val _messages = MutableSharedFlow<Message>()
    val messages: SharedFlow<Message> = _messages

    private val _connectionState = MutableSharedFlow<Boolean>()
    val connectionState: SharedFlow<Boolean> = _connectionState

    init {
        connect()
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        println("WebSocket connected")
        _connectionState.tryEmit(true)
        // Send initial message if needed
        send(JSONObject().apply {
            put("type", "connection_init")
        }.toString())
    }

    override fun onMessage(message: String?) {
        println("WebSocket received: $message")
        val msg = Message(
            id = "remote_${System.currentTimeMillis()}",
            chatId = "supportBot",
            content = message?:"",
            isSent = false,
            timestamp = System.currentTimeMillis()
        )
        message?.let {

            try {
                val json = JSONObject(it)
                when (json.getString("type")) {
                    "chat_message" -> handleChatMessage(json)
                    else -> handleUnknownMessage(it)
                }
            } catch (e: Exception) {
                handlePlainTextMessage(it)
            }
        }
    }

    private fun handleChatMessage(json: JSONObject) {
        val message = Message(
            id = json.optString("id", "remote_${System.currentTimeMillis()}"),
            chatId = json.optString("chatId", "supportBot"),
            content = json.getString("content"),
            isSent = false,
            timestamp = json.optLong("timestamp", System.currentTimeMillis())
        )
        _messages.tryEmit(message)
    }

    private fun handlePlainTextMessage(text: String) {
        val message = Message(
            id = "remote_${System.currentTimeMillis()}",
            chatId = "supportBot",
            content = text,
            isSent = false,
            timestamp = System.currentTimeMillis()
        )
        _messages.tryEmit(message)
    }

    private fun handleUnknownMessage(text: String) {
        println("Received unknown message format: $text")
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        println("WebSocket closed: $reason (code: $code)")
        _connectionState.tryEmit(false)
    }

    override fun onError(ex: Exception?) {
        println("WebSocket error: ${ex?.message}")
        _connectionState.tryEmit(false)
    }

    fun sendMessage(message: Message) {
        if (!isOpen) {
            println("WebSocket not connected, attempting to reconnect")
            reconnect()
        }

        if (isOpen) {
            try {
                val jsonMessage = JSONObject().apply {
                    put("type", "chat_message")
                    put("chatId", message.chatId)
                    put("content", message.content)
                    put("timestamp", message.timestamp)
                }
                send(jsonMessage.toString())
                println("Message sent successfully: ${message.content}")
            } catch (e: Exception) {
                println("Failed to send message: ${e.message}")
            }
        } else {
            println("WebSocket still not connected after reconnect attempt")
        }
    }

    override fun reconnect() {
        try {
            reconnectBlocking()
        } catch (e: Exception) {
            println("Reconnection failed: ${e.message}")
        }
    }
}