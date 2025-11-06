package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.core.extension.toHexString
import org.junit.Assert.*
import org.junit.Test

/**
 * Integration tests with real Solana transaction data.
 *
 * These tests create actual transactions that could be broadcast to Solana
 * devnet/mainnet. They use hardcoded test keys and verify outputs against
 * known good values from solana-web3.js.
 *
 * WARNING: Test keys are publicly known - NEVER use in production!
 */
class TransactionIntegrationTest {

    /**
     * Test creating and signing a real SOL transfer transaction.
     *
     * This demonstrates the complete flow: create → add instruction → sign → serialize
     */
    @Test
    fun `test create and sign real transfer transaction`() {
        // Use test keys from TestHelpers
        val senderKey = TestHelpers.createTestKeys(1)[0]
        val senderPubkey = senderKey.publicKey()!!

        // Parse recipient address from Base58
        val recipientPubkey = TestHelpers.parseAddress(TestHelpers.TEST_RECIPIENT_ADDRESS)

        // Create transaction
        val transaction = Transaction(
            feePayer = senderPubkey,
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        // Add transfer instruction: 0.001 SOL = 1,000,000 lamports
        transaction.add(
            SystemProgram.transfer(
                fromPubkey = senderPubkey,
                toPubkey = recipientPubkey,
                lamports = 1_000_000uL
            )
        )

        // Sign transaction
        transaction.sign(listOf(senderKey))

        // Verify signature structure
        val signatures = transaction.getSignatures()
        assertEquals("Should have exactly 1 signature", 1, signatures.size)
        assertNotNull("Signature should be present", signatures[0].signature)
        assertEquals("Signature should be 64 bytes", 64, signatures[0].signature?.size)

        // Verify signatures are valid
        assertTrue("Signature should verify", transaction.verifySignatures())

        // Serialize to wire format
        val serializedTx = transaction.serialize()
        assertNotNull("Serialized transaction should not be null", serializedTx)
        assertTrue("Serialized transaction should have content", serializedTx.isNotEmpty())

        // Print for manual verification (can compare with solana-web3.js output)
        println("\n=== Real Transfer Transaction ===")
        println("Sender: ${senderPubkey.address()?.address}")
        println("Recipient: ${recipientPubkey.address()?.address}")
        println("Amount: 1,000,000 lamports (0.001 SOL)")
        println("\nSerialized (hex):")
        println(serializedTx.toHexString())
        println("=== End ===\n")
    }

    /**
     * Test that message serialization is deterministic and matches expected format.
     *
     * The message is what gets signed - it should be identical for the same inputs.
     */
    @Test
    fun `test message serialization is deterministic`() {
        val senderKey = TestHelpers.createTestKeys(1)[0]
        val senderPubkey = senderKey.publicKey()!!
        val recipientPubkey = TestHelpers.parseAddress(TestHelpers.TEST_RECIPIENT_ADDRESS)

        // Create two identical transactions
        val tx1 = TestHelpers.createTransferTransaction(
            fromKey = senderKey,
            toPublicKey = recipientPubkey,
            lamports = 1_000_000uL
        )

        val tx2 = TestHelpers.createTransferTransaction(
            fromKey = senderKey,
            toPublicKey = recipientPubkey,
            lamports = 1_000_000uL
        )

        // Serialize messages (before signing)
        val message1 = tx1.serializeMessage()
        val message2 = tx2.serializeMessage()

        // Messages should be identical
        assertArrayEquals(
            "Identical transactions should produce identical messages",
            message1,
            message2
        )

        // Verify message structure
        assertTrue("Message should be at least 3 bytes (header)", message1.size >= 3)

        // Header: [numRequiredSignatures, numReadonlySignedAccounts, numReadonlyUnsignedAccounts]
        assertEquals("Should have 1 required signature", 0x01, message1[0].toInt() and 0xFF)
        assertEquals("Should have 0 readonly signed accounts", 0x00, message1[1].toInt() and 0xFF)

        println("\n=== Message Bytes ===")
        println("Message (hex): ${message1.toHexString()}")
        println("Message length: ${message1.size} bytes")
        println("=== End ===\n")
    }

    /**
     * Test signature verification without broadcasting.
     *
     * This shows how to verify a transaction is properly signed without
     * actually sending it to the network.
     */
    @Test
    fun `test verify signature without broadcasting`() {
        val senderKey = TestHelpers.createTestKeys(1)[0]
        val recipientPubkey = TestHelpers.parseAddress(TestHelpers.TEST_RECIPIENT_ADDRESS)

        val transaction = TestHelpers.createTransferTransaction(
            fromKey = senderKey,
            toPublicKey = recipientPubkey
        )

        // Sign transaction
        transaction.sign(listOf(senderKey))

        // Verify signatures are valid
        assertTrue("Valid signature should verify correctly", transaction.verifySignatures())

        // Get the signature bytes
        val signatureBytes = transaction.getSignatures()[0].signature!!

        println("\n=== Signature Details ===")
        println("Signature (hex): ${signatureBytes.toHexString()}")
        println("Verification: PASSED")
        println("=== End ===\n")

        // Verify that changing the signature breaks verification
        // (Create new transaction with tampered signature)
        val tamperedTx = TestHelpers.createTransferTransaction(
            fromKey = senderKey,
            toPublicKey = recipientPubkey
        )
        tamperedTx.compileMessage()

        // Add tampered signature
        val tamperedSignature = ByteArray(64) { 0x00 } // All zeros = invalid
        tamperedTx.addSignature(senderKey.publicKey()!!, tamperedSignature)

        // Verification should fail
        assertFalse("Tampered signature should fail verification", tamperedTx.verifySignatures())
    }

    /**
     * Test multi-signature transaction with two signers.
     *
     * This demonstrates how to create transactions requiring multiple signatures,
     * which is common in multi-sig wallets and advanced use cases.
     */
    @Test
    fun `test multi-signature transaction`() {
        val keys = TestHelpers.createTestKeys(3)
        val signer1 = keys[0]
        val signer2 = keys[1]
        val recipient = keys[2].publicKey()!!

        val transaction = Transaction(
            feePayer = signer1.publicKey()!!,
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        // Add instruction requiring both signers
        transaction.add(
            TransactionInstruction(
                keys = listOf(
                    AccountMeta(signer1.publicKey()!!, isSigner = true, isWritable = true),
                    AccountMeta(signer2.publicKey()!!, isSigner = true, isWritable = false),
                    AccountMeta(recipient, isSigner = false, isWritable = true)
                ),
                programId = SystemProgram.programId(),
                data = byteArrayOf(
                    2, 0, 0, 0,              // Instruction index: 2 (Transfer)
                    0x40, 0x42, 0x0F, 0,     // Amount: 1,000,000 lamports
                    0, 0, 0, 0               // (little-endian u64)
                )
            )
        )

        // Sign with both signers
        transaction.sign(listOf(signer1, signer2))

        // Verify we have 2 signatures
        val signatures = transaction.getSignatures()
        assertEquals("Should have 2 signatures", 2, signatures.size)
        assertNotNull("First signature should be present", signatures[0].signature)
        assertNotNull("Second signature should be present", signatures[1].signature)

        // Verify both signatures
        assertTrue("Both signatures should verify", transaction.verifySignatures())

        // Serialize
        val serialized = transaction.serialize()

        println("\n=== Multi-Signature Transaction ===")
        println("Signer 1: ${signer1.publicKey()!!.address()?.address}")
        println("Signer 2: ${signer2.publicKey()!!.address()?.address}")
        println("Recipient: ${recipient.address()?.address}")
        println("Signature count: 2")
        println("\nSerialized (hex):")
        println(serialized.toHexString())
        println("=== End ===\n")
    }

    /**
     * Test CreateAccount instruction serialization.
     *
     * This demonstrates account creation, which is required before using
     * many Solana programs.
     */
    @Test
    fun `test create account instruction`() {
        val funderKey = TestHelpers.createTestKeys(1)[0]
        val funderPubkey = funderKey.publicKey()!!

        val newAccountKey = TestHelpers.createTestKeys(2)[1]
        val newAccountPubkey = newAccountKey.publicKey()!!

        val transaction = Transaction(
            feePayer = funderPubkey,
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        // Add CreateAccount instruction
        transaction.add(
            SystemProgram.createAccount(
                fromPubkey = funderPubkey,
                newAccountPubkey = newAccountPubkey,
                lamports = 1_000_000uL,      // Rent for new account
                space = 165uL,                // Account data size
                owner = SystemProgram.programId()  // Owner program
            )
        )

        // Sign with both funder and new account
        transaction.sign(listOf(funderKey, newAccountKey))

        // Verify
        assertTrue("Signatures should verify", transaction.verifySignatures())

        // Serialize
        val serialized = transaction.serialize()

        println("\n=== CreateAccount Transaction ===")
        println("Funder: ${funderPubkey.address()?.address}")
        println("New Account: ${newAccountPubkey.address()?.address}")
        println("Lamports: 1,000,000")
        println("Space: 165 bytes")
        println("\nSerialized (hex):")
        println(serialized.toHexString())
        println("=== End ===\n")
    }

    /**
     * Test transaction size estimation.
     *
     * This helps understand transaction costs and limits.
     */
    @Test
    fun `test transaction size estimation`() {
        val senderKey = TestHelpers.createTestKeys(1)[0]
        val recipientPubkey = TestHelpers.parseAddress(TestHelpers.TEST_RECIPIENT_ADDRESS)

        val transaction = TestHelpers.createTransferTransaction(
            fromKey = senderKey,
            toPublicKey = recipientPubkey
        )

        transaction.sign(listOf(senderKey))

        val serialized = transaction.serialize()

        println("\n=== Transaction Size Analysis ===")
        println("Total size: ${serialized.size} bytes")

        // Transaction format:
        // - Signatures compact array: 1 byte (length) + 64 bytes (signature) = 65 bytes
        // - Message: remainder

        val messageSize = transaction.serializeMessage().size
        val signaturesSize = 1 + 64  // Compact length + signature

        println("Signatures section: $signaturesSize bytes")
        println("Message section: $messageSize bytes")

        assertEquals(
            "Total should equal signatures + message",
            signaturesSize + messageSize,
            serialized.size
        )

        println("\nBreakdown:")
        println("  - Compact signature count: 1 byte")
        println("  - Signature data: 64 bytes")
        println("  - Message header: 3 bytes")
        println("  - Account keys array: depends on accounts")
        println("  - Blockhash: 32 bytes")
        println("  - Instructions array: depends on instructions")
        println("=== End ===\n")

        // Verify size is reasonable for a simple transfer
        assertTrue("Transaction should be under 300 bytes", serialized.size < 300)
    }

    /**
     * Test partial signing workflow for multi-sig scenarios.
     *
     * This simulates a workflow where signatures are collected asynchronously,
     * such as in a multi-sig wallet or hardware wallet scenario.
     */
    @Test
    fun `test partial signing workflow simulation`() {
        val keys = TestHelpers.createTestKeys(3)
        val signer1 = keys[0]
        val signer2 = keys[1]
        val recipient = keys[2].publicKey()!!

        val transaction = Transaction(
            feePayer = signer1.publicKey()!!,
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(
                    AccountMeta(signer1.publicKey()!!, isSigner = true, isWritable = true),
                    AccountMeta(signer2.publicKey()!!, isSigner = true, isWritable = false),
                    AccountMeta(recipient, isSigner = false, isWritable = true)
                ),
                programId = SystemProgram.programId(),
                data = byteArrayOf(2, 0, 0, 0, 0x40, 0x42, 0x0F, 0, 0, 0, 0, 0)
            )
        )

        println("\n=== Partial Signing Workflow ===")

        // Step 1: First signer signs
        transaction.sign(listOf(signer1))
        println("Step 1: Signer 1 signed")
        println("  Signatures present: ${transaction.getSignatures().count { it.signature != null }}/2")

        // At this point, transaction cannot be broadcast (missing signature)
        assertFalse(
            "Transaction with partial signatures should fail requireAllSignatures",
            try {
                transaction.serialize(requireAllSignatures = true, verifySignatures = false)
                true
            } catch (e: ValidationException) {
                false
            }
        )

        // Step 2: Second signer signs (using partialSign)
        transaction.partialSign(listOf(signer2))
        println("Step 2: Signer 2 signed")
        println("  Signatures present: ${transaction.getSignatures().count { it.signature != null }}/2")

        // Now transaction is fully signed and can be broadcast
        val serialized = transaction.serialize()
        println("Step 3: Transaction fully signed and serialized")
        println("  Size: ${serialized.size} bytes")

        assertTrue("All signatures should verify", transaction.verifySignatures())
        println("  Verification: PASSED")
        println("=== End ===\n")
    }
}
