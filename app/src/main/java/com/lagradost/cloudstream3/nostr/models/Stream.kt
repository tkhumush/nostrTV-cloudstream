package com.lagradost.cloudstream3.nostr.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Stream(
    val id: String,
    val authorPubkey: String,
    val title: String,
    val summary: String? = null,
    val image: String? = null,
    val streamingUrl: String? = null,
    val recordingUrl: String? = null,
    val status: StreamStatus = StreamStatus.PLANNED,
    val starts: Long? = null,
    val ends: Long? = null,
    val currentParticipants: Int? = null,
    val totalParticipants: Int? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long
) : Parcelable
