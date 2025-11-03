package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.core.util.toLittleEndianBytes

/**
 * System Program instruction types and encoding.
 *
 * Instruction indices:
 * 0 = CreateAccount
 * 1 = Assign
 * 2 = Transfer
 * 3 = CreateAccountWithSeed
 * 4 = AdvanceNonceAccount
 * 5 = WithdrawNonceAccount
 * 6 = InitializeNonceAccount
 * 7 = AuthorizeNonceAccount
 * 8 = Allocate
 * 9 = AllocateWithSeed
 * 10 = AssignWithSeed
 * 11 = TransferWithSeed
 * 12 = UpgradeNonceAccount
 */
object SystemInstruction {

    /**
     * Creates a Transfer instruction.
     *
     * Wire format:
     * - Byte 0: instruction index (2 for Transfer)
     * - Bytes 1-8: lamports (u64 little-endian)
     *
     * Accounts:
     * - [0] Source account (signer, writable)
     * - [1] Destination account (writable)
     *
     * @param fromPubkey Source account
     * @param toPubkey Destination account
     * @param lamports Amount to transfer
     * @return TransactionInstruction
     */
    fun transfer(fromPubkey: PublicKey, toPubkey: PublicKey, lamports: ULong): TransactionInstruction {
        // Instruction index 2 = Transfer
        val instructionIndex: UInt = 2u

        // Encode instruction data: [index:4 bytes] + [lamports:8 bytes]
        val data = instructionIndex.toLittleEndianBytes() + lamports.toLittleEndianBytes()

        return TransactionInstruction(
            keys = listOf(
                AccountMeta(pubkey = fromPubkey, isSigner = true, isWritable = true),
                AccountMeta(pubkey = toPubkey, isSigner = false, isWritable = true)
            ),
            programId = SystemProgram.programId(),
            data = data
        )
    }

    /**
     * Creates a CreateAccount instruction.
     *
     * Wire format:
     * - Bytes 0-3: instruction index (0 for CreateAccount, u32 little-endian)
     * - Bytes 4-11: lamports (u64 little-endian)
     * - Bytes 12-19: space (u64 little-endian)
     * - Bytes 20-51: owner program ID (32 bytes)
     *
     * Accounts:
     * - [0] Funding account (signer, writable)
     * - [1] New account (signer, writable)
     *
     * @param fromPubkey Funding account
     * @param newAccountPubkey New account to create
     * @param lamports Amount to transfer to new account
     * @param space Bytes to allocate
     * @param owner Program that will own the account
     * @return TransactionInstruction
     */
    fun createAccount(
        fromPubkey: PublicKey,
        newAccountPubkey: PublicKey,
        lamports: ULong,
        space: ULong,
        owner: PublicKey
    ): TransactionInstruction {
        // Instruction index 0 = CreateAccount
        val instructionIndex: UInt = 0u

        // Encode instruction data
        var data = byteArrayOf()
        data += instructionIndex.toLittleEndianBytes()
        data += lamports.toLittleEndianBytes()
        data += space.toLittleEndianBytes()
        data += owner.data()

        return TransactionInstruction(
            keys = listOf(
                AccountMeta(pubkey = fromPubkey, isSigner = true, isWritable = true),
                AccountMeta(pubkey = newAccountPubkey, isSigner = true, isWritable = true)
            ),
            programId = SystemProgram.programId(),
            data = data
        )
    }
}
