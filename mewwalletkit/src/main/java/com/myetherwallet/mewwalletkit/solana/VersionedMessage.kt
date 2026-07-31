package com.myetherwallet.mewwalletkit.solana

import android.os.Parcelable
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import kotlinx.parcelize.Parcelize

/**
 * A versioned Solana transaction message.
 *
 * Wraps either a Legacy or V0 message format, providing a unified interface
 * for transaction processing regardless of version.
 *
 * Versions:
 * - Legacy: Original format with all accounts inline
 * - V0: Versioned format with Address Lookup Table support
 */
@Parcelize
sealed class VersionedMessage: Parcelable {
    /**
     * Transaction version (LEGACY or V0).
     */
    abstract val version: TransactionVersion

    /**
     * Message header with signer and readonly counts.
     */
    abstract val header: MessageHeader

    /**
     * Static account keys (not from ALTs).
     * - Legacy: All account keys
     * - V0: Only non-ALT account keys
     */
    abstract val staticAccountKeys: List<PublicKey>

    /**
     * Recent blockhash for transaction expiry (Base58-encoded).
     */
    abstract var recentBlockhash: String

    /**
     * Compiled instructions with account indices.
     */
    abstract val compiledInstructions: List<CompiledInstruction>

    /**
     * Gets all account keys in global index order.
     *
     * @param addressLookupTableAccounts ALT accounts for resolving v0 lookups
     * @return All account keys (static + ALT resolved)
     */
    abstract fun getAllAccountKeys(
        addressLookupTableAccounts: List<AddressLookupTableAccount> = emptyList()
    ): List<PublicKey>

    /**
     * Checks if an account (by global index) is a signer.
     *
     * @param index Global account index
     * @return true if the account is a required signer
     */
    abstract fun isAccountSigner(index: Int): Boolean

    /**
     * Checks if an account (by global index) is writable.
     *
     * @param index Global account index
     * @return true if the account has write access
     */
    abstract fun isAccountWritable(index: Int): Boolean

    /**
     * Legacy message (no versioning, all accounts inline).
     */
    data class Legacy(val message: Message) : VersionedMessage() {
        override val version: TransactionVersion = TransactionVersion.LEGACY
        override val header: MessageHeader = message.header
        override val staticAccountKeys: List<PublicKey> = message.accountKeys
        override var recentBlockhash: String
            get() = message.recentBlockhash
            set(value) {
                // Message is immutable, we need to handle this differently
                // This will be addressed when we integrate with Transaction class
            }
        override val compiledInstructions: List<CompiledInstruction> = message.instructions

        override fun getAllAccountKeys(
            addressLookupTableAccounts: List<AddressLookupTableAccount>
        ): List<PublicKey> = message.accountKeys

        override fun isAccountSigner(index: Int): Boolean {
            return index < header.numRequiredSignatures.toInt()
        }

        override fun isAccountWritable(index: Int): Boolean {
            val numSignedAccounts = header.numRequiredSignatures.toInt()

            return when {
                // Unsigned accounts region
                index >= numSignedAccounts -> {
                    val unsignedIndex = index - numSignedAccounts
                    val numUnsignedAccounts = staticAccountKeys.size - numSignedAccounts
                    val numWritableUnsigned =
                        numUnsignedAccounts - header.numReadonlyUnsignedAccounts.toInt()
                    unsignedIndex < numWritableUnsigned
                }

                // Signed accounts region
                else -> {
                    val numWritableSigned =
                        numSignedAccounts - header.numReadonlySignedAccounts.toInt()
                    index < numWritableSigned
                }
            }
        }
    }

    /**
     * Version 0 message (with Address Lookup Table support).
     */
    data class V0(val message: MessageV0) : VersionedMessage() {
        override val version: TransactionVersion = TransactionVersion.V0
        override val header: MessageHeader = message.header
        override val staticAccountKeys: List<PublicKey> = message.staticAccountKeys
        override var recentBlockhash: String
            get() = message.recentBlockhash
            set(value) {
                message.recentBlockhash = value
            }
        override val compiledInstructions: List<CompiledInstruction> = message.compiledInstructions

        /**
         * Address lookup tables referenced by this message.
         */
        val addressTableLookups: List<MessageAddressTableLookup> = message.addressTableLookups

        override fun getAllAccountKeys(
            addressLookupTableAccounts: List<AddressLookupTableAccount>
        ): List<PublicKey> = message.getAllAccountKeys(addressLookupTableAccounts)

        override fun isAccountSigner(index: Int): Boolean = message.isAccountSigner(index)

        override fun isAccountWritable(index: Int): Boolean = message.isAccountWritable(index)

        /**
         * Resolves account keys from Address Lookup Tables.
         *
         * @param addressLookupTableAccounts The ALT accounts to resolve from
         * @return Resolved account keys, separated by writability
         * @throws IllegalArgumentException if a referenced ALT is missing or index is invalid
         */
        fun resolveAddressTableLookups(
            addressLookupTableAccounts: List<AddressLookupTableAccount>
        ): AccountKeysFromLookups = message.resolveAddressTableLookups(addressLookupTableAccounts)
    }

    companion object {
        /**
         * Creates a VersionedMessage from a Legacy message.
         */
        fun legacy(message: Message): VersionedMessage = Legacy(message)

        /**
         * Creates a VersionedMessage from a V0 message.
         */
        fun v0(message: MessageV0): VersionedMessage = V0(message)
    }
}
