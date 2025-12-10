package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey

/**
 * Represents a signature slot in a Solana transaction.
 *
 * Each transaction maintains a list of signature pairs, one for each required signer.
 * A signature can be:
 * - null: Not yet signed (placeholder)
 * - 64 bytes: Ed25519 signature
 *
 * @property signature The Ed25519 signature (64 bytes) or null if unsigned
 * @property publicKey The public key that should sign or has signed
 */
data class SignaturePubkeyPair(
    var signature: ByteArray?,
    val publicKey: PublicKey
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignaturePubkeyPair

        if (signature != null) {
            if (other.signature == null) return false
            if (!signature.contentEquals(other.signature)) return false
        } else if (other.signature != null) {
            return false
        }
        if (publicKey != other.publicKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = signature?.contentHashCode() ?: 0
        result = 31 * result + publicKey.hashCode()
        return result
    }
}
