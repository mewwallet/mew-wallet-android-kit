package com.myetherwallet.mewwalletkit.solana

import android.os.Parcelable
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import kotlinx.parcelize.Parcelize

/**
 * Solana transaction message format v0.
 *
 * Version 0 (v0) messages support Address Lookup Tables (ALTs) for compact account references.
 * Serialized with 0x80 version prefix.
 *
 * Wire format:
 * - Version prefix: 0x80
 * - Message header (3 bytes)
 * - Compact array: Static account keys (non-ALT)
 * - Recent blockhash (32 bytes)
 * - Compact array: Compiled instructions
 * - Compact array: Address lookup tables
 *
 * Account key ordering (global indices):
 * - [0..N-1]:       Static account keys
 * - [N..N+W-1]:     Writable accounts from ALTs
 * - [N+W..N+W+R-1]: Readonly accounts from ALTs
 *
 * @property header Message header with signer and readonly counts
 * @property staticAccountKeys Non-ALT account keys (signers, program IDs, frequently used)
 * @property recentBlockhash Recent blockhash for transaction expiry
 * @property compiledInstructions Instructions with account indices
 * @property addressTableLookups ALT references with writable/readonly index lists
 */
@Parcelize
data class MessageV0(
    val header: MessageHeader,
    val staticAccountKeys: List<PublicKey>,
    var recentBlockhash: String,
    val compiledInstructions: List<CompiledInstruction>,
    var addressTableLookups: List<MessageAddressTableLookup> = emptyList()
): Parcelable {
    /**
     * Transaction version (always V0).
     */
    val version: TransactionVersion = TransactionVersion.V0

    /**
     * Total number of accounts referenced from ALTs.
     */
    val numAccountKeysFromLookups: Int
        get() = addressTableLookups.sumOf { it.numLookupAccounts }

    /**
     * Total number of accounts (static + ALT).
     */
    val numAccountKeys: Int
        get() = staticAccountKeys.size + numAccountKeysFromLookups

    /**
     * Checks if an account (by global index) is a signer.
     *
     * @param index Global account index
     * @return true if the account is a required signer
     */
    fun isAccountSigner(index: Int): Boolean {
        return index < header.numRequiredSignatures.toInt()
    }

    /**
     * Checks if an account (by global index) is writable.
     *
     * Account writability depends on:
     * - Static accounts: Header writable counts
     * - ALT accounts: Writable vs readonly index lists
     *
     * @param index Global account index
     * @return true if the account has write access
     */
    fun isAccountWritable(index: Int): Boolean {
        val numSignedAccounts = header.numRequiredSignatures.toInt()
        val numStaticAccountKeys = staticAccountKeys.size

        return when {
            // ALT accounts region
            index >= numStaticAccountKeys -> {
                val lookupIndex = index - numStaticAccountKeys
                val numWritableLookups = addressTableLookups.sumOf {
                    it.writableIndexes.size
                }
                // Writable ALT accounts come first
                lookupIndex < numWritableLookups
            }

            // Unsigned static accounts region
            index >= numSignedAccounts -> {
                val unsignedIndex = index - numSignedAccounts
                val numUnsignedAccounts = numStaticAccountKeys - numSignedAccounts
                val numWritableUnsigned =
                    numUnsignedAccounts - header.numReadonlyUnsignedAccounts.toInt()
                unsignedIndex < numWritableUnsigned
            }

            // Signed static accounts region
            else -> {
                val numWritableSigned =
                    numSignedAccounts - header.numReadonlySignedAccounts.toInt()
                index < numWritableSigned
            }
        }
    }

    /**
     * Resolves account keys from Address Lookup Tables.
     *
     * Expands compact u8 indices into full PublicKey references by looking up
     * addresses in the provided ALT accounts.
     *
     * @param addressLookupTableAccounts The ALT accounts to resolve from
     * @return Resolved account keys, separated by writability
     * @throws IllegalArgumentException if a referenced ALT is missing or index is invalid
     */
    fun resolveAddressTableLookups(
        addressLookupTableAccounts: List<AddressLookupTableAccount>
    ): AccountKeysFromLookups {
        val result = AccountKeysFromLookups()

        addressTableLookups.forEach { lookup ->
            // Find the ALT account
            val table = addressLookupTableAccounts.find { it.key == lookup.accountKey }
                ?: throw IllegalArgumentException(
                    "Address lookup table not found: ${lookup.accountKey.address()?.address}"
                )

            val addresses = table.state.addresses

            // Resolve writable account indices
            lookup.writableIndexes.forEach { index ->
                require(index.toInt() < addresses.size) {
                    "Invalid writable index ${index.toInt()} for ALT ${lookup.accountKey.address()?.address} " +
                            "(has ${addresses.size} addresses)"
                }
                result.writable.add(addresses[index.toInt()])
            }

            // Resolve readonly account indices
            lookup.readonlyIndexes.forEach { index ->
                require(index.toInt() < addresses.size) {
                    "Invalid readonly index ${index.toInt()} for ALT ${lookup.accountKey.address()?.address} " +
                            "(has ${addresses.size} addresses)"
                }
                result.readonly.add(addresses[index.toInt()])
            }
        }

        return result
    }

    /**
     * Gets all account keys in global index order.
     *
     * Requires ALT accounts to be resolved first.
     *
     * @param addressLookupTableAccounts The ALT accounts to resolve from
     * @return All account keys: static + ALT (writable + readonly)
     */
    fun getAllAccountKeys(
        addressLookupTableAccounts: List<AddressLookupTableAccount> = emptyList()
    ): List<PublicKey> {
        if (addressTableLookups.isEmpty()) {
            return staticAccountKeys
        }

        val lookupKeys = resolveAddressTableLookups(addressLookupTableAccounts)
        return staticAccountKeys + lookupKeys.allAccounts()
    }
}
