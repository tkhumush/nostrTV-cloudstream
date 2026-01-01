package com.lagradost.cloudstream3.nostr.repositories

import com.lagradost.cloudstream3.nostr.NostrConstants
import com.lagradost.cloudstream3.nostr.models.NostrFilter
import com.lagradost.cloudstream3.nostr.models.Profile
import com.lagradost.cloudstream3.nostr.network.NostrEventParser.toProfile
import com.lagradost.cloudstream3.nostr.network.NostrMessage
import com.lagradost.cloudstream3.nostr.network.NostrRelayPool
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap

class ProfileRepository(private val relayPool: NostrRelayPool) {

    private val cache = ConcurrentHashMap<String, CachedProfile>()

    data class CachedProfile(
        val profile: Profile,
        val timestamp: Long,
        val accessTime: Long
    )

    suspend fun getProfile(pubkey: String): Profile? {
        val now = System.currentTimeMillis()
        val ttlThreshold = now - NostrConstants.PROFILE_TTL_MS

        // 1. Check cache (TTL-based)
        val cached = cache[pubkey]
        if (cached != null && cached.timestamp > ttlThreshold) {
            // Update LRU access time
            cache[pubkey] = cached.copy(accessTime = now)
            return cached.profile
        }

        // 2. Fetch from relays (timeout 5 seconds)
        val profile = fetchProfileFromRelays(pubkey) ?: return null

        // 3. Cache with LRU eviction
        cache[pubkey] = CachedProfile(profile, now, now)

        // Evict oldest if over limit
        if (cache.size > NostrConstants.PROFILE_MAX_COUNT) {
            evictOldest(cache.size - NostrConstants.PROFILE_MAX_COUNT)
        }

        return profile
    }

    private suspend fun fetchProfileFromRelays(pubkey: String): Profile? {
        val filter = NostrFilter(
            kinds = listOf(NostrConstants.KIND_METADATA),
            authors = listOf(pubkey),
            limit = 1
        )

        val subscriptionId = "profile_$pubkey"
        relayPool.subscribe(subscriptionId, listOf(filter))

        return try {
            withTimeout(5000) {
                relayPool.events
                    .mapNotNull { message ->
                        if (message is NostrMessage.Event &&
                            message.event.kind == NostrConstants.KIND_METADATA &&
                            message.event.pubkey == pubkey
                        ) {
                            message.event.toProfile()
                        } else null
                    }
                    .first()
            }
        } catch (e: Exception) {
            null
        } finally {
            relayPool.closeSubscription(subscriptionId)
        }
    }

    private fun evictOldest(count: Int) {
        // Sort by access time and remove oldest
        val toRemove = cache.entries
            .sortedBy { it.value.accessTime }
            .take(count)
            .map { it.key }

        toRemove.forEach { cache.remove(it) }
    }

    fun clearCache() {
        cache.clear()
    }

    fun getCachedProfile(pubkey: String): Profile? {
        return cache[pubkey]?.profile
    }
}
