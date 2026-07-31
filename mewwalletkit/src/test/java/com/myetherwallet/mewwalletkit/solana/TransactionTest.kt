package com.myetherwallet.mewwalletkit.solana

import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

/**
 * Tests for Solana Transaction data structures (Phase 1)
 */
class TransactionTest {

    @Test
    fun `test create empty transaction`() {
        val transaction = Transaction()

        assertEquals("New transaction should have no instructions", 0, transaction.getInstructions().size)
        assertEquals("New transaction should have no signatures", 0, transaction.getSignatures().size)
        assertNull("New transaction should have no fee payer set", transaction.feePayer)
        assertNull("New transaction should have no blockhash set", transaction.recentBlockhash)
    }

    @Test
    fun `test create transaction with fee payer and blockhash`() {
        val feePayer = TestHelpers.createTestPublicKeys(1)[0]
        val blockhash = "4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ"

        val transaction = Transaction(
            feePayer = feePayer,
            recentBlockhash = blockhash
        )

        assertEquals("Fee payer should be set", feePayer, transaction.feePayer)
        assertEquals("Blockhash should be set", blockhash, transaction.recentBlockhash)
    }

    @Test
    fun `test add single instruction`() {
        val transaction = Transaction()
        val programId = TestHelpers.createTestPublicKeys(1)[0]
        val account1 = TestHelpers.createTestPublicKeys(1)[0]

        val instruction = TransactionInstruction(
            keys = listOf(AccountMeta(account1, isSigner = true, isWritable = true)),
            programId = programId,
            data = byteArrayOf(1, 2, 3)
        )

        transaction.add(instruction)

        assertEquals("Should have 1 instruction", 1, transaction.getInstructions().size)
        assertEquals("Instruction should match", instruction, transaction.getInstructions()[0])
    }

    @Test
    fun `test add multiple instructions with varargs`() {
        val transaction = Transaction()
        val programId = TestHelpers.createTestPublicKeys(1)[0]
        val account1 = TestHelpers.createTestPublicKeys(1)[0]
        val account2 = TestHelpers.createTestPublicKeys(1)[0]

        val instruction1 = TransactionInstruction(
            keys = listOf(AccountMeta(account1, isSigner = true, isWritable = true)),
            programId = programId,
            data = byteArrayOf(1)
        )
        val instruction2 = TransactionInstruction(
            keys = listOf(AccountMeta(account2, isSigner = false, isWritable = true)),
            programId = programId,
            data = byteArrayOf(2)
        )

        transaction.add(instruction1, instruction2)

        assertEquals("Should have 2 instructions", 2, transaction.getInstructions().size)
        assertEquals("First instruction should match", instruction1, transaction.getInstructions()[0])
        assertEquals("Second instruction should match", instruction2, transaction.getInstructions()[1])
    }

    @Test
    fun `test AccountMeta data class`() {
        val pubkey = TestHelpers.createTestPublicKeys(1)[0]
        val meta = AccountMeta(pubkey, isSigner = true, isWritable = false)

        assertEquals("Pubkey should match", pubkey, meta.pubkey)
        assertTrue("Should be signer", meta.isSigner)
        assertFalse("Should not be writable", meta.isWritable)
    }

    @Test
    fun `test TransactionInstruction equality with ByteArray`() {
        val programId = TestHelpers.createTestPublicKeys(1)[0]
        val account = TestHelpers.createTestPublicKeys(1)[0]
        val keys = listOf(AccountMeta(account, isSigner = true, isWritable = true))
        val data = byteArrayOf(1, 2, 3)

        val instruction1 = TransactionInstruction(keys, programId, data)
        val instruction2 = TransactionInstruction(keys, programId, data)

        assertEquals("Instructions with same data should be equal", instruction1, instruction2)
    }

    @Test
    fun `test MessageHeader values`() {
        val header = MessageHeader(
            numRequiredSignatures = 2u,
            numReadonlySignedAccounts = 1u,
            numReadonlyUnsignedAccounts = 3u
        )

        assertEquals("Required signatures should be 2", 2.toUByte(), header.numRequiredSignatures)
        assertEquals("Readonly signed should be 1", 1.toUByte(), header.numReadonlySignedAccounts)
        assertEquals("Readonly unsigned should be 3", 3.toUByte(), header.numReadonlyUnsignedAccounts)
    }

    @Test
    fun `test CompiledInstruction with indices`() {
        val compiled = CompiledInstruction(
            programIdIndex = 5u,
            accounts = listOf(0u, 1u, 2u),
            data = byteArrayOf(0x00, 0x01, 0x02, 0x03)
        )

        assertEquals("Program index should be 5", 5.toUByte(), compiled.programIdIndex)
        assertEquals("Should have 3 account indices", 3, compiled.accounts.size)
        assertArrayEquals("Data should match", byteArrayOf(0, 1, 2, 3), compiled.data)
    }

    @Test
    fun `test SignaturePubkeyPair unsigned`() {
        val pubkey = TestHelpers.createTestPublicKeys(1)[0]
        val pair = SignaturePubkeyPair(signature = null, publicKey = pubkey)

        assertNull("Signature should be null (unsigned)", pair.signature)
        assertEquals("Pubkey should match", pubkey, pair.publicKey)
    }

    @Test
    fun `test SignaturePubkeyPair signed`() {
        val pubkey = TestHelpers.createTestPublicKeys(1)[0]
        val signature = ByteArray(64) { it.toByte() }
        val pair = SignaturePubkeyPair(signature = signature, publicKey = pubkey)

        assertNotNull("Signature should not be null", pair.signature)
        assertArrayEquals("Signature should match", signature, pair.signature)
        assertEquals("Pubkey should match", pubkey, pair.publicKey)
    }

