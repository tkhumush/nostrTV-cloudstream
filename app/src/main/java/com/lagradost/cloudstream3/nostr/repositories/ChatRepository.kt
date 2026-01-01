package com.lagradost.cloudstream3.nostr.repositories

import android.content.Context
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.nostr.NostrConstants
import com.lagradost.cloudstream3.nostr.crypto.EventSigner
import com.lagradost.cloudstream3.nostr.models.ChatMessage
import com.lagradost.cloudstream3.nostr.models.NostrFilter
import com.lagradost.cloudstream3.nostr.network.NostrEventParser.toChatMessage
import com.lagradost.cloudstream3.nostr.network.NostrMessage
import com.lagradost.cloudstream3.nostr.network.NostrRelayPool
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap

class ChatRepository(
    private val context: Context,
    private val relayPool: NostrRelayPool
) {

    private val json = jacksonObjectMapper()
    private val eventSigner = EventSigner(context)

    // Store messages per stream (streamId -> List<ChatMessage>)
    private val messagesCache = ConcurrentHashMap<String, MutableList<ChatMessage>>()

    private val activeSubscriptions = mutableSetOf<String>()

    fun getChatMessages(streamId: String, authorPubkey: String): Flow<List<ChatMessage>> {
        val subscriptionId = "chat_$streamId"

        // Subscribe if not already subscribed
        if (!activeSubscriptions.contains(subscriptionId)) {
            val filter = NostrFilter(
                kinds = listOf(NostrConstants.KIND_LIVE_CHAT),
                addressTags = listOf("${NostrConstants.KIND_LIVE_STREAM}:$authorPubkey:$streamId")
            )
            relayPool.subscribe(subscriptionId, listOf(filter))
            activeSubscriptions.add(subscriptionId)
        }

        return relayPool.events
            .mapNotNull { message ->
                if (message is NostrMessage.Event && message.event.kind == NostrConstants.KIND_LIVE_CHAT) {
                    message.event.toChatMessage()
                } else null
            }
            .filter { it.streamId == streamId }
            .onEach { chatMessage ->
                val messages = messagesCache.getOrPut(streamId) { mutableListOf() }

                // Add message if not duplicate
                if (messages.none { it.id == chatMessage.id }) {
                    messages.add(chatMessage)

                    // Sort by timestamp
                    messages.sortBy { it.createdAt }

                    // Limit to 500 messages per stream
                    if (messages.size > 500) {
                        messages.removeAt(0)
                    }
                }
            }
            .scan(emptyList<ChatMessage>()) { _, message ->
                messagesCache[streamId]?.toList() ?: emptyList()
            }
            .onStart {
                // Emit cached messages if available
                val cached = messagesCache[streamId]?.toList() ?: emptyList()
                if (cached.isNotEmpty()) emit(cached)
            }
    }

    suspend fun sendChatMessage(streamId: String, authorPubkey: String, content: String): Boolean {
        val event = eventSigner.signAndCreateEvent(
            kind = NostrConstants.KIND_LIVE_CHAT,
            content = content,
            tags = listOf(
                listOf("a", "${NostrConstants.KIND_LIVE_STREAM}:$authorPubkey:$streamId")
            )
        ) ?: return false

        val eventJson = json.writeValueAsString(event)
        relayPool.publishEvent(eventJson)
        return true
    }

    fun closeSubscription(streamId: String) {
        val subscriptionId = "chat_$streamId"
        relayPool.closeSubscription(subscriptionId)
        activeSubscriptions.remove(subscriptionId)
    }

    fun clearCache(streamId: String? = null) {
        if (streamId != null) {
            messagesCache.remove(streamId)
        } else {
            messagesCache.clear()
        }
    }
}
