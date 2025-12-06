package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.Network
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.core.util.toLittleEndianBytes

/**
 * Factory for constructing instructions to the Compute Budget program.
 *
 * The Compute Budget program lets you:
 * - Request a larger per-program heap frame
 * - Set a transaction-wide compute unit limit
 * - Set a price (in micro-lamports per CU) for priority fees
 *
 * All three instructions carry no account metas (keys: []) and only a small
 * binary payload: u8 opcode followed by a little-endian integer.
 */
object ComputeBudgetProgram {

    /**
     * Instruction opcodes.
     *
     * Encoded as a single byte at the start of the instruction data.
     */
    object InstructionIndex {
        const val REQUEST_HEAP_FRAME: UByte = 1u
        const val SET_COMPUTE_UNIT_LIMIT: UByte = 2u
        const val SET_COMPUTE_UNIT_PRICE: UByte = 3u
    }

    /**
     * Public key for the Compute Budget program.
     *
     * Mainnet address: ComputeBudget111111111111111111111111111111
     */
    fun programId(): PublicKey {
        return PublicKey.createWithBase58(
            "ComputeBudget111111111111111111111111111111",
            Network.SOLANA
        ) ?: throw IllegalStateException("Failed to create ComputeBudgetProgram ID")
    }

    /**
     * Builds a RequestHeapFrame instruction.
     *
     * This instruction requests an increased per-program heap size for all
     * invoked programs within the transaction. It must be issued before
     * any other instructions in the transaction to take effect.
     *
     * Layout:
     * ```
     * data = [ opcode: u8 = 1 ] || [ bytes: u32 LE ]
     * ```
     *
     * Notes:
     * - The requested size must be a multiple of 1024 bytes
     * - Default heap size is 32 KiB (32768 bytes)
     * - Maximum is cluster-dependent (often 256 KiB or 512 KiB)
     *
     * @param bytes Requested transaction-wide program heap size in bytes (must be multiple of 1024)
     * @return TransactionInstruction with keys: []
     */
    fun requestHeapFrame(bytes: UInt): TransactionInstruction {
        val data = byteArrayOf(InstructionIndex.REQUEST_HEAP_FRAME.toByte()) +
                bytes.toLittleEndianBytes()

        return TransactionInstruction(
            programId = programId(),
            keys = emptyList(),
            data = data
        )
    }

    /**
     * Builds a SetComputeUnitLimit instruction.
     *
     * Sets a transaction-wide upper bound on the number of compute units
     * available for all instructions in the transaction.
     *
     * Layout:
     * ```
     * data = [ opcode: u8 = 2 ] || [ units: u32 LE ]
     * ```
     *
     * Notes:
     * - Default limit: 200,000 compute units
     * - Maximum allowed: 1,400,000 CU (cluster-dependent)
     *
     * @param units Maximum compute units for the transaction
     * @return TransactionInstruction with keys: []
     */
    fun setComputeUnitLimit(units: UInt): TransactionInstruction {
        val data = byteArrayOf(InstructionIndex.SET_COMPUTE_UNIT_LIMIT.toByte()) +
                units.toLittleEndianBytes()

        return TransactionInstruction(
            programId = programId(),
            keys = emptyList(),
            data = data
        )
    }

    fun getComputeUnitLimit(data: ByteArray): UInt? {
        if (data[0] == InstructionIndex.SET_COMPUTE_UNIT_LIMIT.toByte() && data.size == 5) {
            val decodedUnits = (data[1].toInt() and 0xFF) or
                    ((data[2].toInt() and 0xFF) shl 8) or
                    ((data[3].toInt() and 0xFF) shl 16) or
                    ((data[4].toInt() and 0xFF) shl 24)
            return decodedUnits.toUInt()
        }
        return null
    }

    /**
     * Builds a SetComputeUnitPrice instruction.
     *
     * Specifies the price (in micro-lamports per compute unit) to prioritize
     * the transaction in fee markets. Higher prices give higher priority.
     *
     * Layout:
     * ```
     * data = [ opcode: u8 = 3 ] || [ microLamports: u64 LE ]
     * ```
     *
     * Notes:
     * - 1 micro-lamport = 1 × 10⁻⁶ lamports per compute unit
     * - Used by block producers to determine priority fees
     *
     * @param microLamports Price per compute unit (in micro-lamports)
     * @return TransactionInstruction with keys: []
     */
    fun setComputeUnitPrice(microLamports: ULong): TransactionInstruction {
        val data = byteArrayOf(InstructionIndex.SET_COMPUTE_UNIT_PRICE.toByte()) +
                microLamports.toLittleEndianBytes()

        return TransactionInstruction(
            programId = programId(),
            keys = emptyList(),
            data = data
        )
    }


    fun getComputeUnitPrice(data: ByteArray): ULong? {
        if (data[0] == InstructionIndex.SET_COMPUTE_UNIT_PRICE.toByte() && data.size == 9) {
            val decodedMicroLamports = (data[1].toLong() and 0xFF) or
                    ((data[2].toLong() and 0xFF) shl 8) or
                    ((data[3].toLong() and 0xFF) shl 16) or
                    ((data[4].toLong() and 0xFF) shl 24) or
                    ((data[5].toLong() and 0xFF) shl 32) or
                    ((data[6].toLong() and 0xFF) shl 40) or
                    ((data[7].toLong() and 0xFF) shl 48) or
                    ((data[8].toLong() and 0xFF) shl 56)
            return decodedMicroLamports.toULong()
        }
        return null
    }
}
