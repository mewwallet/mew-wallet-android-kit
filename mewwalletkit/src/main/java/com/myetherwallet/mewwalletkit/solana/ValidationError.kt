package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey

/**
 * Represents a signature validation error for a specific public key.
 *
 * Used to report issues with transaction signatures during serialization.
 */
sealed class ValidationError {
    /**
     * The public key associated with this error.
     */
    abstract val publicKey: PublicKey

    /**
     * A signature is missing (null), but requireAllSignatures is true.
     *
     * @property publicKey The public key that requires a signature
     */
    data class MissingSignature(override val publicKey: PublicKey) : ValidationError() {
        override fun toString(): String =
            "Missing signature for ${publicKey.address()?.address ?: "unknown"}"
    }

    /**
     * The provided signature bytes do not validate against the public key and signed message.
     *
     * @property publicKey The public key whose signature failed verification
     */
    data class InvalidSignature(override val publicKey: PublicKey) : ValidationError() {
        override fun toString(): String =
            "Invalid signature for ${publicKey.address()?.address ?: "unknown"}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ValidationError) return false
        if (this::class != other::class) return false
        return publicKey == other.publicKey
    }

    override fun hashCode(): Int {
        return publicKey.hashCode() * 31 + this::class.hashCode()
    }
}
