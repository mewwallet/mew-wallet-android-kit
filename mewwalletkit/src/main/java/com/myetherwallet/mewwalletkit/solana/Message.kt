package com.myetherwallet.mewwalletkit.solana

import android.os.Parcelable
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import kotlinx.parcelize.Parcelize

/**
 * A compiled Solana transaction message (legacy format).
 *
 * This is the immutable, compiled representation of a transaction that can be signed and serialized.
 * All PublicKey references have been collected, deduplicated, sorted, and replaced with indices.
 *
 * Message wire format:
 * - MessageHeader (3 bytes)
 * - Compact array of account keys (PublicKey[])
 * - Recent blockhash (32 bytes)
 * - Compact array of compiled instructions (CompiledInstruction[])
 *
 * @property header Metadata about account distribution and signature requirements
 * @property accountKeys Ordered list of all accounts referenced by instructions (sorted by signer/writable flags)
 * @property recentBlockhash Base58-encoded recent blockhash for transaction expiry
 * @property instructions Compiled instructions with account indices instead of pubkeys
 */
@Parcelize
data class Message(
    val header: MessageHeader,
    val accountKeys: List<PublicKey>,
    val recentBlockhash: String,
    val instructions: List<CompiledInstruction>
): Parcelable {
    /**
     * Returns true if the account at the given index is writable in this message.
     *
     * Writability is derived from header counts assuming canonical ordering:
     * - Signers: first numRequiredSignatures, with the last
     *   numReadonlySignedAccounts being read-only.
     * - Non-signers: the remainder, with the last
     *   numReadonlyUnsignedAccounts being read-only.
     *
     * @param index The account index to check
     * @return true if the account is writable, false otherwise
     */
    fun isAccountWritable(index: Int): Boolean {
        val numSignedAccounts = header.numRequiredSignatures.toInt()
        return if (index >= numSignedAccounts) {
            // Non-signer account
            val unsignedAccountIndex = index - numSignedAccounts
            val numUnsignedAccounts = accountKeys.size - numSignedAccounts
            val numWritableUnsignedAccounts = numUnsignedAccounts - header.numReadonlyUnsignedAccounts.toInt()
            unsignedAccountIndex < numWritableUnsignedAccounts
        } else {
            // Signer account
            val numWritableSignedAccounts = numSignedAccounts - header.numReadonlySignedAccounts.toInt()
            index < numWritableSignedAccounts
        }
    }

    /**
     * Returns true if the account at the given index is a signer in this message.
     *
     * @param index The account index to check
     * @return true if the account is a signer, false otherwise
     */
    fun isAccountSigner(index: Int): Boolean {
        return index < header.numRequiredSignatures.toInt()
    }
}
