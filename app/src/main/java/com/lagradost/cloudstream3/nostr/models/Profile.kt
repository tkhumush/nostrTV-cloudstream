package com.lagradost.cloudstream3.nostr.models

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.parcelize.Parcelize

@Parcelize
data class Profile(
    val pubkey: String,
    val name: String? = null,
    val displayName: String? = null,
    val picture: String? = null,
    val about: String? = null,
    val nip05: String? = null,
    val lud16: String? = null
) : Parcelable

data class ProfileMetadata(
    val name: String? = null,
    @JsonProperty("display_name")
    val displayName: String? = null,
    val picture: String? = null,
    val about: String? = null,
    val nip05: String? = null,
    val lud16: String? = null
)
