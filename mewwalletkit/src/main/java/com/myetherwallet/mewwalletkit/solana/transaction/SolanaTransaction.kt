package com.myetherwallet.mewwalletkit.solana.transaction

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.solana.crypto.SolanaPrivateKey
import com.myetherwallet.mewwalletkit.solana.crypto.SolanaSignature
import com.myetherwallet.mewwalletkit.solana.encoding.BinaryWriter
import com.myetherwallet.mewwalletkit.solana.instruction.SolanaAccountMeta
import com.myetherwallet.mewwalletkit.solana.instruction.SolanaTransactionInstruction

/**
 * Represents a Solana transaction
 */
class SolanaTransaction {

    /**
     * Transaction signatures
     */
    var signatures: MutableList<SolanaSignature> = mutableListOf()

    /**
     * The fee payer for this transaction
     */
    var feePayer: PublicKey? = null

    /**
     * Instructions to execute atomically
     */
    var instructions: MutableList<SolanaTransactionInstruction> = mutableListOf()

    /**
     * Recent blockhash (required for transaction validity)
     */
    var recentBlockhash: String? = null

    /**
     * Compiled message cache
     */
    private var compiledMessage: SolanaMessage? = null

    /**
     * Add an instruction to this transaction
     */
    fun addInstruction(instruction: SolanaTransactionInstruction) {
        instructions.add(instruction)
        invalidateMessage()
    }

    /**
     * Add multiple instructions to this transaction
     */
    fun addInstructions(vararg instructions: SolanaTransactionInstruction) {
        this.instructions.addAll(instructions)
        invalidateMessage()
    }

    /**
     * Sign this transaction with the provided private keys
     */
    fun sign(vararg signers: SolanaPrivateKey) {
        require(signers.isNotEmpty()) { "At least one signer is required" }

        // Set fee payer to first signer if not already set
        if (feePayer == null) {
            feePayer = signers[0].publicKey()
        }

        // Compile the message
        val message = compileMessage()

        // Clear existing signatures
        signatures.clear()

        // Create signatures for each signer
        val signerPublicKeys = signers.map { it.publicKey() }

        // Find each signer in the message's account keys and create signatures
        for (accountKey in message.accountKeys) {
            val signerIndex = signerPublicKeys.indexOfFirst { it == accountKey }
            if (signerIndex >= 0) {
                // This account key has a corresponding signer
                val signature = signers[signerIndex].sign(message.serialize())
                signatures.add(signature)
            } else {
                // No signer for this account key, add empty signature
                signatures.add(SolanaSignature.empty())
            }
        }
    }

    /**
     * Compile the transaction message
     */
    fun compileMessage(): SolanaMessage {
        if (compiledMessage == null) {
            require(feePayer != null) { "Fee payer is required" }
            require(recentBlockhash != null) { "Recent blockhash is required" }
            require(instructions.isNotEmpty()) { "At least one instruction is required" }

            // Collect all account keys
            val allAccountKeys = mutableSetOf<PublicKey>()

            // Add fee payer first
            allAccountKeys.add(feePayer!!)

            // Add all accounts from instructions
            for (instruction in instructions) {
                allAccountKeys.add(instruction.programId)
                for (account in instruction.accounts) {
                    allAccountKeys.add(account.publicKey)
                }
            }

            // Separate into signers and non-signers
            val signerKeys = mutableListOf<PublicKey>()
            val nonSignerKeys = mutableListOf<PublicKey>()

            for (key in allAccountKeys) {
                val isSigner = key == feePayer || instructions.any { instruction ->
                    instruction.accounts.any { it.publicKey == key && it.isSigner }
                }

                if (isSigner) {
                    signerKeys.add(key)
                } else {
                    nonSignerKeys.add(key)
                }
            }

            // Sort: fee payer first, then other signers, then non-signers
            val sortedAccountKeys = mutableListOf<PublicKey>()
            sortedAccountKeys.add(feePayer!!)
            sortedAccountKeys.addAll(signerKeys.filter { it != feePayer })
            sortedAccountKeys.addAll(nonSignerKeys)

            // Compile instructions with account indices
            val compiledInstructions = instructions.map { instruction ->
                SolanaCompiledInstruction(
                    programIdIndex = sortedAccountKeys.indexOf(instruction.programId).toByte(),
                    accountIndices = instruction.accounts.map {
                        sortedAccountKeys.indexOf(it.publicKey).toByte()
                    },
                    data = instruction.data
                )
            }

            // Count writable accounts
            val writableSignerCount = signerKeys.count { key ->
                key == feePayer || instructions.any { instruction ->
                    instruction.accounts.any { it.publicKey == key && it.isSigner && it.isWritable }
                }
            }

            val writableNonSignerCount = nonSignerKeys.count { key ->
                instructions.any { instruction ->
                    instruction.accounts.any { it.publicKey == key && !it.isSigner && it.isWritable }
                }
            }

            compiledMessage = SolanaMessage(
                accountKeys = sortedAccountKeys,
                recentBlockhash = recentBlockhash!!,
                instructions = compiledInstructions,
                numRequiredSignatures = signerKeys.size.toByte(),
                numReadonlySignedAccounts = (signerKeys.size - writableSignerCount).toByte(),
                numReadonlyUnsignedAccounts = (nonSignerKeys.size - writableNonSignerCount).toByte()
            )
        }

        return compiledMessage!!
    }

    /**
     * Serialize the entire transaction
     */
    fun serialize(): ByteArray {
        val message = compileMessage()

        return BinaryWriter.write { writer ->
            // Write signatures
            writer.writeArray(signatures) { signature, w ->
                w.writeBytes(signature.bytes())
            }

            // Write message
            writer.writeBytes(message.serialize())
        }
    }

    /**
     * Get the transaction size in bytes
     */
    fun getTransactionSize(): Int {
        return serialize().size
    }

    /**
     * Invalidate the cached compiled message
     */
    private fun invalidateMessage() {
        compiledMessage = null
    }

    companion object {
        /**
         * Create a simple transfer transaction
         */
        fun createTransfer(
            from: PublicKey,
            to: PublicKey,
            lamports: Long,
            recentBlockhash: String
        ): SolanaTransaction {
            val transaction = SolanaTransaction()
            transaction.feePayer = from
            transaction.recentBlockhash = recentBlockhash

            // Create transfer instruction (will be implemented in SystemProgram)
            val transferInstruction = SolanaTransactionInstruction(
                programId = PublicKey.fromSolanaBase58("11111111111111111111111111111111"),
                accounts = listOf(
                    SolanaAccountMeta.signer(from, true),
                    SolanaAccountMeta.writable(to)
                ),
                data = createTransferInstructionData(lamports)
            )

            transaction.addInstruction(transferInstruction)
            return transaction
        }

        /**
         * Create instruction data for a transfer (simplified)
         */
        private fun createTransferInstructionData(lamports: Long): ByteArray {
            return BinaryWriter.write { writer ->
                writer.writeUInt32(2) // Transfer instruction type
                writer.writeUInt64(lamports)
            }
        }
    }
}