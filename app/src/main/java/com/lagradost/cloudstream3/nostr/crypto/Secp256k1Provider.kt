package com.lagradost.cloudstream3.nostr.crypto

import fr.acinq.secp256k1.Secp256k1
import java.security.SecureRandom

data class KeyPair(
    val privateKey: String,  // Hex-encoded
    val publicKey: String    // Hex-encoded (x-only pubkey)
)

class Secp256k1Provider {

    private val secp256k1 = Secp256k1.get()
    private val secureRandom = SecureRandom()

    fun generateKeyPair(): KeyPair {
        val privateKey = ByteArray(32)
        secureRandom.nextBytes(privateKey)
        val publicKey = secp256k1.pubKeyCompress(secp256k1.pubkeyCreate(privateKey)).drop(1).toByteArray()

        return KeyPair(
            privateKey = privateKey.toHex(),
            publicKey = publicKey.toHex()
        )
    }

    fun signSchnorr(eventJson: String, privateKey: String): String {
        val eventHash = sha256(eventJson)
        val privKeyBytes = privateKey.hexToByteArray()
        val signature = secp256k1.signSchnorr(eventHash, privKeyBytes, null)
        return signature.toHex()
    }

    fun verifySchnorr(eventJson: String, signature: String, publicKey: String): Boolean {
        return try {
            val eventHash = sha256(eventJson)
            val sigBytes = signature.hexToByteArray()
            val pubKeyBytes = ("02" + publicKey).hexToByteArray() // Add prefix

            secp256k1.verifySchnorr(sigBytes, eventHash, pubKeyBytes)
        } catch (e: Exception) {
            false
        }
    }

    fun getPublicKey(privateKey: String): String {
        val privKeyBytes = privateKey.hexToByteArray()
        val publicKey = secp256k1.pubKeyCompress(secp256k1.pubkeyCreate(privKeyBytes)).drop(1).toByteArray()
        return publicKey.toHex()
    }
}
