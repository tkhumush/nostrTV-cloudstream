package com.lagradost.cloudstream3.nostr.repositories

import android.util.Log
import com.lagradost.cloudstream3.nostr.NostrConstants
import com.lagradost.cloudstream3.nostr.models.NostrFilter
import com.lagradost.cloudstream3.nostr.models.Stream
import com.lagradost.cloudstream3.nostr.models.StreamStatus
import com.lagradost.cloudstream3.nostr.network.NostrEventParser.toStream
import com.lagradost.cloudstream3.nostr.network.NostrMessage
import com.lagradost.cloudstream3.nostr.network.NostrRelayPool
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap

class StreamRepository(private val relayPool: NostrRelayPool) {

    companion object {
        private const val TAG = "StreamRepository"
    }

    private val cache = ConcurrentHashMap<String, CachedStream>()

    data class CachedStream(val stream: Stream, val timestamp: Long)

    fun getLiveStreams(): Flow<List<Stream>> {
        val oneDayAgo = System.currentTimeMillis() / 1000 - (24 * 60 * 60)
        val filter = NostrFilter(
            kinds = listOf(NostrConstants.KIND_LIVE_STREAM),
            since = oneDayAgo,
            limit = 100
        )

        Log.d(TAG, "Setting up stream subscription with filter: kinds=[30311], since=$oneDayAgo, limit=100")

        return relayPool.events
            .onEach { message ->
                when (message) {
                    is NostrMessage.Event -> {
                        if (message.event.kind == NostrConstants.KIND_LIVE_STREAM) {
                            Log.d(TAG, "Received Kind 30311 event: ${message.event.id.take(8)}")
                        }
                    }
                    is NostrMessage.Eose -> Log.d(TAG, "Received EOSE for: ${message.subscriptionId}")
                    else -> {}
                }
            }
            .mapNotNull { message ->
                if (message is NostrMessage.Event && message.event.kind == NostrConstants.KIND_LIVE_STREAM) {
                    val stream = message.event.toStream()
                    if (stream != null) {
                        Log.d(TAG, "Parsed stream: ${stream.title} | status=${stream.status} | url=${stream.streamingUrl}")
                    } else {
                        Log.w(TAG, "Failed to parse Kind 30311 event to Stream: ${message.event.id.take(8)}")
                    }
                    stream
                } else null
            }
            .onEach { stream ->
                cache[stream.id] = CachedStream(stream, System.currentTimeMillis())
            }
            .scan(emptyList<Stream>()) { acc, stream ->
                (acc + stream).distinctBy { it.id }
            }
            .onStart {
                // Emit cached streams if fresh (< 30 seconds old)
                val now = System.currentTimeMillis()
                val fresh = cache.values
                    .filter { now - it.timestamp < NostrConstants.STREAM_STALE_THRESHOLD_MS }
                    .map { it.stream }
                if (fresh.isNotEmpty()) {
                    Log.d(TAG, "Emitting ${fresh.size} cached streams")
                    emit(fresh)
                } else {
                    Log.d(TAG, "No fresh cached streams found")
                }

                // Subscribe to new streams
                Log.d(TAG, "Subscribing to 'live_streams' on all connected relays")
                relayPool.subscribe("live_streams", listOf(filter))
            }
            .map { streams ->
                // DEBUG: Show ALL streams (not just LIVE) to see what we're receiving
                // TODO: Re-enable this filter once we confirm we're getting streams
                // streams.filter { it.status == StreamStatus.LIVE }
                Log.d(TAG, "Final stream list size: ${streams.size}")
                streams
            }
    }

    fun getStreamById(streamId: String): Stream? {
        return cache[streamId]?.stream
    }

    fun clearCache() {
        cache.clear()
    }

    fun closeSubscription() {
        relayPool.closeSubscription("live_streams")
    }
}
