package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.Network
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.core.extension.decodeBase58

/**
 * Solana System Program.
 *
 * The System Program is responsible for:
 * - Creating new accounts
 * - Transferring lamports between accounts
 * - Assigning account ownership
 * - Allocating account data space
 *
 * Program ID: 11111111111111111111111111111111
 */
object SystemProgram {

    /**
     * The System Program ID (Base58-encoded address).
     */
    const val PROGRAM_ID = "11111111111111111111111111111111"

    /**
     * Returns the System Program's PublicKey.
     */
    fun programId(): PublicKey {
        val alphabet = Network.SOLANA.alphabet()
            ?: throw IllegalStateException("Failed to get Solana alphabet")
        val publicKeyBytes = PROGRAM_ID.decodeBase58(alphabet)
            ?: throw IllegalStateException("Failed to decode System Program ID")
        return PublicKey(publicKeyBytes, Network.SOLANA)
    }

    /**
     * Creates a Transfer instruction.
     *
     * Transfers lamports (Solana's native token) from one account to another.
     *
     * @param fromPubkey Source account (will be debited, must sign)
     * @param toPubkey Destination account (will be credited)
     * @param lamports Amount to transfer in lamports (1 SOL = 1,000,000,000 lamports)
     * @return TransactionInstruction for the transfer
     */
    fun transfer(fromPubkey: PublicKey, toPubkey: PublicKey, lamports: ULong): TransactionInstruction {
        return SystemInstruction.transfer(fromPubkey, toPubkey, lamports)
    }

    /**
     * Creates a CreateAccount instruction.
     *
     * Creates a new account at the specified address and allocates space.
     *
     * @param fromPubkey Account that will fund the new account (must sign)
     * @param newAccountPubkey Address of the new account (must sign)
     * @param lamports Amount to transfer to the new account for rent exemption
     * @param space Number of bytes to allocate for account data
     * @param owner Program that will own the new account
     * @return TransactionInstruction for creating the account
     */
    fun createAccount(
        fromPubkey: PublicKey,
        newAccountPubkey: PublicKey,
        lamports: ULong,
        space: ULong,
        owner: PublicKey
    ): TransactionInstruction {
        return SystemInstruction.createAccount(fromPubkey, newAccountPubkey, lamports, space, owner)
    }
}
