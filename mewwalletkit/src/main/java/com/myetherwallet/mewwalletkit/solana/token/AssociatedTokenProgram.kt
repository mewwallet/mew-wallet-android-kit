package com.myetherwallet.mewwalletkit.solana.token

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.solana.encoding.BinaryWriter
import com.myetherwallet.mewwalletkit.solana.instruction.SolanaAccountMeta
import com.myetherwallet.mewwalletkit.solana.instruction.SolanaTransactionInstruction
import com.myetherwallet.mewwalletkit.solana.core.ProgramDerivedAddress
import com.myetherwallet.mewwalletkit.solana.program.SystemProgram
import com.myetherwallet.mewwalletkit.solana.core.SolanaPublicKey

/**
 * Associated Token Program implementation
 * Handles Associated Token Account (ATA) creation and management
 */
object AssociatedTokenProgram {

    /**
     * Associated Token Program ID
     */
    val PROGRAM_ID = PublicKey.fromSolanaBase58("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL")

    /**
     * Create Associated Token Account instruction
     * @param payer Account that will pay for the creation
     * @param owner Owner of the new token account
     * @param mint Token mint for the account
     * @return Instruction to create Associated Token Account
     */
    fun createAssociatedTokenAccount(
        payer: PublicKey,
        owner: PublicKey,
        mint: PublicKey
    ): SolanaTransactionInstruction {
        val associatedTokenAddress = getAssociatedTokenAddress(owner, mint)

        // No instruction data needed for ATA creation
        val data = BinaryWriter.write { writer ->
            // Associated Token Program uses empty instruction data
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                // Payer - will pay for account creation
                SolanaAccountMeta.signer(payer, isWritable = true),
                // Associated token account - will be created
                SolanaAccountMeta.writable(associatedTokenAddress),
                // Owner - owner of the new token account
                SolanaAccountMeta.readOnly(owner),
                // Mint - token mint
                SolanaAccountMeta.readOnly(mint),
                // System program - for account creation
                SolanaAccountMeta.readOnly(SystemProgram.PROGRAM_ID),
                // Token program - for token account initialization
                SolanaAccountMeta.readOnly(TokenProgram.PROGRAM_ID),
                // Rent sysvar - for rent exemption check
                SolanaAccountMeta.readOnly(SolanaPublicKey.SYSTEM_PROGRAM)
            ),
            data = data
        )
    }

    /**
     * Get Associated Token Address for given owner and mint
     * This is a Program Derived Address (PDA)
     * @param owner Owner public key
     * @param mint Token mint public key
     * @return Associated Token Account address
     */
    fun getAssociatedTokenAddress(owner: PublicKey, mint: PublicKey): PublicKey {
        val seeds = listOf(
            owner.data(),
            TokenProgram.PROGRAM_ID.data(),
            mint.data()
        )

        val (pda, _) = ProgramDerivedAddress.findProgramAddress(
            seeds = seeds,
            programId = PROGRAM_ID
        )

        return pda
    }

    /**
     * Create instruction to create ATA if it doesn't exist
     * This is an idempotent operation - won't fail if ATA already exists
     * @param payer Account that will pay for creation
     * @param owner Owner of the token account
     * @param mint Token mint
     * @return Instruction that creates ATA only if needed
     */
    fun createAssociatedTokenAccountIdempotent(
        payer: PublicKey,
        owner: PublicKey,
        mint: PublicKey
    ): SolanaTransactionInstruction {
        val associatedTokenAddress = getAssociatedTokenAddress(owner, mint)

        val data = BinaryWriter.write { writer ->
            writer.writeByte(1) // CreateIdempotent instruction
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                SolanaAccountMeta.signer(payer, isWritable = true),
                SolanaAccountMeta.writable(associatedTokenAddress),
                SolanaAccountMeta.readOnly(owner),
                SolanaAccountMeta.readOnly(mint),
                SolanaAccountMeta.readOnly(SystemProgram.PROGRAM_ID),
                SolanaAccountMeta.readOnly(TokenProgram.PROGRAM_ID),
                SolanaAccountMeta.readOnly(SolanaPublicKey.SYSTEM_PROGRAM)
            ),
            data = data
        )
    }

    /**
     * Check if an address is a valid Associated Token Account address
     * @param address Address to check
     * @param owner Expected owner
     * @param mint Expected mint
     * @return true if address matches expected ATA derivation
     */
    fun isAssociatedTokenAddress(
        address: PublicKey,
        owner: PublicKey,
        mint: PublicKey
    ): Boolean {
        val expectedAddress = getAssociatedTokenAddress(owner, mint)
        return address == expectedAddress
    }

    /**
     * Get the owner and mint from an ATA address (if possible)
     * Note: This is computationally expensive as it requires trying different combinations
     * In practice, you should store or fetch this information from the blockchain
     * @param ataAddress The ATA address
     * @param possibleOwners List of possible owners to check
     * @param possibleMints List of possible mints to check
     * @return Pair of (owner, mint) if found, null otherwise
     */
    fun getATAInfo(
        ataAddress: PublicKey,
        possibleOwners: List<PublicKey>,
        possibleMints: List<PublicKey>
    ): Pair<PublicKey, PublicKey>? {
        for (owner in possibleOwners) {
            for (mint in possibleMints) {
                if (getAssociatedTokenAddress(owner, mint) == ataAddress) {
                    return Pair(owner, mint)
                }
            }
        }
        return null
    }

    /**
     * Create multiple ATA creation instructions efficiently
     * @param payer Account that will pay for creations
     * @param accounts List of (owner, mint) pairs
     * @return List of instructions to create all ATAs
     */
    fun createMultipleAssociatedTokenAccounts(
        payer: PublicKey,
        accounts: List<Pair<PublicKey, PublicKey>>
    ): List<SolanaTransactionInstruction> {
        return accounts.map { (owner, mint) ->
            createAssociatedTokenAccountIdempotent(payer, owner, mint)
        }
    }

    /**
     * Get multiple ATA addresses efficiently
     * @param accounts List of (owner, mint) pairs
     * @return List of corresponding ATA addresses
     */
    fun getMultipleAssociatedTokenAddresses(
        accounts: List<Pair<PublicKey, PublicKey>>
    ): List<PublicKey> {
        return accounts.map { (owner, mint) ->
            getAssociatedTokenAddress(owner, mint)
        }
    }

    /**
     * Create instruction for recovering SOL from closed ATA
     * When an ATA is closed, any remaining SOL goes to a specified destination
     * @param ataAddress The closed ATA address
     * @param destination Where to send the recovered SOL
     * @param owner Owner of the original ATA
     * @return Instruction to recover SOL
     */
    fun recoverNested(
        ataAddress: PublicKey,
        destination: PublicKey,
        owner: PublicKey
    ): SolanaTransactionInstruction {
        val data = BinaryWriter.write { writer ->
            writer.writeByte(2) // RecoverNested instruction
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                SolanaAccountMeta.writable(ataAddress),
                SolanaAccountMeta.writable(destination),
                SolanaAccountMeta.readOnly(owner),
                SolanaAccountMeta.readOnly(SystemProgram.PROGRAM_ID)
            ),
            data = data
        )
    }
}