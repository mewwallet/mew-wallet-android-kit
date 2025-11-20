package com.myetherwallet.mewwalletkit.solana

import android.os.Parcelable
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import kotlinx.parcelize.Parcelize

/**
 * A reference to an Address Lookup Table in a transaction message.
 *
 * Specifies which ALT to use and which account indices to read from it.
 * Accounts are categorized as writable or readonly.
 *
 * Wire format:
 * - accountKey (32 bytes)
 * - shortvec(writableIndexes.size) + raw u8[]
 * - shortvec(readonlyIndexes.size) + raw u8[]
 *
 * @property accountKey The on-chain address of the ALT account to read from
 * @property writableIndexes Indices of writable accounts in the ALT (max 255)
 * @property readonlyIndexes Indices of readonly accounts in the ALT (max 255)
 */
@Parcelize
data class MessageAddressTableLookup(
    val accountKey: PublicKey,
    val writableIndexes: List<UByte>,
    val readonlyIndexes: List<UByte>
): Parcelable {
    init {
        require(writableIndexes.size <= 255) {
            "writableIndexes cannot exceed 255, got ${writableIndexes.size}"
        }
        require(readonlyIndexes.size <= 255) {
            "readonlyIndexes cannot exceed 255, got ${readonlyIndexes.size}"
        }
    }

    /**
     * Total number of account references from this ALT.
     */
    val numLookupAccounts: Int
        get() = writableIndexes.size + readonlyIndexes.size
}
