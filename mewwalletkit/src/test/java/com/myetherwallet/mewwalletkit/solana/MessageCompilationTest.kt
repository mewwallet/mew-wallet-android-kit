package com.myetherwallet.mewwalletkit.solana

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for Solana message compilation (Phase 2)
 */
class MessageCompilationTest {

    @Test
    fun `test account deduplication merges flags`() {
        val keys = TestHelpers.createTestPublicKeys(2)
        val pubkey = keys[0]

        val metas = listOf(
            AccountMeta(pubkey, isSigner = true, isWritable = false),
            AccountMeta(pubkey, isSigner = false, isWritable = true),
            AccountMeta(keys[1], isSigner = true, isWritable = true)
        )

        val deduplicated = CompiledKeys.deduplicateAndMerge(metas)

        assertEquals("Should have 2 unique keys", 2, deduplicated.size)

        val merged = deduplicated.find { it.pubkey == pubkey }!!
        assertTrue("Should be signer (merged from first occurrence)", merged.isSigner)
        assertTrue("Should be writable (merged from second occurrence)", merged.isWritable)
    }

    @Test
    fun `test account sorting order`() {
        val keys = TestHelpers.createTestPublicKeys(6)

        val feePayer = keys[0]
        val writableSigner1 = keys[1]
        val writableSigner2 = keys[2]
        val readonlySigner = keys[3]
        val writableNonSigner = keys[4]
        val readonlyNonSigner = keys[5]

        val metas = listOf(
            AccountMeta(readonlySigner, isSigner = true, isWritable = false),
            AccountMeta(writableNonSigner, isSigner = false, isWritable = true),
            AccountMeta(feePayer, isSigner = true, isWritable = true),
            AccountMeta(readonlyNonSigner, isSigner = false, isWritable = false),
            AccountMeta(writableSigner2, isSigner = true, isWritable = true),
            AccountMeta(writableSigner1, isSigner = true, isWritable = true)
        )

        val (sortedKeys, header) = CompiledKeys.sortAndCreateHeader(metas, feePayer)

        // Fee payer must be first
        assertEquals("Fee payer must be first", feePayer, sortedKeys[0])

        // Check that signers come before non-signers
        val signerIndices = sortedKeys.indices.filter { index ->
            val key = sortedKeys[index]
            metas.find { it.pubkey == key }?.isSigner == true
        }
        val nonSignerIndices = sortedKeys.indices.filter { index ->
            val key = sortedKeys[index]
            metas.find { it.pubkey == key }?.isSigner == false
        }

        assertTrue("All signers should come before non-signers",
            signerIndices.max() < nonSignerIndices.min())

        // Verify header counts
        assertEquals("Should have 4 required signatures", 4.toUByte(), header.numRequiredSignatures)
        assertEquals("Should have 1 readonly signed account", 1.toUByte(), header.numReadonlySignedAccounts)
        assertEquals("Should have 1 readonly unsigned account", 1.toUByte(), header.numReadonlyUnsignedAccounts)
    }

