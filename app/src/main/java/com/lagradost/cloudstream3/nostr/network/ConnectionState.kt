package com.lagradost.cloudstream3.nostr.network

sealed class ConnectionState {
    object CONNECTING : ConnectionState()
    object CONNECTED : ConnectionState()
    object DISCONNECTED : ConnectionState()
    data class ERROR(val message: String) : ConnectionState()
}
