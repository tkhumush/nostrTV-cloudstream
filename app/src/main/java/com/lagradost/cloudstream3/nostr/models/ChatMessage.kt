package com.lagradost.cloudstream3.nostr.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChatMessage(
    val id: String,
    val streamId: String,
    val authorPubkey: String,
    val content: String,
    val createdAt: Long
) : Parcelable