    @Test
    fun `test fee payer must be signer`() {
        val keys = TestHelpers.createTestPublicKeys(2)
        val feePayer = keys[0]

        val metas = listOf(
            AccountMeta(feePayer, isSigner = false, isWritable = true) // Not a signer!
        )

        try {
            CompiledKeys.sortAndCreateHeader(metas, feePayer)
            fail("Should throw exception when fee payer is not a signer")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should have correct error message",
                e.message?.contains("must be a signer") == true)
        }
    }

    @Test
    fun `test fee payer not in metas throws exception`() {
        val keys = TestHelpers.createTestPublicKeys(2)
        val feePayer = keys[0]
        val otherKey = keys[1]

        val metas = listOf(
            AccountMeta(otherKey, isSigner = true, isWritable = true)
        )

        try {
            CompiledKeys.sortAndCreateHeader(metas, feePayer)
            fail("Should throw exception when fee payer not in metas")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should have correct error message",
                e.message?.contains("not found") == true)
        }
    }

    @Test
    fun `test instruction compilation replaces pubkeys with indices`() {
        val keys = TestHelpers.createTestPublicKeys(4)
        val programId = keys[0]
        val account1 = keys[1]
        val account2 = keys[2]
        val account3 = keys[3]

        val instructions = listOf(
            TransactionInstruction(
                keys = listOf(
                    AccountMeta(account1, isSigner = true, isWritable = true),
                    AccountMeta(account2, isSigner = false, isWritable = false)
                ),
                programId = programId,
                data = byteArrayOf(1, 2, 3)
            )
        )

        val accountKeys = listOf(account3, account1, account2, programId) // Some arbitrary order

        val compiled = CompiledKeys.compileInstructions(instructions, accountKeys)

        assertEquals("Should have 1 compiled instruction", 1, compiled.size)

        val instruction = compiled[0]
        assertEquals("Program ID should be at index 3", 3.toUByte(), instruction.programIdIndex)
        assertEquals("Should have 2 account indices", 2, instruction.accounts.size)
        assertEquals("Account1 should be at index 1", 1.toUByte(), instruction.accounts[0])
        assertEquals("Account2 should be at index 2", 2.toUByte(), instruction.accounts[1])
    }

    @Test
    fun `test collect accounts includes program IDs`() {
        val keys = TestHelpers.createTestPublicKeys(3)
        val programId = keys[0]
        val account1 = keys[1]

        val instructions = listOf(
            TransactionInstruction(
                keys = listOf(AccountMeta(account1, isSigner = true, isWritable = true)),
                programId = programId,
                data = byteArrayOf()
            )
        )

        val collected = CompiledKeys.collectAccounts(instructions)

        assertEquals("Should collect 2 accounts (1 key + 1 program)", 2, collected.size)

        val programMeta = collected.find { it.pubkey == programId }!!
        assertFalse("Program ID should not be signer", programMeta.isSigner)
        assertFalse("Program ID should not be writable", programMeta.isWritable)
    }

    @Test
    fun `test transaction compileMessage with simple transfer`() {
        val keys = TestHelpers.createTestPublicKeys(3)
        val sender = keys[0]
        val receiver = keys[1]
        val programId = keys[2]

        val transaction = Transaction(
            feePayer = sender,
            recentBlockhash = "4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ"
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(
                    AccountMeta(sender, isSigner = true, isWritable = true),
                    AccountMeta(receiver, isSigner = false, isWritable = true)
                ),
                programId = programId,
                data = byteArrayOf(2, 0, 0, 0, 0xE8.toByte(), 0x03, 0, 0, 0, 0, 0, 0) // Transfer 1000 lamports
            )
        )

        val message = transaction.compileMessage()

        assertNotNull("Message should not be null", message)
        assertEquals("Should have correct blockhash", "4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ", message.recentBlockhash)

        // Verify account keys order
        assertEquals("Should have 3 account keys", 3, message.accountKeys.size)
        assertEquals("Fee payer should be first", sender, message.accountKeys[0])

        // Verify header
        assertEquals("Should have 1 required signature", 1.toUByte(), message.header.numRequiredSignatures)
        assertEquals("Should have 0 readonly signed accounts", 0.toUByte(), message.header.numReadonlySignedAccounts)
        assertEquals("Should have 1 readonly unsigned account (program)", 1.toUByte(), message.header.numReadonlyUnsignedAccounts)

        // Verify compiled instructions
        assertEquals("Should have 1 compiled instruction", 1, message.instructions.size)
    }

    @Test
    fun `test compileMessage caches result`() {
        val keys = TestHelpers.createTestPublicKeys(2)
        val sender = keys[0]
        val programId = keys[1]

        val transaction = Transaction(
            feePayer = sender,
            recentBlockhash = "test-blockhash"
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(sender, isSigner = true, isWritable = true)),
                programId = programId,
                data = byteArrayOf()
            )
        )

        val message1 = transaction.compileMessage()
        val message2 = transaction.compileMessage()

        assertSame("Should return same cached instance", message1, message2)
    }

    @Test
    fun `test adding instruction invalidates cache`() {
        val keys = TestHelpers.createTestPublicKeys(2)
        val sender = keys[0]
        val programId = keys[1]

        val transaction = Transaction(
            feePayer = sender,
            recentBlockhash = "test-blockhash"
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(sender, isSigner = true, isWritable = true)),
                programId = programId,
                data = byteArrayOf(1)
            )
        )

        val message1 = transaction.compileMessage()

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(sender, isSigner = true, isWritable = true)),
                programId = programId,
                data = byteArrayOf(2)
            )
        )

        val message2 = transaction.compileMessage()

        assertNotSame("Should return new instance after adding instruction", message1, message2)
        assertEquals("First message should have 1 instruction", 1, message1.instructions.size)
        assertEquals("Second message should have 2 instructions", 2, message2.instructions.size)
    }

    @Test
    fun `test compileMessage requires blockhash`() {
        val keys = TestHelpers.createTestPublicKeys(2)
        val sender = keys[0]
        val programId = keys[1]

        val transaction = Transaction(
            feePayer = sender
            // No blockhash!
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(sender, isSigner = true, isWritable = true)),
                programId = programId,
                data = byteArrayOf()
            )
        )

        try {
            transaction.compileMessage()
            fail("Should throw exception when blockhash is missing")
        } catch (e: IllegalStateException) {
            assertTrue("Should have correct error message",
                e.message?.contains("recentBlockhash") == true)
        }
    }

    @Test
    fun `test compileMessage requires fee payer`() {
        val keys = TestHelpers.createTestPublicKeys(2)
        val account = keys[0]
        val programId = keys[1]

        val transaction = Transaction(
            // No fee payer!
            recentBlockhash = "test-blockhash"
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(account, isSigner = false, isWritable = true)),
                programId = programId,
                data = byteArrayOf()
            )
        )

        try {
            transaction.compileMessage()
            fail("Should throw exception when fee payer is missing")
        } catch (e: IllegalStateException) {
            assertTrue("Should have correct error message",
                e.message?.contains("feePayer") == true)
        }
    }
}
