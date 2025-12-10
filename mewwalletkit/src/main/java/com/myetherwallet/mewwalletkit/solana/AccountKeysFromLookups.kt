package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey

/**
 * Account keys resolved from Address Lookup Tables.
 *
 * Holds the accounts extracted from ALTs, separated by writability.
 * Used to expand compact ALT references into full account lists.
 */
data class AccountKeysFromLookups(
    /**
     * Writable accounts from ALTs (in order of lookup).
     */
    val writable: MutableList<PublicKey> = mutableListOf(),

    /**
     * Readonly accounts from ALTs (in order of lookup).
     */
    val readonly: MutableList<PublicKey> = mutableListOf()
) {
    /**
     * Total number of accounts resolved from lookups.
     */
    val numLookupAccounts: Int
        get() = writable.size + readonly.size

    /**
     * Gets all accounts in the correct order: writable first, then readonly.
     */
    fun allAccounts(): List<PublicKey> = writable + readonly
}
