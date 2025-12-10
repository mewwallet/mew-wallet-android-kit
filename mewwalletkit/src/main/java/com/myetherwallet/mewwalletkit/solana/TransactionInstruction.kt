package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey

/**
 * Represents a single instruction in a Solana transaction.
 *
 * Each instruction specifies:
 * - Which accounts it operates on (with their roles)
 * - Which program to invoke
 * - The instruction data to pass to the program
 *
 * @property keys The accounts required by this instruction with their access flags
 * @property programId The public key of the program to invoke
 * @property data The instruction-specific data to pass to the program
 */
data class TransactionInstruction(
    val keys: List<AccountMeta>,
    val programId: PublicKey,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TransactionInstruction

        if (keys != other.keys) return false
        if (programId != other.programId) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keys.hashCode()
        result = 31 * result + programId.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
