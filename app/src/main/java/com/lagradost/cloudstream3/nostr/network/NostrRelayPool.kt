package com.lagradost.cloudstream3.nostr.network

import android.util.Log
import com.lagradost.cloudstream3.nostr.models.NostrFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient

class NostrRelayPool(
    private val okHttpClient: OkHttpClient,
    private val scope: CoroutineScope
) {
    private val connections = mutableMapOf<String, RelayConnection>()

    private val _events = MutableSharedFlow<NostrMessage>(
        replay = 0,
        extraBufferCapacity = 100
    )
    val events: SharedFlow<NostrMessage> = _events.asSharedFlow()

    suspend fun connectToRelays(relayUrls: List<String>) {
        relayUrls.forEach { url ->
            if (!connections.containsKey(url)) {
                val connection = RelayConnection(
                    url = url,
                    client = okHttpClient,
                    scope = scope
                )
                connections[url] = connection
                connection.connect()

                // Collect events from this relay
                scope.launch {
                    connection.messages.collect { message ->
                        _events.emit(message)
                    }
                }
            }
        }

        // Wait for at least one relay to connect (max 5 seconds)
        Log.d("NostrRelayPool", "Waiting for at least one relay to connect...")
        val connected = withTimeoutOrNull(5000) {
            while (true) {
                val anyConnected = connections.values.any {
                    it.connectionState.value is ConnectionState.CONNECTED
                }
                if (anyConnected) {
                    Log.d("NostrRelayPool", "At least one relay is now connected")
                    return@withTimeoutOrNull true
                }
                delay(100)
            }
            false
        }

        if (connected != true) {
            Log.w("NostrRelayPool", "Timeout waiting for relays to connect")
        }
    }

    fun subscribe(subscriptionId: String, filters: List<NostrFilter>) {
        val connectedRelays = connections.filter {
            it.value.connectionState.value is ConnectionState.CONNECTED
        }
        Log.d("NostrRelayPool", "Subscribing to ${connectedRelays.size}/${connections.size} connected relays")
        connectedRelays.values.forEach { relay ->
            relay.sendSubscription(subscriptionId, filters)
        }
    }

    fun closeSubscription(subscriptionId: String) {
        connections.values.forEach { relay ->
            relay.closeSubscription(subscriptionId)
        }
    }

    suspend fun publishEvent(eventJson: String) {
        connections.values.forEach { relay ->
            relay.publishEvent(eventJson)
        }
    }

    fun disconnectAll() {
        connections.values.forEach { it.disconnect() }
        connections.clear()
    }

    fun disconnectRelay(url: String) {
        connections[url]?.disconnect()
        connections.remove(url)
    }
}
