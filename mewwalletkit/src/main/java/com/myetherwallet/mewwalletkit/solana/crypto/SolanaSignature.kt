package com.myetherwallet.mewwalletkit.solana.crypto

import android.os.Parcelable
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import com.myetherwallet.mewwalletkit.core.extension.toHexString
import com.myetherwallet.mewwalletkit.solana.core.SolanaConstants
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import io.github.novacrypto.base58.Base58
import kotlinx.parcelize.Parcelize
import java.util.Arrays

/**
 * Represents a Solana signature (64 bytes)
 * Ed25519 signature
 */
@Parcelize
data class SolanaSignature(
    private val bytes: ByteArray
) : Parcelable {

    init {
        require(bytes.size == SolanaConstants.SIGNATURE_LENGTH) {
            "Signature must be ${SolanaConstants.SIGNATURE_LENGTH} bytes"
        }
    }

    /**
     * Get the raw signature bytes
     */
    fun bytes(): ByteArray = bytes.copyOf()

    /**
     * Get Base58 encoded string representation
     */
    fun toBase58(): String = Base58.base58Encode(bytes)

    /**
     * Get hex string representation
     */
    fun toHexString(): String = bytes.toHexString()

    /**
     * Verify this signature against a message and public key
     * @param message The original message that was signed
     * @param publicKey The public key to verify against
     * @return true if the signature is valid
     */
    fun verify(message: ByteArray, publicKey: PublicKey): Boolean {
        return try {
            val publicKeyParams = Ed25519PublicKeyParameters(publicKey.data(), 0)
            val signer = Ed25519Signer()
            signer.init(false, publicKeyParams)
            signer.update(message, 0, message.size)
            signer.verifySignature(bytes)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if this is a valid signature (not null/empty)
     */
    fun isValid(): Boolean {
        return !bytes.all { it == 0.toByte() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SolanaSignature
        return Arrays.equals(bytes, other.bytes)
    }

    override fun hashCode(): Int = Arrays.hashCode(bytes)

    override fun toString(): String = toBase58()

    companion object {
        /**
         * Create from Base58 string
         */
        @JvmStatic
        fun fromBase58(base58: String): SolanaSignature {
            return SolanaSignature(Base58.base58Decode(base58))
        }

        /**
         * Create from hex string
         */
        @JvmStatic
        fun fromHex(hex: String): SolanaSignature {
            val cleanHex = hex.removePrefix("0x")
            require(cleanHex.length == SolanaConstants.SIGNATURE_LENGTH * 2) {
                "Hex string must be ${SolanaConstants.SIGNATURE_LENGTH * 2} characters"
            }
            val bytes = cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            return SolanaSignature(bytes)
        }

        /**
         * Create from raw bytes
         */
        @JvmStatic
        fun fromBytes(bytes: ByteArray): SolanaSignature {
            return SolanaSignature(bytes)
        }

        /**
         * Create an empty/null signature
         */
        @JvmStatic
        fun empty(): SolanaSignature {
            return SolanaSignature(ByteArray(SolanaConstants.SIGNATURE_LENGTH))
        }
    }
}