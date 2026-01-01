package com.lagradost.cloudstream3.nostr.network

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.nostr.models.*

object NostrEventParser {

    private val json = jacksonObjectMapper()

    fun NostrEvent.toProfile(): Profile? {
        if (kind != NostrEvent.KIND_METADATA) return null

        return try {
            val metadata = json.readValue<ProfileMetadata>(content)
            Profile(
                pubkey = pubkey,
                name = metadata.name,
                displayName = metadata.displayName,
                picture = metadata.picture,
                about = metadata.about,
                nip05 = metadata.nip05,
                lud16 = metadata.lud16
            )
        } catch (e: Exception) {
            null
        }
    }

    fun NostrEvent.toStream(): Stream? {
        if (kind != NostrEvent.KIND_LIVE_EVENT) return null

        val dTag = tags.find { it.firstOrNull() == "d" }?.getOrNull(1) ?: return null
        val title = tags.find { it.firstOrNull() == "title" }?.getOrNull(1) ?: "Untitled"
        val summary = tags.find { it.firstOrNull() == "summary" }?.getOrNull(1)
        val image = tags.find { it.firstOrNull() == "image" }?.getOrNull(1)
        val streamingUrl = tags.find { it.firstOrNull() == "streaming" }?.getOrNull(1)
        val recordingUrl = tags.find { it.firstOrNull() == "recording" }?.getOrNull(1)
        val statusStr = tags.find { it.firstOrNull() == "status" }?.getOrNull(1)
        val status = when (statusStr) {
            "live" -> StreamStatus.LIVE
            "ended" -> StreamStatus.ENDED
            else -> StreamStatus.PLANNED
        }
        val starts = tags.find { it.firstOrNull() == "starts" }?.getOrNull(1)?.toLongOrNull()
        val ends = tags.find { it.firstOrNull() == "ends" }?.getOrNull(1)?.toLongOrNull()
        val currentParticipants = tags.find { it.firstOrNull() == "current_participants" }
            ?.getOrNull(1)?.toIntOrNull()
        val totalParticipants = tags.find { it.firstOrNull() == "total_participants" }
            ?.getOrNull(1)?.toIntOrNull()

        val hashtags = tags.filter { it.firstOrNull() == "t" }.mapNotNull { it.getOrNull(1) }

        return Stream(
            id = dTag,
            authorPubkey = pubkey,
            title = title,
            summary = summary,
            image = image,
            streamingUrl = streamingUrl,
            recordingUrl = recordingUrl,
            status = status,
            starts = starts,
            ends = ends,
            currentParticipants = currentParticipants,
            totalParticipants = totalParticipants,
            tags = hashtags,
            createdAt = createdAt
        )
    }

    fun NostrEvent.toChatMessage(): ChatMessage? {
        if (kind != NostrEvent.KIND_LIVE_CHAT) return null

        val aTag = tags.find { it.firstOrNull() == "a" }?.getOrNull(1) ?: return null
        val streamId = aTag.split(":").getOrNull(2) ?: return null

        return ChatMessage(
            id = id,
            streamId = streamId,
            authorPubkey = pubkey,
            content = content,
            createdAt = createdAt
        )
    }
}
