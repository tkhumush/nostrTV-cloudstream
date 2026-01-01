package com.lagradost.cloudstream3.nostr.network

import com.lagradost.cloudstream3.nostr.models.NostrEvent

sealed class NostrMessage {
    data class Event(val subscriptionId: String, val event: NostrEvent) : NostrMessage()
    data class Eose(val subscriptionId: String) : NostrMessage()
    data class Notice(val message: String) : NostrMessage()
    data class Ok(val eventId: String, val accepted: Boolean, val message: String) : NostrMessage()
    data class Unknown(val raw: String) : NostrMessage()
}
