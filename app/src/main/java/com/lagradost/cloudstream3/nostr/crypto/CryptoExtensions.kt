package com.lagradost.cloudstream3.nostr.crypto

import java.security.MessageDigest

fun String.hexToByteArray(): ByteArray {
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

fun ByteArray.toHex(): String {
    return joinToString("") { "%02x".format(it) }
}

fun sha256(input: String): ByteArray {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(input.toByteArray())
}

fun sha256(input: ByteArray): ByteArray {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(input)
}
