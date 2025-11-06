package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.Network
import com.myetherwallet.mewwalletkit.bip.bip44.PrivateKey
import com.myetherwallet.mewwalletkit.core.extension.hexToByteArray
import com.myetherwallet.mewwalletkit.core.extension.toHexString
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for Solana transaction signing functionality.
 *
 * Based on iOS tests from:
 * https://github.com/mewwallet/mew-wallet-ios-kit/blob/19e6209db869b934538d24f0af952e832e245993/Tests/mew-wallet-ios-kit-solana-sign/Solana.Transaction.Sign%2BTests.swift
 */
class TransactionSignTest {

    @Test
    fun `test partialSign`() {
        // Create two test accounts
        val account1 = PrivateKey.createWithPrivateKey(
            "59a1aceb689ed3fe7adc1b44d78be38d0f1f0ec99263ce6199fbb369722a773c".hexToByteArray(),
            Network.SOLANA
        )
        val account2 = PrivateKey.createWithPrivateKey(
            "d4db5154833691176b4172b6dce524d40a035698738400ca6d131e8f6decc764".hexToByteArray(),
            Network.SOLANA
        )
        val recentBlockhash = "9U2wM3MUToUCRiBsR6zAcnqJw43kcXSxp27QxsKEJSBf"

        // Create transfer instruction that requires both accounts as signers
        // Use a transfer from account1 to a third party, with account2 as additional signer
        val transfer = TransactionInstruction(
            keys = listOf(
                AccountMeta(account1.publicKey()!!, isSigner = true, isWritable = true),
                AccountMeta(account2.publicKey()!!, isSigner = true, isWritable = false)
            ),
            programId = SystemProgram.programId(),
            data = byteArrayOf(0x02, 0x00, 0x00, 0x00, 0x7b, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        )

        // Create and fully sign a transaction
        val transaction = Transaction(
            feePayer = account1.publicKey(),
            recentBlockhash = recentBlockhash
        )
        transaction.add(transfer)
        transaction.sign(account1, account2)
        val serialized = transaction.serialize()

        // Create a partial transaction using setSigners
        val partialTransaction = Transaction(
            feePayer = account1.publicKey(),
            recentBlockhash = recentBlockhash
        )
        partialTransaction.add(transfer)
        partialTransaction.setSigners(account1.publicKey()!!, account2.publicKey()!!)

        // Verify signatures are initially null
        val signatures = partialTransaction.getSignatures()
        assertEquals("Should have 2 signature slots", 2, signatures.size)
        assertNull("First signature should be null", signatures[0].signature)
        assertNull("Second signature should be null", signatures[1].signature)

        // Partially sign with first account
        partialTransaction.partialSign(account1)
        val signaturesAfterFirst = partialTransaction.getSignatures()
        assertNotNull("First signature should be present", signaturesAfterFirst[0].signature)
        assertNull("Second signature should still be null", signaturesAfterFirst[1].signature)

        // Should throw ValidationException when trying to serialize with missing signatures
        try {
            partialTransaction.serialize()
            fail("Should throw ValidationException for missing signatures")
        } catch (e: ValidationException) {
            assertEquals("Should have 1 error", 1, e.errors.size)
            assertTrue("Error should be MissingSignature",
                e.errors[0] is ValidationError.MissingSignature)
            assertEquals("Error should be for account2",
                account2.publicKey(), (e.errors[0] as ValidationError.MissingSignature).publicKey)
        }

        // Should succeed when requireAllSignatures is false
        val partialSerialized = partialTransaction.serialize(requireAllSignatures = false)
        assertNotNull("Should serialize with requireAllSignatures=false", partialSerialized)
        assertTrue("Partial serialization should have data", partialSerialized.isNotEmpty())

        // Partially sign with second account
        partialTransaction.partialSign(account2)
        val signaturesAfterSecond = partialTransaction.getSignatures()
        assertNotNull("First signature should still be present", signaturesAfterSecond[0].signature)
        assertNotNull("Second signature should now be present", signaturesAfterSecond[1].signature)

        // Should now serialize successfully
        val fullySerialized = partialTransaction.serialize()
        assertNotNull("Should serialize with all signatures", fullySerialized)

        // The two transactions should be equal
        assertArrayEquals("Partial and full transactions should serialize identically",
            serialized, fullySerialized)

        // Corrupt one signature
        val corruptedTransaction = Transaction(
            feePayer = account1.publicKey(),
            recentBlockhash = recentBlockhash
        )
        corruptedTransaction.add(transfer)
        corruptedTransaction.setSigners(account1.publicKey()!!, account2.publicKey()!!)
        corruptedTransaction.partialSign(account1, account2)

        // Replace first signature with invalid data
        corruptedTransaction.addSignature(account1.publicKey()!!, ByteArray(64) { 0x01 })

        // Should throw ValidationException for invalid signature
        try {
            corruptedTransaction.serialize(requireAllSignatures = false, verifySignatures = true)
            fail("Should throw ValidationException for invalid signature")
        } catch (e: ValidationException) {
            assertEquals("Should have 1 error", 1, e.errors.size)
            assertTrue("Error should be InvalidSignature",
                e.errors[0] is ValidationError.InvalidSignature)
        }

        // Should succeed when verifySignatures is false
        val corruptedSerialized = corruptedTransaction.serialize(
            requireAllSignatures = false,
            verifySignatures = false
        )
        assertNotNull("Should serialize with verifySignatures=false", corruptedSerialized)
    }

    @Test
    fun `test throws for invalid signatures`() {
        // Create sender account
        val sender = PrivateKey.createWithPrivateKey(
            ByteArray(32) { 0x08 },
            Network.SOLANA
        )
        val senderPublicKey = sender.publicKey()!!

        // Verify sender public key
        val expectedSenderPubkey = "1398f62c6d1a457c51ba6a4b5f3dbd2f69fca93216218dc8997e416bd17d93ca"
        assertEquals("Sender public key should match",
            expectedSenderPubkey, senderPublicKey.data().toHexString())

        val recentBlockhash = "EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k"
        val recipient = TestHelpers.parseAddress("J3dxNj7nDRRqRRXuEMynDG57DkZK4jYRuv3Garmb1i99")

        // Create transfer instruction
        val transfer = SystemProgram.transfer(
            fromPubkey = senderPublicKey,
            toPubkey = recipient,
            lamports = 49uL
        )

        // Create transaction with feePayer set
        val sampleTransaction = Transaction(
            feePayer = senderPublicKey,
            recentBlockhash = recentBlockhash
        )
        sampleTransaction.add(transfer)

        // Should throw ValidationException for missing signature
        try {
            sampleTransaction.serialize()
            fail("Should throw ValidationException for missing signature")
        } catch (e: ValidationException) {
            assertEquals("Should have 1 error", 1, e.errors.size)
            assertTrue("Error should be MissingSignature",
                e.errors[0] is ValidationError.MissingSignature)
            assertEquals("Error should be for sender",
                senderPublicKey, (e.errors[0] as ValidationError.MissingSignature).publicKey)
        }

        // Should succeed when verifySignatures is false (still need requireAllSignatures = false)
        val unverifiedSerialized = sampleTransaction.serialize(
            requireAllSignatures = false,
            verifySignatures = false
        )
        assertNotNull("Should serialize with verifySignatures=false", unverifiedSerialized)

        // serializeMessage should work without signatures
        val messageSerialized = sampleTransaction.serializeMessage()
        assertNotNull("serializeMessage should work", messageSerialized)
        assertTrue("Message should have data", messageSerialized.isNotEmpty())

        // Set signers and add invalid signature (all zeros)
        sampleTransaction.feePayer = null // Reset to test setSigners
        sampleTransaction.setSigners(senderPublicKey)
        assertEquals("Should have 1 signature slot", 1, sampleTransaction.getSignatures().size)

        // Add invalid signature (all zeros)
        sampleTransaction.addSignature(senderPublicKey, ByteArray(64) { 0x00 })

        // Should throw ValidationException for invalid signature
        try {
            sampleTransaction.serialize()
            fail("Should throw ValidationException for invalid signature")
        } catch (e: ValidationException) {
            assertEquals("Should have 1 error", 1, e.errors.size)
            assertTrue("Error should be InvalidSignature",
                e.errors[0] is ValidationError.InvalidSignature)
            assertEquals("Error should be for sender",
                senderPublicKey, (e.errors[0] as ValidationError.InvalidSignature).publicKey)
        }

        // Test multiple errors at once
        val tempKey = PrivateKey.createWithPrivateKey(
            "2e070ce3a9f6b2626aaf797beefef58948bf172ea91557d0cf5164f2d5cba59a".hexToByteArray(),
            Network.SOLANA
        )
        val tempPublicKey = tempKey.publicKey()!!

        // Create a new transaction with tempKey as feePayer and both keys as signers
        val multiErrorTransaction = Transaction(
            feePayer = tempPublicKey,
            recentBlockhash = recentBlockhash
        )
        // Add instruction that requires both signers
        val multiSigInstruction = TransactionInstruction(
            keys = listOf(
                AccountMeta(tempPublicKey, isSigner = true, isWritable = true),
                AccountMeta(senderPublicKey, isSigner = true, isWritable = false),
                AccountMeta(recipient, isSigner = false, isWritable = true)
            ),
            programId = SystemProgram.programId(),
            data = byteArrayOf(0x02, 0x00, 0x00, 0x00, 0x31, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        )
        multiErrorTransaction.add(multiSigInstruction)
        multiErrorTransaction.setSigners(tempPublicKey, senderPublicKey)

        // Add invalid signature for sender (0x2A repeated)
        multiErrorTransaction.addSignature(senderPublicKey, ByteArray(64) { 0x2A })
        // tempKey signature remains null

        // Should throw ValidationException with multiple errors
        try {
            multiErrorTransaction.serialize()
            fail("Should throw ValidationException for multiple errors")
        } catch (e: ValidationException) {
            assertEquals("Should have 2 errors", 2, e.errors.size)

            // Verify we have both MissingSignature and InvalidSignature errors
            val missingError = e.errors.find { it is ValidationError.MissingSignature }
            val invalidError = e.errors.find { it is ValidationError.InvalidSignature }

            assertNotNull("Should have MissingSignature error", missingError)
            assertNotNull("Should have InvalidSignature error", invalidError)

            assertEquals("MissingSignature should be for tempKey",
                tempPublicKey, (missingError as ValidationError.MissingSignature).publicKey)
            assertEquals("InvalidSignature should be for sender",
                senderPublicKey, (invalidError as ValidationError.InvalidSignature).publicKey)
        }
    }
}
