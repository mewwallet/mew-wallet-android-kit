package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.Network
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.core.extension.sha256
import org.bouncycastle.math.ec.rfc8032.Ed25519

/**
 * Checks if this public key is on the Ed25519 curve.
 *
 * Used to distinguish regular Ed25519 keys (on-curve) from Solana Program Derived Addresses (off-curve).
 * Uses partial validation to check geometric curve membership without RFC 8032 security constraints.
 */
fun PublicKey.isOnCurve(): Boolean {
    return try {
        val keyBytes = this.data()
        if (keyBytes.size != Ed25519.PUBLIC_KEY_SIZE) return false
        Ed25519.validatePublicKeyPartial(keyBytes, 0)
    } catch (e: Exception) {
        false
    }
}

/**
 * Derives the Associated Token Account (ATA) address for this owner and mint.
 *
 * Seeds: owner + tokenProgramId + tokenMint → findProgramAddress(seeds, associatedTokenProgramId)
 *
 * @param allowOwnerOffCurve Allow off-curve owners (PDAs). Default requires on-curve keys.
 * @throws AssociatedTokenError.OwnerOffCurve if owner is off-curve and allowOwnerOffCurve is false
 */
fun PublicKey.associatedTokenAddress(
    tokenMint: PublicKey,
    allowOwnerOffCurve: Boolean = false,
    tokenProgramId: PublicKey = TokenProgram.programId(),
    associatedTokenProgramId: PublicKey = AssociatedTokenProgram.programId()
): PublicKey {
    if (!allowOwnerOffCurve && !this.isOnCurve()) {
        throw AssociatedTokenError.OwnerOffCurve()
    }

    val seeds = listOf(this.data(), tokenProgramId.data(), tokenMint.data())
    return findProgramAddress(seeds, associatedTokenProgramId).first
}

/**
 * Finds a valid PDA by searching bump seeds from 255 down to 0.
 *
 * @return Pair of (PDA address, bump seed)
 * @throws AssociatedTokenError.NoAddress if no valid off-curve PDA found
 */
fun findProgramAddress(seeds: List<ByteArray>, programId: PublicKey): Pair<PublicKey, UByte> {
    var nonce: UByte = 255u

    while (true) {
        try {
            val address = createProgramAddress(seeds + listOf(byteArrayOf(nonce.toByte())), programId)
            return Pair(address, nonce)
        } catch (e: AssociatedTokenError.InvalidSeed) {
            // Try next bump
        } catch (e: AssociatedTokenError) {
            throw e
        }

        if (nonce == 0u.toUByte()) break
        nonce = (nonce - 1u).toUByte()
    }

    throw AssociatedTokenError.NoAddress()
}

/**
 * Creates a PDA from seeds: sha256(seeds + programId + "ProgramDerivedAddress").
 *
 * @param seeds Each seed must be ≤ 32 bytes
 * @throws AssociatedTokenError.MaxSeedLengthExceeded if any seed > 32 bytes
 * @throws AssociatedTokenError.InvalidSeed if result is on-curve (PDAs must be off-curve)
 */
fun createProgramAddress(seeds: List<ByteArray>, programId: PublicKey): PublicKey {
    var data = ByteArray(0)
    seeds.forEach { seed ->
        if (seed.size > 32) throw AssociatedTokenError.MaxSeedLengthExceeded()
        data += seed
    }
    data += programId.data()
    data += "ProgramDerivedAddress".toByteArray(Charsets.UTF_8)

    val hash = data.sha256()
    if (hash.size != 32) throw AssociatedTokenError.InternalError("Hash is not 32 bytes")

    try {
        val key = PublicKey(hash, Network.SOLANA)
        if (key.isOnCurve()) throw AssociatedTokenError.InvalidSeed()
        return key
    } catch (e: AssociatedTokenError) {
        throw e
    } catch (e: Exception) {
        throw AssociatedTokenError.Underlying(e)
    }
}
