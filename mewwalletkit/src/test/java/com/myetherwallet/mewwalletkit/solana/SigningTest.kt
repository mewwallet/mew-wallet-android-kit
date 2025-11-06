package com.myetherwallet.mewwalletkit.solana

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for Solana transaction signing and verification (Phase 5)
 */
class SigningTest {

    @Test
    fun `test sign transaction with single signer`() {
        val keys = TestHelpers.createTestKeys(1)
        val privateKey = keys[0]
        val publicKey = privateKey.publicKey()!!

        val transaction = Transaction(
            feePayer = publicKey,
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(publicKey, isSigner = true, isWritable = true)),
                programId = SystemProgram.programId(),
                data = byteArrayOf(0x02, 0x00, 0x00, 0x00)
            )
        )

        // Sign the transaction
        transaction.sign(listOf(privateKey))

        // Verify signature was added
        val signatures = transaction.getSignatures()
        assertEquals("Should have 1 signature", 1, signatures.size)
        assertNotNull("Signature should not be null", signatures[0].signature)
        assertEquals("Signature should be 64 bytes", 64, signatures[0].signature?.size)
        assertEquals("Signature public key should match", publicKey, signatures[0].publicKey)
    }

    @Test
    fun `test sign transaction with multiple signers`() {
        val keys = TestHelpers.createTestKeys(3)
        val privateKeys = keys
        val publicKeys = keys.map { it.publicKey()!! }

        val transaction = Transaction(
            feePayer = publicKeys[0],
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(
                    AccountMeta(publicKeys[0], isSigner = true, isWritable = true),
                    AccountMeta(publicKeys[1], isSigner = true, isWritable = false),
                    AccountMeta(publicKeys[2], isSigner = true, isWritable = false)
                ),
                programId = SystemProgram.programId(),
                data = byteArrayOf()
            )
        )

        // Sign with all signers
        transaction.sign(privateKeys)

        // Verify all signatures were added
        val signatures = transaction.getSignatures()
        assertEquals("Should have 3 signatures", 3, signatures.size)
        signatures.forEach { sig ->
            assertNotNull("Each signature should not be null", sig.signature)
            assertEquals("Each signature should be 64 bytes", 64, sig.signature?.size)
        }
    }

    @Test
    fun `test sign deduplicates signers by public key`() {
        val keys = TestHelpers.createTestKeys(2)
        val privateKey = keys[0]
        val publicKey = privateKey.publicKey()!!

        val transaction = Transaction(
            feePayer = publicKey,
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(publicKey, isSigner = true, isWritable = true)),
                programId = SystemProgram.programId(),
                data = byteArrayOf()
            )
        )

        // Sign with duplicate signer (same key twice)
        transaction.sign(listOf(privateKey, privateKey, privateKey))

        // Should only have 1 signature (deduplicated)
        val signatures = transaction.getSignatures()
        assertEquals("Should deduplicate and have only 1 signature", 1, signatures.size)
        assertNotNull("Signature should not be null", signatures[0].signature)
    }

    @Test
    fun `test partial signing workflow`() {
        val keys = TestHelpers.createTestKeys(3)
        val privateKeys = keys
        val publicKeys = keys.map { it.publicKey()!! }

        val transaction = Transaction(
            feePayer = publicKeys[0],
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(
                    AccountMeta(publicKeys[0], isSigner = true, isWritable = true),
                    AccountMeta(publicKeys[1], isSigner = true, isWritable = false),
                    AccountMeta(publicKeys[2], isSigner = true, isWritable = false)
                ),
                programId = SystemProgram.programId(),
                data = byteArrayOf()
            )
        )

        // First signer signs
        transaction.sign(listOf(privateKeys[0]))
        val signaturesAfterFirst = transaction.getSignatures()
        assertEquals("Should have 3 signature slots after first signer", 3, signaturesAfterFirst.size)

        // Only first signature should be non-null
        val signedCount = signaturesAfterFirst.count { it.signature != null }
        assertEquals("Should have 1 signed signature after first signer", 1, signedCount)
        assertNotNull("First signature should be present", signaturesAfterFirst[0].signature)

        // Second and third signers sign using partialSign
        transaction.partialSign(listOf(privateKeys[1], privateKeys[2]))
        val signaturesAfterAll = transaction.getSignatures()
        assertEquals("Should still have 3 signature slots after partial signing", 3, signaturesAfterAll.size)

        // All signatures should be present now
        signaturesAfterAll.forEach { sig ->
            assertNotNull("All signatures should be present", sig.signature)
        }
    }

    @Test
    fun `test partialSign preserves existing signatures`() {
        val keys = TestHelpers.createTestKeys(2)
        val privateKeys = keys
        val publicKeys = keys.map { it.publicKey()!! }

        val transaction = Transaction(
            feePayer = publicKeys[0],
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(
                    AccountMeta(publicKeys[0], isSigner = true, isWritable = true),
                    AccountMeta(publicKeys[1], isSigner = true, isWritable = false)
                ),
                programId = SystemProgram.programId(),
                data = byteArrayOf()
            )
        )

        // Sign with first signer
        transaction.sign(listOf(privateKeys[0]))
        val firstSignature = transaction.getSignatures()[0].signature
        assertNotNull("First signature should be present", firstSignature)

        // Partially sign with second signer
        transaction.partialSign(listOf(privateKeys[1]))

        // Verify first signature is unchanged
        val signatures = transaction.getSignatures()
        assertEquals("Should have 2 signatures", 2, signatures.size)
        assertArrayEquals("First signature should be preserved", firstSignature, signatures[0].signature)
        assertNotNull("Second signature should be added", signatures[1].signature)
    }

    @Test
    fun `test addSignature adds external signature`() {
        val keys = TestHelpers.createTestKeys(1)
        val privateKey = keys[0]
        val publicKey = privateKey.publicKey()!!

        val transaction = Transaction(
            feePayer = publicKey,
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(publicKey, isSigner = true, isWritable = true)),
                programId = SystemProgram.programId(),
                data = byteArrayOf()
            )
        )

        // Compile to populate signature slots
        transaction.compileMessage()

        // Create external signature (64 bytes)
        val externalSignature = ByteArray(64) { it.toByte() }

        // Add external signature
        transaction.addSignature(publicKey, externalSignature)

        // Verify signature was added
        val signatures = transaction.getSignatures()
        assertEquals("Should have 1 signature", 1, signatures.size)
        assertArrayEquals("Should have external signature", externalSignature, signatures[0].signature)
    }

    @Test
    fun `test addSignature fails with wrong signature size`() {
        val keys = TestHelpers.createTestKeys(1)
        val publicKey = keys[0].publicKey()!!

        val transaction = Transaction(
            feePayer = publicKey,
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(publicKey, isSigner = true, isWritable = true)),
                programId = SystemProgram.programId(),
                data = byteArrayOf()
            )
        )

        transaction.compileMessage()

        // Try to add signature with wrong size
        val badSignature = ByteArray(32)  // Should be 64 bytes

        try {
            transaction.addSignature(publicKey, badSignature)
            fail("Should throw exception for wrong signature size")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should mention signature size", e.message?.contains("64 bytes") == true)
        }
    }

    @Test
    fun `test addSignature fails for unknown public key`() {
        val keys = TestHelpers.createTestKeys(2)
        val publicKey1 = keys[0].publicKey()!!
        val publicKey2 = keys[1].publicKey()!!  // Not in transaction

        val transaction = Transaction(
            feePayer = publicKey1,
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(publicKey1, isSigner = true, isWritable = true)),
                programId = SystemProgram.programId(),
                data = byteArrayOf()
            )
        )

        transaction.compileMessage()

        val signature = ByteArray(64)

        try {
            transaction.addSignature(publicKey2, signature)
            fail("Should throw exception for unknown public key")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should mention public key not found", e.message?.contains("not found") == true)
        }
    }

    @Test
    fun `test verifySignatures with valid signatures`() {
        val keys = TestHelpers.createTestKeys(2)
        val privateKeys = keys
        val publicKeys = keys.map { it.publicKey()!! }

        val transaction = Transaction(
            feePayer = publicKeys[0],
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(
                    AccountMeta(publicKeys[0], isSigner = true, isWritable = true),
                    AccountMeta(publicKeys[1], isSigner = true, isWritable = false)
                ),
                programId = SystemProgram.programId(),
                data = byteArrayOf()
            )
        )

        // Sign the transaction
        transaction.sign(privateKeys)

        // Verify signatures
        assertTrue("Signatures should be valid", transaction.verifySignatures())
    }

    @Test
    fun `test verifySignatures with invalid signature`() {
        val keys = TestHelpers.createTestKeys(1)
        val publicKey = keys[0].publicKey()!!

        val transaction = Transaction(
            feePayer = publicKey,
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(publicKey, isSigner = true, isWritable = true)),
                programId = SystemProgram.programId(),
                data = byteArrayOf()
            )
        )

        transaction.compileMessage()

        // Add invalid signature (all zeros)
        val invalidSignature = ByteArray(64)
        transaction.addSignature(publicKey, invalidSignature)

        // Verify should fail
        assertFalse("Invalid signature should not verify", transaction.verifySignatures())
    }

    @Test
    fun `test verifySignatures with missing signatures when required`() {
        val keys = TestHelpers.createTestKeys(1)
        val publicKey = keys[0].publicKey()!!

        val transaction = Transaction(
            feePayer = publicKey,
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(publicKey, isSigner = true, isWritable = true)),
                programId = SystemProgram.programId(),
                data = byteArrayOf()
            )
        )

        // Compile but don't sign
        transaction.compileMessage()

        // Verify should fail when requiring all signatures
        assertFalse("Should fail with missing signatures", transaction.verifySignatures(requireAllSignatures = true))
    }

    @Test
    fun `test verifySignatures with missing signatures when not required`() {
        val keys = TestHelpers.createTestKeys(1)
        val publicKey = keys[0].publicKey()!!

        val transaction = Transaction(
            feePayer = publicKey,
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(publicKey, isSigner = true, isWritable = true)),
                programId = SystemProgram.programId(),
                data = byteArrayOf()
            )
        )

        // Compile but don't sign
        transaction.compileMessage()

        // Verify should pass when not requiring all signatures (no signatures to verify)
        assertTrue("Should pass with missing signatures when not required",
            transaction.verifySignatures(requireAllSignatures = false))
    }

    @Test
    fun `test serialize with verification enabled`() {
        val keys = TestHelpers.createTestKeys(1)
        val privateKey = keys[0]
        val publicKey = privateKey.publicKey()!!

        val transaction = Transaction(
            feePayer = publicKey,
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(publicKey, isSigner = true, isWritable = true)),
                programId = SystemProgram.programId(),
                data = byteArrayOf()
            )
        )

        // Sign the transaction
        transaction.sign(listOf(privateKey))

        // Serialize with verification enabled (should succeed)
        val serialized = transaction.serialize(requireAllSignatures = true, verifySignatures = true)

        assertNotNull("Serialization should succeed with valid signatures", serialized)
        assertTrue("Serialized transaction should have content", serialized.isNotEmpty())
    }

    @Test
    fun `test serialize fails with invalid signature when verification enabled`() {
        val keys = TestHelpers.createTestKeys(1)
        val publicKey = keys[0].publicKey()!!

        val transaction = Transaction(
            feePayer = publicKey,
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(publicKey, isSigner = true, isWritable = true)),
                programId = SystemProgram.programId(),
                data = byteArrayOf()
            )
        )

        transaction.compileMessage()

        // Add invalid signature
        val invalidSignature = ByteArray(64)
        transaction.addSignature(publicKey, invalidSignature)

        try {
            transaction.serialize(requireAllSignatures = true, verifySignatures = true)
            fail("Serialize should fail with invalid signature when verification enabled")
        } catch (e: ValidationException) {
            assertEquals("Should have 1 error", 1, e.errors.size)
            assertTrue("Error should be InvalidSignature",
                e.errors[0] is ValidationError.InvalidSignature)
        }
    }

    @Test
    fun `test full integration flow - create compile sign serialize verify`() {
        val keys = TestHelpers.createTestKeys(2)
        val privateKeys = keys
        val publicKeys = keys.map { it.publicKey()!! }

        // 1. Create transaction
        val transaction = Transaction(
            feePayer = publicKeys[0],
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        // 2. Add transfer instruction
        transaction.add(SystemProgram.transfer(publicKeys[0], publicKeys[1], 1000000uL))

        // 3. Sign transaction
        transaction.sign(listOf(privateKeys[0]))

        // 4. Verify signatures
        assertTrue("Signatures should be valid", transaction.verifySignatures())

        // 5. Serialize for broadcast
        val serialized = transaction.serialize()

        assertNotNull("Serialized transaction should not be null", serialized)
        assertTrue("Serialized transaction should have content", serialized.isNotEmpty())

        // Serialized format: [signatures compact array] + [message]
        // With 1 signature: [0x01] + [64 bytes] + [message bytes]
        assertTrue("Serialized should be larger than just signature", serialized.size > 65)
    }

    @Test
    fun `test sign requires at least one signer`() {
        val keys = TestHelpers.createTestKeys(1)
        val publicKey = keys[0].publicKey()!!

        val transaction = Transaction(
            feePayer = publicKey,
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(publicKey, isSigner = true, isWritable = true)),
                programId = SystemProgram.programId(),
                data = byteArrayOf()
            )
        )

        try {
            transaction.sign(emptyList())
            fail("Should require at least one signer")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should mention signer required", e.message?.contains("signer") == true)
        }
    }

    @Test
    fun `test partialSign requires at least one signer`() {
        val keys = TestHelpers.createTestKeys(1)
        val publicKey = keys[0].publicKey()!!

        val transaction = Transaction(
            feePayer = publicKey,
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(publicKey, isSigner = true, isWritable = true)),
                programId = SystemProgram.programId(),
                data = byteArrayOf()
            )
        )

        try {
            transaction.partialSign(emptyList())
            fail("Should require at least one signer")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should mention signer required", e.message?.contains("signer") == true)
        }
    }
}
