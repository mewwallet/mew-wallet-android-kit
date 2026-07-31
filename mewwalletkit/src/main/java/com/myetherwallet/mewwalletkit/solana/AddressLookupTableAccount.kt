package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey

/**
 * The on-chain state of an Address Lookup Table.
 *
 * Address Lookup Tables (ALTs) are on-chain accounts that store lists of addresses,
 * allowing transactions to reference accounts by u8 index instead of full 32-byte address.
 */
data class AddressLookupTableState(
    /**
     * Deactivation slot. ULong.MAX_VALUE means the table is active.
     */
    val deactivationSlot: ULong,

    /**
     * Last slot when the table was extended with new addresses.
     */
    val lastExtendedSlot: ULong,

    /**
     * Start index for addresses added in lastExtendedSlot.
     */
    val lastExtendedSlotStartIndex: UByte,

    /**
     * Authority that can modify the table. Null means the table is finalized (immutable).
     */
    val authority: PublicKey?,

    /**
     * List of addresses stored in this lookup table.
     * Can be referenced by index (0-255) in transactions.
     */
    val addresses: List<PublicKey>
)

/**
 * An Address Lookup Table account.
 *
 * Combines the on-chain account address (key) with its state (address list).
 * ALTs enable compact transaction encoding by referencing accounts via u8 index.
 *
 * @property key The on-chain address of this ALT account
 * @property state The current state of the ALT (addresses, authority, etc.)
 */
data class AddressLookupTableAccount(
    val key: PublicKey,
    val state: AddressLookupTableState
) {
    /**
     * Whether this ALT is active (not deactivated).
     */
    val isActive: Boolean
        get() = state.deactivationSlot == ULong.MAX_VALUE

    /**
     * Gets an address from the table by index.
     *
     * @param index Index in the address list (0-255)
     * @return PublicKey at the given index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    fun getAddress(index: UByte): PublicKey {
        require(index.toInt() < state.addresses.size) {
            "Index ${index.toInt()} out of bounds for ALT with ${state.addresses.size} addresses"
        }
        return state.addresses[index.toInt()]
    }
}
