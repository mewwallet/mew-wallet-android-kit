package com.myetherwallet.mewwalletkit.solana.transaction

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.solana.crypto.SolanaPrivateKey
import com.myetherwallet.mewwalletkit.solana.crypto.SolanaSignature
import com.myetherwallet.mewwalletkit.solana.instruction.SolanaTransactionInstruction
import com.myetherwallet.mewwalletkit.solana.encoding.BinaryWriter

/**
 * Versioned transaction implementation for Solana
 * Supports both legacy and v0 transaction formats
 */
class VersionedTransaction private constructor(
    val version: TransactionVersion,
    val payer: PublicKey,
    val recentBlockhash: String,
    val instructions: List<SolanaTransactionInstruction>
) {
    private val signatures = mutableListOf<SolanaSignature>()

    enum class TransactionVersion(val value: Byte) {
        LEGACY(0xFF.toByte()),
        V0(0x00.toByte())
    }

    /**
     * Sign the transaction with the given private key
     */
    fun sign(privateKey: SolanaPrivateKey) {
        require(privateKey.publicKey() == payer) { "Private key does not match payer" }

        val message = serializeMessage()
        val signature = privateKey.sign(message)

        // Replace existing signature from this key or add new one
        signatures.clear()
        signatures.add(signature)
    }

    /**
     * Serialize the transaction for transmission
     */
    fun serialize(): ByteArray {
        return BinaryWriter.write { writer ->
            // Write version if V0
            if (version == TransactionVersion.V0) {
                writer.writeByte(version.value)
            }

            // Write signatures
            writer.writeShortVec(signatures.map { it.bytes() })

            // Write message
            writer.writeBytes(serializeMessage())
        }
    }

    /**
     * Serialize the transaction message (for signing)
     */
    private fun serializeMessage(): ByteArray {
        return BinaryWriter.write { writer ->
            // Message header
            writer.writeByte(1) // numRequiredSignatures (payer only for now)
            writer.writeByte(0) // numReadonlySignedAccounts
            writer.writeByte(0) // numReadonlyUnsignedAccounts

            // Account keys (payer first, then unique accounts from instructions)
            val allAccounts = mutableSetOf<PublicKey>()
            allAccounts.add(payer)

            instructions.forEach { instruction ->
                allAccounts.add(instruction.programId)
                instruction.accounts.forEach { accountMeta ->
                    allAccounts.add(accountMeta.publicKey)
                }
            }

            val accountList = allAccounts.toList()
            writer.writeShortVec(accountList.map { it.data() })

            // Recent blockhash
            writer.writeBytes(recentBlockhash.toByteArray())

            // Instructions
            writer.writeShortVec(instructions.map { instruction ->
                BinaryWriter.write { instrWriter ->
                    // Program ID index
                    val programIndex = accountList.indexOf(instruction.programId)
                    instrWriter.writeByte(programIndex.toByte())

                    // Account indices
                    val accountIndices = instruction.accounts.map { accountMeta ->
                        accountList.indexOf(accountMeta.publicKey).toByte()
                    }
                    instrWriter.writeShortVec(accountIndices.map { byteArrayOf(it) })

                    // Instruction data
                    instrWriter.writeShortVec(listOf(instruction.data))
                }
            })
        }
    }

    companion object {
        /**
         * Builder for creating versioned transactions
         */
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var version: TransactionVersion = TransactionVersion.LEGACY
        private var payer: PublicKey? = null
        private var recentBlockhash: String? = null
        private val instructions = mutableListOf<SolanaTransactionInstruction>()

        fun setVersion(version: TransactionVersion): Builder {
            this.version = version
            return this
        }

        fun setPayer(payer: PublicKey): Builder {
            this.payer = payer
            return this
        }

        fun setRecentBlockhash(blockhash: String): Builder {
            this.recentBlockhash = blockhash
            return this
        }

        fun addInstruction(instruction: SolanaTransactionInstruction): Builder {
            instructions.add(instruction)
            return this
        }

        fun addInstructions(instructions: List<SolanaTransactionInstruction>): Builder {
            this.instructions.addAll(instructions)
            return this
        }

        fun build(): VersionedTransaction {
            requireNotNull(payer) { "Payer is required" }
            requireNotNull(recentBlockhash) { "Recent blockhash is required" }
            require(instructions.isNotEmpty()) { "At least one instruction is required" }

            return VersionedTransaction(
                version = version,
                payer = payer!!,
                recentBlockhash = recentBlockhash!!,
                instructions = instructions.toList()
            )
        }
    }
}