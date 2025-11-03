package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey

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
data class Message(
    val header: MessageHeader,
    val accountKeys: List<PublicKey>,
    val recentBlockhash: String,
    val instructions: List<CompiledInstruction>
)
