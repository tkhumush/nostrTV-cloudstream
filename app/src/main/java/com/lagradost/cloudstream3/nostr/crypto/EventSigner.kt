package com.lagradost.cloudstream3.nostr.crypto

import android.content.Context
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.nostr.models.NostrEvent

class EventSigner(private val context: Context) {

    private val secp256k1Provider = Secp256k1Provider()
    private val json = jacksonObjectMapper()

    fun signAndCreateEvent(
        kind: Int,
        content: String,
        tags: List<List<String>> = emptyList()
    ): NostrEvent? {
        val privateKey = NostrKeyManager.getPrivateKey(context) ?: return null
        val publicKey = NostrKeyManager.getPublicKey(context) ?: return null

        // Create unsigned event
        val createdAt = System.currentTimeMillis() / 1000
        val unsignedEvent = listOf(
            0,
            publicKey,
            createdAt,
            kind,
            tags,
            content
        )

        // Serialize for signing: [0, pubkey, created_at, kind, tags, content]
        val serialized = json.writeValueAsString(unsignedEvent)

        // Generate event ID (SHA-256 hash)
        val id = sha256(serialized).toHex()

        // Sign with Schnorr
        val signature = secp256k1Provider.signSchnorr(serialized, privateKey)

        return NostrEvent(
            id = id,
            pubkey = publicKey,
            createdAt = createdAt,
            kind = kind,
            tags = tags,
            content = content,
            sig = signature
        )
    }
}
