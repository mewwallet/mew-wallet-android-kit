package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey

/**
 * Utilities for compiling account keys in a Solana transaction message.
 *
 * This handles the complex process of:
 * 1. Collecting all accounts from instructions (including program IDs)
 * 2. Deduplicating and merging flags for the same pubkey
 * 3. Sorting accounts in Solana's required order
 * 4. Generating the MessageHeader with account counts
 * 5. Replacing pubkeys with indices in instructions
 */
object CompiledKeys {

    /**
     * Deduplicates account metas by pubkey and merges their flags.
     *
     * If the same pubkey appears multiple times:
     * - isSigner = true if ANY occurrence is a signer
     * - isWritable = true if ANY occurrence is writable
     *
     * @param metas List of account metas (may contain duplicates)
     * @return Deduplicated list with merged flags
     */
    fun deduplicateAndMerge(metas: List<AccountMeta>): List<AccountMeta> {
        val merged = mutableMapOf<PublicKey, AccountMeta>()

        for (meta in metas) {
            val existing = merged[meta.pubkey]
            if (existing == null) {
                merged[meta.pubkey] = meta
            } else {
                // Merge flags: true if ANY occurrence is true
                merged[meta.pubkey] = AccountMeta(
                    pubkey = meta.pubkey,
                    isSigner = existing.isSigner || meta.isSigner,
                    isWritable = existing.isWritable || meta.isWritable
                )
            }
        }

        return merged.values.toList()
    }

    /**
     * Sorts account keys according to Solana's requirements and generates the MessageHeader.
     *
     * Sorting order:
     * 1. Fee payer (first position, writable signer)
     * 2. Other writable signers (lexicographic by Base58 address)
     * 3. Read-only signers (lexicographic by Base58 address)
     * 4. Writable non-signers (lexicographic by Base58 address)
     * 5. Read-only non-signers (lexicographic by Base58 address)
     *
     * @param metas Deduplicated account metas
     * @param feePayer The fee payer public key (must be in metas as a signer)
     * @return Pair of (sorted account keys, message header)
     */
    fun sortAndCreateHeader(metas: List<AccountMeta>, feePayer: PublicKey): Pair<List<PublicKey>, MessageHeader> {
        // Ensure fee payer is in the list and is a signer
        val feePayerMeta = metas.find { it.pubkey == feePayer }
            ?: throw IllegalArgumentException("Fee payer not found in account metas")

        if (!feePayerMeta.isSigner) {
            throw IllegalArgumentException("Fee payer must be a signer")
        }

        // Separate fee payer from other accounts
        val otherMetas = metas.filter { it.pubkey != feePayer }

        // Sort other accounts by category
        val writableSigners = otherMetas.filter { it.isSigner && it.isWritable }
            .sortedBy { it.pubkey.address()?.address }

        val readonlySigners = otherMetas.filter { it.isSigner && !it.isWritable }
            .sortedBy { it.pubkey.address()?.address }

        val writableNonSigners = otherMetas.filter { !it.isSigner && it.isWritable }
            .sortedBy { it.pubkey.address()?.address }

        val readonlyNonSigners = otherMetas.filter { !it.isSigner && !it.isWritable }
            .sortedBy { it.pubkey.address()?.address }

        // Build sorted account keys list
        val sortedKeys = mutableListOf<PublicKey>()

        // 1. Fee payer (always first)
        sortedKeys.add(feePayer)

        // 2. Writable signers
        sortedKeys.addAll(writableSigners.map { it.pubkey })

        // 3. Readonly signers
        sortedKeys.addAll(readonlySigners.map { it.pubkey })

        // 4. Writable non-signers
        sortedKeys.addAll(writableNonSigners.map { it.pubkey })

        // 5. Readonly non-signers
        sortedKeys.addAll(readonlyNonSigners.map { it.pubkey })

        // Calculate header counts
        val numRequiredSignatures = (1 + writableSigners.size + readonlySigners.size).toUByte()
        val numReadonlySignedAccounts = readonlySigners.size.toUByte()
        val numReadonlyUnsignedAccounts = readonlyNonSigners.size.toUByte()

        val header = MessageHeader(
            numRequiredSignatures = numRequiredSignatures,
            numReadonlySignedAccounts = numReadonlySignedAccounts,
            numReadonlyUnsignedAccounts = numReadonlyUnsignedAccounts
        )

        return Pair(sortedKeys, header)
    }

    /**
     * Compiles instructions by replacing pubkeys with indices into the account keys array.
     *
     * @param instructions Original instructions with pubkey references
     * @param accountKeys Sorted account keys (output from sortAndCreateHeader)
     * @return Compiled instructions with indices instead of pubkeys
     * @throws IllegalStateException if a pubkey is not found in accountKeys
     */
    fun compileInstructions(
        instructions: List<TransactionInstruction>,
        accountKeys: List<PublicKey>
    ): List<CompiledInstruction> {
        return instructions.map { instruction ->
            // Find program ID index
            val programIdIndex = accountKeys.indexOf(instruction.programId)
            if (programIdIndex == -1) {
                throw IllegalStateException("Program ID ${instruction.programId.address()?.address} not found in account keys")
            }

            // Find account indices
            val accountIndices = instruction.keys.map { meta ->
                val index = accountKeys.indexOf(meta.pubkey)
                if (index == -1) {
                    throw IllegalStateException("Account ${meta.pubkey.address()?.address} not found in account keys")
                }
                index.toUByte()
            }

            CompiledInstruction(
                programIdIndex = programIdIndex.toUByte(),
                accounts = accountIndices,
                data = instruction.data
            )
        }
    }

    /**
     * Collects all accounts referenced by instructions, including program IDs.
     *
     * Program IDs are added as read-only, non-signer accounts.
     *
     * @param instructions List of transaction instructions
     * @return List of all account metas (including program IDs)
     */
    fun collectAccounts(instructions: List<TransactionInstruction>): List<AccountMeta> {
        val accounts = mutableListOf<AccountMeta>()

        for (instruction in instructions) {
            // Add instruction's account keys
            accounts.addAll(instruction.keys)

            // Add program ID as readonly, non-signer
            accounts.add(AccountMeta(
                pubkey = instruction.programId,
                isSigner = false,
                isWritable = false
            ))
        }

        return accounts
    }
}
