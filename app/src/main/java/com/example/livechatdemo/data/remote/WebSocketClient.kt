package com.example.livechatdemo.data.remote

import android.content.Context
import com.example.livechatdemo.domain.model.Message
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Singleton
class WebSocketClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) : WebSocketClient(URI("wss://s14465.blr1.piesocket.com/v3/1?api_key=JPQrNpeEViEtk9oHHd8CBaE8F4gAZSQenJIt7kcW&notify_self=false")) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _messages = MutableSharedFlow<Message>(replay = 0, extraBufferCapacity = 10)
    val messages: SharedFlow<Message> = _messages

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    // Track sent messages by content, chatId, and timestamp
    private val sentMessages = ConcurrentHashMap<String, Long>()

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
            scope.launch {
                try {
                    val json = JSONObject(it)
                    val content = json.optString("content", json.optString("message", it))
                    val chatId = json.optString("chatId", "supportBot")
                    val messageId = json.optString("id", "remote_${System.currentTimeMillis()}")
                    val timestamp = json.optLong("timestamp", System.currentTimeMillis())

                    // Check if this is a self-sent message
                    val key = "$chatId:$content:$timestamp"
                    if (sentMessages.containsKey(key)) {
                        println("Ignoring self-sent message: id=$messageId, chatId=$chatId, content=$content, timestamp=$timestamp")
                        sentMessages.remove(key)
                        return@launch
                    }

                    val msg = Message(
                        id = messageId,
                        chatId = chatId,
                        content = content,
                        isSent = false,
                        timestamp = timestamp
                    )
                    println("Emitting message: id=${msg.id}, chatId=${msg.chatId}, content=${msg.content}, timestamp=${msg.timestamp}")
                    _messages.emit(msg)
                    println("Message emitted successfully")
                } catch (e: Exception) {
                    println("Error parsing message: ${e.message}")
                    val msg = Message(
                        id = "remote_${System.currentTimeMillis()}",
                        chatId = "supportBot",
                        content = it,
                        isSent = false,
                        timestamp = System.currentTimeMillis()
                    )
                    println("Emitting fallback message: id=${msg.id}, chatId=${msg.chatId}, content=${msg.content}")
                    _messages.emit(msg)
                    println("Fallback message emitted successfully")
                }
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
                // Store message key
                val key = "${message.chatId}:${message.content}:${message.timestamp}"
                sentMessages[key] = System.currentTimeMillis()
                // Clean up old entries (older than 1 minute)
                sentMessages.entries.removeAll { System.currentTimeMillis() - it.value > 60_000 }

                val jsonMessage = JSONObject().apply {
                    put("type", "chat_message")
                    put("chatId", message.chatId)
                    put("content", message.content)
                    put("id", message.id)
                    put("timestamp", message.timestamp)
                }
                println("WebSocket sending: ${jsonMessage.toString()}")
                send(jsonMessage.toString())
            } catch (e: Exception) {
                println("Failed to send message: ${e.message}")
                sentMessages.remove("${message.chatId}:${message.content}:${message.timestamp}")
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