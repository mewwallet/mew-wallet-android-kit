package com.myetherwallet.mewwalletkit.solana.program

import com.myetherwallet.mewwalletkit.solana.core.SolanaConstants
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.solana.encoding.BinaryWriter
import com.myetherwallet.mewwalletkit.solana.instruction.SolanaAccountMeta
import com.myetherwallet.mewwalletkit.solana.instruction.SolanaTransactionInstruction

/**
 * System Program instructions for Solana
 * Handles account creation, transfers, and other system operations
 */
object SystemProgram {

    /**
     * System Program ID
     */
    val PROGRAM_ID = PublicKey.fromSolanaBase58("11111111111111111111111111111111")

    /**
     * Create a transfer instruction
     * @param fromPubkey Source account public key
     * @param toPubkey Destination account public key
     * @param lamports Amount to transfer in lamports
     * @return Transfer instruction
     */
    fun transfer(
        fromPubkey: PublicKey,
        toPubkey: PublicKey,
        lamports: Long
    ): SolanaTransactionInstruction {
        require(lamports > 0) { "Transfer amount must be positive" }

        val data = BinaryWriter.write { writer ->
            writer.writeUInt32(SolanaConstants.SystemInstruction.TRANSFER.toLong())
            writer.writeUInt64(lamports)
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                SolanaAccountMeta.signer(fromPubkey, isWritable = true),
                SolanaAccountMeta.writable(toPubkey)
            ),
            data = data
        )
    }

    /**
     * Create an account creation instruction
     * @param fromPubkey Funding account public key
     * @param newAccountPubkey New account public key
     * @param lamports Amount to fund the new account
     * @param space Space to allocate for the new account
     * @param programId Program that will own the new account
     * @return Create account instruction
     */
    fun createAccount(
        fromPubkey: PublicKey,
        newAccountPubkey: PublicKey,
        lamports: Long,
        space: Long,
        programId: PublicKey
    ): SolanaTransactionInstruction {
        require(lamports >= 0) { "Lamports must be non-negative" }
        require(space >= 0) { "Space must be non-negative" }

        val data = BinaryWriter.write { writer ->
            writer.writeUInt32(SolanaConstants.SystemInstruction.CREATE_ACCOUNT.toLong())
            writer.writeUInt64(lamports)
            writer.writeUInt64(space)
            writer.writePublicKey(programId)
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                SolanaAccountMeta.signer(fromPubkey, isWritable = true),
                SolanaAccountMeta.signer(newAccountPubkey, isWritable = true)
            ),
            data = data
        )
    }

    /**
     * Create an assign instruction (change account owner)
     * @param accountPubkey Account to assign
     * @param programId New program owner
     * @return Assign instruction
     */
    fun assign(
        accountPubkey: PublicKey,
        programId: PublicKey
    ): SolanaTransactionInstruction {
        val data = BinaryWriter.write { writer ->
            writer.writeUInt32(SolanaConstants.SystemInstruction.ASSIGN.toLong())
            writer.writePublicKey(programId)
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                SolanaAccountMeta.signer(accountPubkey, isWritable = true)
            ),
            data = data
        )
    }

    /**
     * Create an allocate instruction (allocate space for account)
     * @param accountPubkey Account to allocate space for
     * @param space Amount of space to allocate
     * @return Allocate instruction
     */
    fun allocate(
        accountPubkey: PublicKey,
        space: Long
    ): SolanaTransactionInstruction {
        require(space >= 0) { "Space must be non-negative" }

        val data = BinaryWriter.write { writer ->
            writer.writeUInt32(SolanaConstants.SystemInstruction.ALLOCATE.toLong())
            writer.writeUInt64(space)
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                SolanaAccountMeta.signer(accountPubkey, isWritable = true)
            ),
            data = data
        )
    }

    /**
     * Create a transfer with seed instruction
     * @param fromPubkey Source account public key
     * @param fromBasePubkey Base public key for derived address
     * @param fromSeed Seed for derived address
     * @param fromOwner Owner of the derived address
     * @param toPubkey Destination account public key
     * @param lamports Amount to transfer
     * @return Transfer with seed instruction
     */
    fun transferWithSeed(
        fromPubkey: PublicKey,
        fromBasePubkey: PublicKey,
        fromSeed: String,
        fromOwner: PublicKey,
        toPubkey: PublicKey,
        lamports: Long
    ): SolanaTransactionInstruction {
        require(lamports > 0) { "Transfer amount must be positive" }

        val data = BinaryWriter.write { writer ->
            writer.writeUInt32(SolanaConstants.SystemInstruction.TRANSFER_WITH_SEED.toLong())
            writer.writeUInt64(lamports)
            writer.writeString(fromSeed)
            writer.writePublicKey(fromOwner)
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                SolanaAccountMeta.writable(fromPubkey),
                SolanaAccountMeta.readOnly(fromBasePubkey),
                SolanaAccountMeta.writable(toPubkey)
            ),
            data = data
        )
    }

    /**
     * Minimum balance for rent exemption calculation
     * This is a simplified version - in production, this should come from RPC
     */
    fun getMinimumBalanceForRentExemption(dataLength: Long): Long {
        // Simplified calculation: ~0.00204428 SOL per byte + base rent
        val LAMPORTS_PER_BYTE = 6960L
        val BASE_RENT = 890880L
        return BASE_RENT + (dataLength * LAMPORTS_PER_BYTE)
    }
}