    @Test
    fun `test Message immutable data class`() {
        val header = MessageHeader(1u, 0u, 1u)
        val accountKeys = listOf(TestHelpers.createTestPublicKeys(1)[0], TestHelpers.createTestPublicKeys(1)[0])
        val blockhash = "4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ"
        val instructions = listOf(
            CompiledInstruction(1u, listOf(0u), byteArrayOf(1, 2, 3))
        )

        val message = Message(header, accountKeys, blockhash, instructions)

        assertEquals("Header should match", header, message.header)
        assertEquals("Account keys should match", accountKeys, message.accountKeys)
        assertEquals("Blockhash should match", blockhash, message.recentBlockhash)
        assertEquals("Instructions should match", instructions, message.instructions)
    }

    @Test
    fun `test transaction returns immutable copies`() {
        val transaction = Transaction()
        val programId = TestHelpers.createTestPublicKeys(1)[0]
        val account = TestHelpers.createTestPublicKeys(1)[0]

        val instruction = TransactionInstruction(
            keys = listOf(AccountMeta(account, isSigner = true, isWritable = true)),
            programId = programId,
            data = byteArrayOf(1, 2, 3)
        )

        transaction.add(instruction)
        val instructions1 = transaction.getInstructions()
        val instructions2 = transaction.getInstructions()

        // Should return different list instances (defensive copies)
        assertNotSame("Should return different list instances", instructions1, instructions2)
        assertEquals("But contents should be equal", instructions1, instructions2)
    }

    @Test
    fun `test deserialize V0 transaction`() {
        // V0 transaction from iOS tests (contains Address Lookup Tables)
        val serializedVersionedTxBase64 = "AdTIDASR42TgVuXKkd7mJKk373J3LPVp85eyKMVcrboo9KTY8/vm6N/Cv0NiHqk2I8iYw6VX5ZaBKG8z9l1XjwiAAQACA+6qNbqfjaIENwt9GzEK/ENiB/ijGwluzBUmQ9xlTAMcCaS0ctnyxTcXXlJr7u2qtnaMgIAO2/c7RBD0ipHWUcEDBkZv5SEXMv/srbpyw5vnvIzlu8X3EmssQ5s6QAAAAJbI7VNs6MzREUlnzRaJpBKP8QQoDn2dWQvD0KIgHFDiAwIACQAgoQcAAAAAAAIABQEAAAQAATYPBwAKBDIBAyQWIw0oCxIdCA4iJzQRKwUZHxceHCohMBUJJiwpMxAaGC0TLhQxGyAMBiU2NS8VDgAAAADuAgAAAAAAAAIAAAAAAAAAAdGCTQiq5yw3+3m1sPoRNj0GtUNNs0FIMocxzt3zuoSZHQABAwQFBwgLDA8RFBcYGhwdHh8iIyUnKiwtLi8yFwIGCQoNDhASExUWGRsgISQmKCkrMDEz"
        val serializedVersionedTx = Base64.getDecoder().decode(serializedVersionedTxBase64)

        // Deserialize using Transaction.deserialize()
        val transaction = Transaction.deserialize(serializedVersionedTx)

        // Verify it's recognized as V0
        assertEquals("Should be V0 version", TransactionVersion.V0, transaction.version)

        // NOTE: This V0 transaction uses Address Lookup Tables, so instructions cannot be
        // reconstructed without ALT data. The instructions list will be empty.
        // The transaction can still be re-serialized correctly using the cached versioned message.

        // Verify re-serialization produces the same bytes
        val reserialized = transaction.serialize(requireAllSignatures = false, verifySignatures = false)
        assertArrayEquals("Re-serialized bytes should match original", serializedVersionedTx, reserialized)
    }

    @Test
    fun `test deserialize Legacy transaction`() {
        // Create a simple Legacy transaction
        val senderBytes = ByteArray(32) { 0x08 }
        val sender = com.myetherwallet.mewwalletkit.bip.bip44.PrivateKey.createWithPrivateKey(
            senderBytes,
            com.myetherwallet.mewwalletkit.bip.bip44.Network.SOLANA
        )
        val recentBlockhash = "EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k"
        val recipient = com.myetherwallet.mewwalletkit.bip.bip44.PublicKey.createWithBase58(
            "J3dxNj7nDRRqRRXuEMynDG57DkZK4jYRuv3Garmb1i99",
            com.myetherwallet.mewwalletkit.bip.bip44.Network.SOLANA
        )

        val transfer = SystemProgram.transfer(
            fromPubkey = sender.publicKey()!!,
            toPubkey = recipient,
            lamports = 49u
        )

        var expectedTransaction = Transaction(sender.publicKey(), recentBlockhash)
        expectedTransaction.add(transfer)
        expectedTransaction.sign(sender)

        val serialized = expectedTransaction.serialize()

        // Deserialize using Transaction.deserialize()
        val deserialized = Transaction.deserialize(serialized)

        // Verify it's recognized as Legacy
        assertEquals("Should be Legacy version", TransactionVersion.LEGACY, deserialized.version)

        // Verify blockhash matches
        assertEquals("Blockhash should match", recentBlockhash, deserialized.recentBlockhash)

        // Verify re-serialization produces the same bytes
        val reserialized = deserialized.serialize()
        assertArrayEquals("Re-serialized bytes should match original", serialized, reserialized)
    }
}
