package com.myetherwallet.mewwalletkit.solana.transaction

import android.os.Parcelable
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.solana.encoding.BinaryWriter
import io.github.novacrypto.base58.Base58
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Represents a compiled Solana transaction message
 */
@Parcelize
data class SolanaMessage(
    /**
     * All accounts referenced by this transaction
     */
    val accountKeys: List<@RawValue PublicKey>,

    /**
     * Recent blockhash for transaction validity
     */
    val recentBlockhash: String,

    /**
     * Compiled instructions
     */
    val instructions: List<SolanaCompiledInstruction>,

    /**
     * Number of required signatures
     */
    val numRequiredSignatures: Byte,

    /**
     * Number of readonly signed accounts
     */
    val numReadonlySignedAccounts: Byte,

    /**
     * Number of readonly unsigned accounts
     */
    val numReadonlyUnsignedAccounts: Byte
) : Parcelable {

    /**
     * Serialize this message to bytes
     */
    fun serialize(): ByteArray {
        return BinaryWriter.write { writer ->
            // Write header
            writer.writeByte(numRequiredSignatures)
            writer.writeByte(numReadonlySignedAccounts)
            writer.writeByte(numReadonlyUnsignedAccounts)

            // Write account keys
            writer.writeArray(accountKeys) { publicKey, w ->
                w.writePublicKey(publicKey)
            }

            // Write recent blockhash
            val blockhashBytes = Base58.base58Decode(recentBlockhash)
            writer.writeBytes(blockhashBytes)

            // Write instructions
            writer.writeArray(instructions) { instruction, w ->
                w.writeByte(instruction.programIdIndex)

                // Write account indices
                w.writeArray(instruction.accountIndices) { index, writer2 ->
                    writer2.writeByte(index)
                }

                // Write instruction data
                w.writeArray(instruction.data.asList()) { byte, writer2 ->
                    writer2.writeByte(byte)
                }
            }
        }
    }

    /**
     * Get the fee payer account (first account)
     */
    fun getFeePayer(): PublicKey? {
        return accountKeys.firstOrNull()
    }

    /**
     * Check if account at index is signer
     */
    fun isAccountSigner(index: Int): Boolean {
        return index < numRequiredSignatures
    }

    /**
     * Check if account at index is writable
     */
    fun isAccountWritable(index: Int): Boolean {
        if (index < numRequiredSignatures) {
            // Signed account
            return index < (numRequiredSignatures - numReadonlySignedAccounts)
        } else {
            // Unsigned account
            val unsignedIndex = index - numRequiredSignatures
            val writableUnsignedAccounts = accountKeys.size - numRequiredSignatures - numReadonlyUnsignedAccounts
            return unsignedIndex < writableUnsignedAccounts
        }
    }
}

/**
 * Represents a compiled instruction with account indices
 */
@Parcelize
data class SolanaCompiledInstruction(
    /**
     * Index of the program account in the message's account keys
     */
    val programIdIndex: Byte,

    /**
     * Indices of accounts used by this instruction
     */
    val accountIndices: List<Byte>,

    /**
     * Program-specific instruction data
     */
    val data: ByteArray
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SolanaCompiledInstruction

        if (programIdIndex != other.programIdIndex) return false
        if (accountIndices != other.accountIndices) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = programIdIndex.toInt()
        result = 31 * result + accountIndices.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}