package com.lagradost.cloudstream3.nostr.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class StreamStatus : Parcelable {
    PLANNED,
    LIVE,
    ENDED
}
