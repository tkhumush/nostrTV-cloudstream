package com.lagradost.cloudstream3.nostr.crypto

import android.content.Context
import com.lagradost.cloudstream3.utils.DataStore.getKey
import com.lagradost.cloudstream3.utils.DataStore.setKey
import com.lagradost.cloudstream3.utils.DataStore.containsKey

object NostrKeyManager {
    private const val PRIVATE_KEY = "nostr_private_key"
    private const val PUBLIC_KEY = "nostr_public_key"

    fun hasKeypair(context: Context): Boolean {
        return context.containsKey(PRIVATE_KEY) && context.containsKey(PUBLIC_KEY)
    }

    fun generateAndStoreKeypair(context: Context) {
        val provider = Secp256k1Provider()
        val keyPair = provider.generateKeyPair()
        context.setKey(PRIVATE_KEY, keyPair.privateKey)
        context.setKey(PUBLIC_KEY, keyPair.publicKey)
    }

    fun importKeypair(context: Context, privateKey: String): Boolean {
        return try {
            val provider = Secp256k1Provider()
            val publicKey = provider.getPublicKey(privateKey)
            context.setKey(PRIVATE_KEY, privateKey)
            context.setKey(PUBLIC_KEY, publicKey)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getPrivateKey(context: Context): String? {
        return context.getKey(PRIVATE_KEY, null as String?)
    }

    fun getPublicKey(context: Context): String? {
        return context.getKey(PUBLIC_KEY, null as String?)
    }

    fun deleteKeypair(context: Context) {
        context.setKey(PRIVATE_KEY, null as String?)
        context.setKey(PUBLIC_KEY, null as String?)
    }
}
