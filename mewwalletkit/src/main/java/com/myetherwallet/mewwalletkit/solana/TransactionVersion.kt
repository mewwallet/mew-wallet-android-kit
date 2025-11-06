package com.myetherwallet.mewwalletkit.solana

/**
 * Solana transaction version.
 *
 * Legacy: Original transaction format with no version prefix, all accounts inline
 * V0: Versioned transaction format with 0x80 prefix, supports Address Lookup Tables
 */
enum class TransactionVersion(val prefix: Byte?) {
    /**
     * Legacy transaction format (no version prefix byte).
     * All accounts are included inline in the message.
     */
    LEGACY(null),

    /**
     * Version 0 transaction format (0x80 prefix byte).
     * Supports Address Lookup Tables for compact account references.
     */
    V0(0x80.toByte());

    companion object {
        /** Version prefix mask (high bit set) */
        const val VERSION_PREFIX_MASK: Int = 0x80

        /**
         * Determines transaction version from the first byte of serialized data.
         *
         * @param byte First byte of serialized transaction/message
         * @return Transaction version
         * @throws UnsupportedOperationException if version is not supported
         */
        fun fromByte(byte: Byte): TransactionVersion {
            val isVersioned = (byte.toInt() and VERSION_PREFIX_MASK) != 0
            return if (isVersioned) {
                val versionNumber = byte.toInt() and 0x7F
                when (versionNumber) {
                    0 -> V0
                    else -> throw UnsupportedOperationException(
                        "Transaction version $versionNumber is not supported"
                    )
                }
            } else {
                LEGACY
            }
        }
    }
}
