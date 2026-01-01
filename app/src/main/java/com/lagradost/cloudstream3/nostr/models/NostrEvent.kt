package com.lagradost.cloudstream3.nostr.models

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.parcelize.Parcelize

@Parcelize
data class NostrEvent(
    val id: String,
    val pubkey: String,
    @JsonProperty("created_at")
    val createdAt: Long,
    val kind: Int,
    val tags: List<List<String>>,
    val content: String,
    val sig: String
) : Parcelable {
    companion object {
        const val KIND_METADATA = 0           // Profiles
        const val KIND_CONTACTS = 3           // Follows
        const val KIND_LIVE_CHAT = 1311       // Live chat messages
        const val KIND_ZAP = 9735             // Zaps (deferred)
        const val KIND_RELAY_LIST = 10002     // User relay list
        const val KIND_LIVE_EVENT = 30311     // Live streaming event
    }
}
