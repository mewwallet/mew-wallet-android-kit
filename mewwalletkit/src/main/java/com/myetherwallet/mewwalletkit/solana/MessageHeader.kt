package com.myetherwallet.mewwalletkit.solana

/**
 * Header metadata for a Solana transaction message.
 *
 * Describes how accounts are distributed among different categories:
 * - Required signers (accounts that must sign the transaction)
 * - Readonly signed accounts (signers whose data won't be modified)
 * - Readonly unsigned accounts (non-signers whose data won't be modified)
 *
 * The total number of accounts is implicitly defined by the account keys array.
 * The header allows the runtime to determine account permissions without inspecting each account.
 *
 * @property numRequiredSignatures Total number of signatures required (both writable and readonly signers)
 * @property numReadonlySignedAccounts Number of readonly accounts that are also signers
 * @property numReadonlyUnsignedAccounts Number of readonly accounts that are not signers
 */
data class MessageHeader(
    val numRequiredSignatures: UByte,
    val numReadonlySignedAccounts: UByte,
    val numReadonlyUnsignedAccounts: UByte
)
