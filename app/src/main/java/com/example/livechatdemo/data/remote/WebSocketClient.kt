package com.example.livechatdemo.data.remote

import android.content.Context
import com.example.livechatdemo.domain.model.Message
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.livechatdemo.data.worker.MessageRetryWorker
import kotlinx.coroutines.flow.last
import java.util.concurrent.TimeUnit

@Singleton
class WebSocketClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) : WebSocketClient(URI("wss://s14465.blr1.piesocket.com/v3/1?api_key=JPQrNpeEViEtk9oHHd8CBaE8F4gAZSQenJIt7kcW&notify_self=true")) {

    private val _messages = MutableSharedFlow<Message>(replay = 0)
    val messages: SharedFlow<Message> = _messages

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    init {
        connect()
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        println("WebSocket opened: ${handshakedata?.httpStatusMessage}")
        _connectionState.value = true
    }

    override fun onMessage(message: String?) {
        println("WebSocket received raw: $message")
        message?.let {
            try {
                val json = JSONObject(it)
                val content = json.optString("content", json.optString("message", it)) // Fallback to "message" or raw string
                val chatId = json.optString("chatId", "supportBot") // Default to supportBot
                val msg = Message(
                    id = json.optString("id", "remote_${System.currentTimeMillis()}"),
                    chatId = chatId,
                    content = content,
                    isSent = false,
                    timestamp = json.optLong("timestamp", System.currentTimeMillis())
                )
                println("Parsed message: id=${msg.id}, chatId=${msg.chatId}, content=${msg.content}")
                _messages.tryEmit(msg)
            } catch (e: Exception) {
                println("Error parsing message: ${e.message}")
                val msg = Message(
                    id = "remote_${System.currentTimeMillis()}",
                    chatId = "supportBot",
                    content = message,
                    isSent = false,
                    timestamp = System.currentTimeMillis()
                )
                _messages.tryEmit(msg)
                println("Parsed messages data ${_messages}")
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        println("WebSocket closed: code=$code, reason=$reason, remote=$remote")
        _connectionState.value = false
        scheduleRetry()
    }

    override fun onError(ex: Exception?) {
        println("WebSocket error: ${ex?.message}")
        _connectionState.value = false
        scheduleRetry()
    }

    fun sendMessage(message: Message) {
        if (isOpen) {
            try {
                val jsonMessage = JSONObject().apply {
                    put("type", "chat_message")
                    put("chatId", message.chatId)
                    put("content", message.content)
                    put("timestamp", message.timestamp)
                }
                println("WebSocket sending: ${jsonMessage.toString()}")
                send(jsonMessage.toString())
            } catch (e: Exception) {
                println("Failed to send message: ${e.message}")
                scheduleRetry()
                throw e
            }
        } else {
            println("WebSocket not connected, queuing message: ${message.content}")
            scheduleRetry()
            throw IllegalStateException("WebSocket not connected")
        }
    }

    private fun scheduleRetry() {
        println("Scheduling retry with WorkManager")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<MessageRetryWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueue(workRequest)
    }
}