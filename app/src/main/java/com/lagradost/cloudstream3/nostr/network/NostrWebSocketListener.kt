package com.lagradost.cloudstream3.nostr.network

import android.util.Log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.nostr.models.NostrEvent
import kotlinx.coroutines.channels.SendChannel
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class NostrWebSocketListener(
    private val url: String,
    private val messageChannel: SendChannel<NostrMessage>,
    private val stateChannel: SendChannel<ConnectionState>
) : WebSocketListener() {

    private val json = jacksonObjectMapper()

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "✅ Connected to relay: $url")
        stateChannel.trySend(ConnectionState.CONNECTED)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        try {
            val message = parseNostrMessage(text)
            when (message) {
                is NostrMessage.Event -> Log.d(TAG, "📨 EVENT from $url: kind=${message.event.kind}, id=${message.event.id.take(8)}")
                is NostrMessage.Eose -> Log.d(TAG, "🔚 EOSE from $url: ${message.subscriptionId}")
                is NostrMessage.Notice -> Log.i(TAG, "📢 NOTICE from $url: ${message.message}")
                is NostrMessage.Ok -> Log.d(TAG, "✔️ OK from $url: ${message.eventId.take(8)} accepted=${message.accepted}")
                is NostrMessage.Unknown -> Log.w(TAG, "❓ Unknown message from $url: ${text.take(100)}")
            }
            messageChannel.trySend(message)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing message from $url", e)
            messageChannel.trySend(NostrMessage.Unknown(text))
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e(TAG, "❌ Connection failed to $url: ${t.message}", t)
        stateChannel.trySend(ConnectionState.ERROR(t.message ?: "Unknown error"))
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "⏸️ Closing connection to $url: code=$code, reason=$reason")
        stateChannel.trySend(ConnectionState.DISCONNECTED)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "❌ Closed connection to $url: code=$code, reason=$reason")
        stateChannel.trySend(ConnectionState.DISCONNECTED)
    }

    companion object {
        private const val TAG = "NostrWebSocket"
    }

    private fun parseNostrMessage(text: String): NostrMessage {
        val arr: List<Any> = json.readValue(text)

        if (arr.isEmpty()) return NostrMessage.Unknown(text)

        val type = arr[0] as? String ?: return NostrMessage.Unknown(text)

        return when (type) {
            "EVENT" -> {
                val subscriptionId = arr.getOrNull(1) as? String ?: ""
                val eventMap = arr.getOrNull(2) ?: return NostrMessage.Unknown(text)
                val eventJson = json.writeValueAsString(eventMap)
                val event = json.readValue<NostrEvent>(eventJson)
                NostrMessage.Event(subscriptionId, event)
            }
            "EOSE" -> {
                val subscriptionId = arr.getOrNull(1) as? String ?: ""
                NostrMessage.Eose(subscriptionId)
            }
            "NOTICE" -> {
                val message = arr.getOrNull(1) as? String ?: ""
                NostrMessage.Notice(message)
            }
            "OK" -> {
                val eventId = arr.getOrNull(1) as? String ?: ""
                val accepted = arr.getOrNull(2) as? Boolean ?: false
                val message = arr.getOrNull(3) as? String ?: ""
                NostrMessage.Ok(eventId, accepted, message)
            }
            else -> NostrMessage.Unknown(text)
        }
    }
}
