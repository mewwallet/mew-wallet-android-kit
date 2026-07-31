package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey

/**
 * Represents an account's metadata in a Solana transaction instruction.
 *
 * @property pubkey The public key of the account
 * @property isSigner Whether the account must sign the transaction
 * @property isWritable Whether the account's data will be modified
 */
data class AccountMeta(
    val pubkey: PublicKey,
    val isSigner: Boolean,
    val isWritable: Boolean
)
