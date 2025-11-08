package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.Network
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.core.extension.decodeBase58

/**
 * Solana Token Program (SPL Token).
 */
object TokenProgram {
    const val PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"

    fun programId(): PublicKey {
        val alphabet = Network.SOLANA.alphabet()
            ?: throw IllegalStateException("Failed to get Solana alphabet")
        val publicKeyBytes = PROGRAM_ID.decodeBase58(alphabet)
            ?: throw IllegalStateException("Failed to decode Token Program ID")
        return PublicKey(publicKeyBytes, Network.SOLANA)
    }
}

/**
 * Solana Associated Token Account Program.
 *
 * The Associated Token Account Program is responsible for:
 * - Creating deterministic token accounts for wallet addresses
 * - Deriving associated token account addresses
 * - Managing the relationship between wallet and token accounts
 *
 * Program ID: ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL
 */
object AssociatedTokenProgram {

    /**
     * The Associated Token Account Program ID (Base58-encoded address).
     */
    const val PROGRAM_ID = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"

    /**
     * Returns the Associated Token Account Program's PublicKey.
     */
    fun programId(): PublicKey {
        val alphabet = Network.SOLANA.alphabet()
            ?: throw IllegalStateException("Failed to get Solana alphabet")
        val publicKeyBytes = PROGRAM_ID.decodeBase58(alphabet)
            ?: throw IllegalStateException("Failed to decode Associated Token Program ID")
        return PublicKey(publicKeyBytes, Network.SOLANA)
    }
}
