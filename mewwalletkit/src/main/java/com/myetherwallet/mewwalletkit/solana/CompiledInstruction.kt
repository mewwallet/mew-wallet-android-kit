package com.myetherwallet.mewwalletkit.solana

/**
 * A compiled transaction instruction with account keys replaced by indices.
 *
 * After message compilation, all PublicKey references are replaced with indices
 * into the message's account keys array. This reduces transaction size and allows
 * the runtime to efficiently locate accounts.
 *
 * @property programIdIndex Index into account keys array pointing to the program to invoke
 * @property accounts Indices into account keys array for accounts this instruction operates on
 * @property data The instruction-specific data to pass to the program
 */
data class CompiledInstruction(
    val programIdIndex: UByte,
    val accounts: List<UByte>,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CompiledInstruction

        if (programIdIndex != other.programIdIndex) return false
        if (accounts != other.accounts) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = programIdIndex.hashCode()
        result = 31 * result + accounts.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
