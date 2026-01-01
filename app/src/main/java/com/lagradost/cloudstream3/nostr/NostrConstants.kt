package com.lagradost.cloudstream3.nostr

object NostrConstants {
    // Event Kinds
    const val KIND_METADATA = 0
    const val KIND_LIVE_CHAT = 1311
    const val KIND_LIVE_STREAM = 30311

    // Default Relays - Focused on live streaming
    val DEFAULT_RELAYS = listOf(
        "wss://relay.snort.social",
        "wss://relay.damus.io",
        "wss://nos.lol",
        "wss://relay.nostr.band",
        "wss://nostr.wine",
        "wss://relay.primal.net",
        "wss://purplepag.es"
    )

    // DataStore Keys
    const val NOSTR_RELAYS_KEY = "nostr_relays"
    const val NOSTR_ENABLED_KEY = "nostr_enabled"

    // Cache Settings
    const val STREAM_STALE_THRESHOLD_MS = 30_000L  // 30 seconds
    const val PROFILE_TTL_MS = 3_600_000L          // 1 hour
    const val PROFILE_MAX_COUNT = 1000
}
