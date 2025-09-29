package com.myetherwallet.mewwalletkit.solana.crypto

import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import com.myetherwallet.mewwalletkit.core.extension.toHexString
import com.myetherwallet.mewwalletkit.solana.core.SolanaConstants
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.bip.bip44.Network
import java.security.SecureRandom
import java.util.Arrays

/**
 * Represents a Solana private key (32 bytes)
 * Used for Ed25519 signing operations
 */
class SolanaPrivateKey private constructor(
    private val seed: ByteArray
) {

    init {
        require(seed.size == SolanaConstants.PRIVATE_KEY_LENGTH) {
            "Private key seed must be ${SolanaConstants.PRIVATE_KEY_LENGTH} bytes"
        }
    }

    private val privateKeyParams: Ed25519PrivateKeyParameters by lazy {
        Ed25519PrivateKeyParameters(seed, 0)
    }

    private val publicKeyParams: Ed25519PublicKeyParameters by lazy {
        privateKeyParams.generatePublicKey()
    }

    /**
     * Get the corresponding public key
     */
    fun publicKey(): PublicKey {
        return PublicKey(publicKeyParams.encoded, Network.SOLANA)
    }

    /**
     * Sign a message with this private key
     * @param message The message to sign
     * @return The signature bytes (64 bytes)
     */
    fun sign(message: ByteArray): SolanaSignature {
        val signer = Ed25519Signer()
        signer.init(true, privateKeyParams)
        signer.update(message, 0, message.size)
        val signatureBytes = signer.generateSignature()
        return SolanaSignature(signatureBytes)
    }

    /**
     * Get the raw seed bytes (use with caution)
     */
    fun seed(): ByteArray = seed.copyOf()

    /**
     * Get hex representation of the seed
     */
    fun toHexString(): String = seed.toHexString()

    /**
     * Securely clear the private key data
     */
    fun clear() {
        Arrays.fill(seed, 0.toByte())
    }

    companion object {
        /**
         * Generate a new random private key
         */
        @JvmStatic
        fun generate(): SolanaPrivateKey {
            val keyPairGenerator = Ed25519KeyPairGenerator()
            keyPairGenerator.init(Ed25519KeyGenerationParameters(SecureRandom()))
            val keyPair = keyPairGenerator.generateKeyPair()
            val privateKey = keyPair.private as Ed25519PrivateKeyParameters
            return SolanaPrivateKey(privateKey.encoded)
        }

        /**
         * Create from seed bytes
         */
        @JvmStatic
        fun fromSeed(seed: ByteArray): SolanaPrivateKey {
            return SolanaPrivateKey(seed.copyOf())
        }

        /**
         * Create from hex string seed
         */
        @JvmStatic
        fun fromHex(hex: String): SolanaPrivateKey {
            val cleanHex = hex.removePrefix("0x")
            require(cleanHex.length == SolanaConstants.PRIVATE_KEY_LENGTH * 2) {
                "Hex string must be ${SolanaConstants.PRIVATE_KEY_LENGTH * 2} characters"
            }
            val seed = cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            return SolanaPrivateKey(seed)
        }

        /**
         * Derive from BIP44 path using a master seed
         * This is a simplified implementation - for production use,
         * should integrate with the existing BIP44 derivation system
         */
        @JvmStatic
        fun fromBip44Seed(masterSeed: ByteArray, accountIndex: Int = 0): SolanaPrivateKey {
            // This is a placeholder - should use proper BIP44 derivation
            // For now, we'll use a simple approach with the master seed
            val derivationPath = "m/44'/501'/$accountIndex'/0'"

            // Simple seed derivation (should be replaced with proper BIP44)
            val combinedSeed = (masterSeed + derivationPath.toByteArray()).copyOf(32)
            return SolanaPrivateKey(combinedSeed)
        }
    }
}