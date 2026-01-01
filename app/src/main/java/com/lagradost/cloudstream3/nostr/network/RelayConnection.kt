package com.lagradost.cloudstream3.nostr.network

import android.util.Log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.nostr.models.NostrFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket

class RelayConnection(
    private val url: String,
    private val client: OkHttpClient,
    private val scope: CoroutineScope
) {
    private var webSocket: WebSocket? = null
    private val messageChannel = Channel<NostrMessage>(Channel.BUFFERED)
    private val stateChannel = Channel<ConnectionState>(Channel.CONFLATED)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    val messages = messageChannel.receiveAsFlow()

    private val json = jacksonObjectMapper().apply {
        setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    }

    init {
        scope.launch {
            stateChannel.receiveAsFlow().collect { state ->
                _connectionState.value = state
            }
        }
    }

    fun connect() {
        if (_connectionState.value is ConnectionState.CONNECTED) {
            Log.d("RelayConnection", "Already connected to $url")
            return
        }

        Log.d("RelayConnection", "Connecting to relay: $url")
        _connectionState.value = ConnectionState.CONNECTING

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(
            request,
            NostrWebSocketListener(url, messageChannel, stateChannel)
        )
    }

    fun sendSubscription(subscriptionId: String, filters: List<NostrFilter>) {
        val filtersJson = filters.joinToString(",") { json.writeValueAsString(it) }
        val message = """["REQ","$subscriptionId",$filtersJson]"""
        Log.d("RelayConnection", "Sending subscription to $url: $subscriptionId")
        Log.d("RelayConnection", "Filter: $message")
        webSocket?.send(message)
    }

    fun closeSubscription(subscriptionId: String) {
        webSocket?.send("""["CLOSE","$subscriptionId"]""")
    }

    fun publishEvent(eventJson: String) {
        webSocket?.send("""["EVENT",$eventJson]""")
    }

    fun disconnect() {
        webSocket?.close(1000, "Client closing")
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}
