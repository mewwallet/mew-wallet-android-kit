package com.myetherwallet.mewwalletkit.solana.core

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.bip.bip44.Network

/**
 * Compatibility object for existing Solana code
 * Now delegates to the standard PublicKey class
 */
object SolanaPublicKey {
    /**
     * System Program public key (all zeros)
     */
    val SYSTEM_PROGRAM = PublicKey.fromSolanaBase58("11111111111111111111111111111111")

    /**
     * Create from Base58 string
     */
    @JvmStatic
    fun fromBase58(base58: String): PublicKey {
        return PublicKey.fromSolanaBase58(base58)
    }

    /**
     * Create from hex string
     */
    @JvmStatic
    fun fromHex(hex: String): PublicKey {
        val cleanHex = hex.removePrefix("0x")
        require(cleanHex.length == SolanaConstants.PUBLIC_KEY_LENGTH * 2) {
            "Hex string must be ${SolanaConstants.PUBLIC_KEY_LENGTH * 2} characters"
        }
        val bytes = cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return PublicKey(bytes, Network.SOLANA)
    }

    /**
     * Create from raw bytes
     */
    @JvmStatic
    fun fromBytes(bytes: ByteArray): PublicKey {
        return PublicKey(bytes, Network.SOLANA)
    }
}