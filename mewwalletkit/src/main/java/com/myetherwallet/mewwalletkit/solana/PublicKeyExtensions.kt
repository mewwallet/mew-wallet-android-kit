package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import org.bouncycastle.math.ec.rfc8032.Ed25519

/**
 * Checks whether the public key corresponds to a valid point on the Edwards25519 elliptic curve.
 *
 * This performs a full RFC 8032 validation which checks:
 * 1. Point validity - Ensures the key represents a valid curve point
 * 2. Non-canonical format - Rejects keys >= P (the field prime)
 * 3. Small order check - Verifies the point isn't in an insecure subgroup
 * 4. Prime order verification - Confirms the point is in the prime order subgroup
 *
 * This is useful for distinguishing between regular Ed25519 public keys (on-curve)
 * and Solana Program Derived Addresses (PDAs), which are intentionally created off-curve
 * and cannot sign transactions.
 *
 * @return true if the public key is a valid point on the Ed25519 curve, false otherwise
 */
fun PublicKey.isOnCurve(): Boolean {
    return try {
        val keyBytes = this.data()

        // Validate that we have exactly 32 bytes
        if (keyBytes.size != Ed25519.PUBLIC_KEY_SIZE) {
            return false
        }

        // Use BouncyCastle's full RFC 8032 validation
        // Returns true if the point is valid, on-curve, and in the prime order subgroup
        Ed25519.validatePublicKeyFull(keyBytes, 0)
    } catch (e: Exception) {
        // Any error means the key is invalid
        false
    }
}
