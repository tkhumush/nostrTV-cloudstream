package com.lagradost.cloudstream3.nostr.models

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true, value = ["stability", "dtags"])
data class NostrFilter(
    val ids: List<String>? = null,
    val authors: List<String>? = null,
    val kinds: List<Int>? = null,
    @JsonProperty("#e")
    val eventTags: List<String>? = null,
    @JsonProperty("#p")
    val pubkeyTags: List<String>? = null,
    @JsonProperty("#a")
    val addressTags: List<String>? = null,
    @JsonProperty("#d")
    val dTags: List<String>? = null,
    val since: Long? = null,
    val until: Long? = null,
    val limit: Int? = null
) : Parcelable
