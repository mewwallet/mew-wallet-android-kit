package com.myetherwallet.mewwalletkit.solana.instruction

import android.os.Parcelable
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.solana.encoding.BinaryWriter
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Represents a single instruction in a Solana transaction
 */
@Parcelize
data class SolanaTransactionInstruction(
    /**
     * The program ID that will process this instruction
     */
    val programId: @RawValue PublicKey,

    /**
     * Accounts that this instruction will read from and/or write to
     */
    val accounts: List<SolanaAccountMeta>,

    /**
     * Program-specific instruction data
     */
    val data: ByteArray
) : Parcelable {

    /**
     * Serialize this instruction to bytes
     */
    fun serialize(): ByteArray {
        return BinaryWriter.write { writer ->
            // Write program ID index (will be resolved during compilation)
            writer.writeByte(0) // Placeholder - will be set during transaction compilation

            // Write accounts array
            writer.writeArray(accounts) { accountMeta, w ->
                w.writeByte(0) // Account index placeholder
            }

            // Write instruction data
            writer.writeArray(data.asList()) { byte, w ->
                w.writeByte(byte)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SolanaTransactionInstruction

        if (programId != other.programId) return false
        if (accounts != other.accounts) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = programId.hashCode()
        result = 31 * result + accounts.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }

    companion object {
        /**
         * Create a simple instruction with no accounts and empty data
         */
        fun simple(programId: PublicKey, data: ByteArray = byteArrayOf()): SolanaTransactionInstruction {
            return SolanaTransactionInstruction(
                programId = programId,
                accounts = emptyList(),
                data = data
            )
        }
    }
}

/**
 * Represents account metadata for a transaction instruction
 */
@Parcelize
data class SolanaAccountMeta(
    /**
     * The account's public key
     */
    val publicKey: @RawValue PublicKey,

    /**
     * True if this account is a signer of the transaction
     */
    val isSigner: Boolean,

    /**
     * True if this account's data may be modified
     */
    val isWritable: Boolean
) : Parcelable {

    companion object {
        /**
         * Create a read-only account meta
         */
        fun readOnly(publicKey: PublicKey): SolanaAccountMeta {
            return SolanaAccountMeta(publicKey, isSigner = false, isWritable = false)
        }

        /**
         * Create a writable account meta
         */
        fun writable(publicKey: PublicKey): SolanaAccountMeta {
            return SolanaAccountMeta(publicKey, isSigner = false, isWritable = true)
        }

        /**
         * Create a signer account meta (also writable by default)
         */
        fun signer(publicKey: PublicKey, isWritable: Boolean = true): SolanaAccountMeta {
            return SolanaAccountMeta(publicKey, isSigner = true, isWritable = isWritable)
        }
    }
}