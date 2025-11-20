package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.Network
import com.myetherwallet.mewwalletkit.bip.bip44.PrivateKey
import com.myetherwallet.mewwalletkit.core.extension.hexToByteArray
import org.junit.Assert.*
import org.junit.Test

class VersionedTransactionTest {

    @Test
    fun `test VersionedTransaction with Legacy message initialization`() {
        val keys = TestHelpers.createTestPublicKeys(3)
        val message = Message(
            header = MessageHeader(1u, 0u, 1u),
            accountKeys = keys,
            recentBlockhash = "EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k",
            instructions = emptyList()
        )

        val versionedMessage = VersionedMessage.Legacy(message)
        val versionedTx = VersionedTransaction(versionedMessage)

        assertEquals(1, versionedTx.signatures.size)
        assertTrue(versionedTx.signatures.all { it.all { byte -> byte == 0.toByte() } })
        assertEquals(TransactionVersion.LEGACY, versionedTx.version)
        assertEquals("EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k", versionedTx.recentBlockhash)
    }

    @Test
    fun `test VersionedTransaction with V0 message initialization`() {
        val keys = TestHelpers.createTestPublicKeys(2)
        val messageV0 = MessageV0(
            header = MessageHeader(1u, 0u, 1u),
            staticAccountKeys = keys,
            recentBlockhash = "EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k",
            compiledInstructions = emptyList(),
            addressTableLookups = emptyList()
        )

        val versionedMessage = VersionedMessage.V0(messageV0)
        val versionedTx = VersionedTransaction(versionedMessage)

        assertEquals(1, versionedTx.signatures.size)
        assertEquals(TransactionVersion.V0, versionedTx.version)
        assertEquals("EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k", versionedTx.recentBlockhash)
    }

    @Test
    fun `test sign Legacy transaction`() {
        val privateKeyHex = "94c8ae05bf067d52a6c88e85993c3f1bcf7a3b560d42bba4bada7c8a45c7f93e"
        val privateKey = PrivateKey.createWithPrivateKey(privateKeyHex.hexToByteArray(), Network.SOLANA)

        val publicKey = privateKey.publicKey()!!
        val keys = listOf(publicKey)

        val message = Message(
            header = MessageHeader(1u, 0u, 0u),
            accountKeys = keys,
            recentBlockhash = "EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k",
            instructions = emptyList()
        )

        val versionedMessage = VersionedMessage.Legacy(message)
        val versionedTx = VersionedTransaction(versionedMessage)

        versionedTx.sign(privateKey)

        assertFalse(versionedTx.signatures[0].all { it == 0.toByte() })
        assertEquals(64, versionedTx.signatures[0].size)
    }

    @Test
    fun `test sign V0 transaction`() {
        val privateKeyHex = "94c8ae05bf067d52a6c88e85993c3f1bcf7a3b560d42bba4bada7c8a45c7f93e"
        val privateKey = PrivateKey.createWithPrivateKey(privateKeyHex.hexToByteArray(), Network.SOLANA)

        val publicKey = privateKey.publicKey()!!
        val keys = listOf(publicKey)

        val messageV0 = MessageV0(
            header = MessageHeader(1u, 0u, 0u),
            staticAccountKeys = keys,
            recentBlockhash = "EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k",
            compiledInstructions = emptyList(),
            addressTableLookups = emptyList()
        )

        val versionedMessage = VersionedMessage.V0(messageV0)
        val versionedTx = VersionedTransaction(versionedMessage)

        versionedTx.sign(privateKey)

        assertFalse(versionedTx.signatures[0].all { it == 0.toByte() })
        assertEquals(64, versionedTx.signatures[0].size)
    }

    @Test
    fun `test serialize and deserialize Legacy transaction`() {
        val privateKeyHex = "94c8ae05bf067d52a6c88e85993c3f1bcf7a3b560d42bba4bada7c8a45c7f93e"
        val privateKey = PrivateKey.createWithPrivateKey(privateKeyHex.hexToByteArray(), Network.SOLANA)

        val publicKey = privateKey.publicKey()!!
        val keys = listOf(publicKey)

        val message = Message(
            header = MessageHeader(1u, 0u, 0u),
            accountKeys = keys,
            recentBlockhash = "EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k",
            instructions = emptyList()
        )

        val versionedMessage = VersionedMessage.Legacy(message)
        val versionedTx = VersionedTransaction(versionedMessage)
        versionedTx.sign(privateKey)

        val serialized = versionedTx.serialize()

        val deserialized = VersionedTransaction.deserialize(serialized)

        assertEquals(versionedTx.version, deserialized.version)
        assertEquals(versionedTx.signatures.size, deserialized.signatures.size)
        assertArrayEquals(versionedTx.signatures[0], deserialized.signatures[0])
        assertEquals(versionedTx.recentBlockhash, deserialized.recentBlockhash)
    }

    @Test
    fun `test serialize and deserialize V0 transaction`() {
        val privateKeyHex = "94c8ae05bf067d52a6c88e85993c3f1bcf7a3b560d42bba4bada7c8a45c7f93e"
        val privateKey = PrivateKey.createWithPrivateKey(privateKeyHex.hexToByteArray(), Network.SOLANA)

        val publicKey = privateKey.publicKey()!!
        val keys = listOf(publicKey)

        val messageV0 = MessageV0(
            header = MessageHeader(1u, 0u, 0u),
            staticAccountKeys = keys,
            recentBlockhash = "EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k",
            compiledInstructions = emptyList(),
            addressTableLookups = emptyList()
        )

        val versionedMessage = VersionedMessage.V0(messageV0)
        val versionedTx = VersionedTransaction(versionedMessage)
        versionedTx.sign(privateKey)

        val serialized = versionedTx.serialize()

        val deserialized = VersionedTransaction.deserialize(serialized)

        assertEquals(TransactionVersion.V0, deserialized.version)
        assertEquals(versionedTx.signatures.size, deserialized.signatures.size)
        assertArrayEquals(versionedTx.signatures[0], deserialized.signatures[0])
        assertEquals(versionedTx.recentBlockhash, deserialized.recentBlockhash)
    }

    @Test
    fun `test addSignature with valid signer`() {
        val privateKeyHex = "94c8ae05bf067d52a6c88e85993c3f1bcf7a3b560d42bba4bada7c8a45c7f93e"
        val privateKey = PrivateKey.createWithPrivateKey(privateKeyHex.hexToByteArray(), Network.SOLANA)

        val publicKey = privateKey.publicKey()!!
        val keys = listOf(publicKey)

        val message = Message(
            header = MessageHeader(1u, 0u, 0u),
            accountKeys = keys,
            recentBlockhash = "EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k",
            instructions = emptyList()
        )

        val versionedMessage = VersionedMessage.Legacy(message)
        val versionedTx = VersionedTransaction(versionedMessage)

        val signature = ByteArray(64) { 0xFF.toByte() }
        versionedTx.addSignature(publicKey, signature)

        assertArrayEquals(signature, versionedTx.signatures[0])
    }

    @Test(expected = VersionedTransaction.VersionedTransactionError.InvalidSignature::class)
    fun `test addSignature with invalid signature size`() {
        val keys = TestHelpers.createTestPublicKeys(1)
        val message = Message(
            header = MessageHeader(1u, 0u, 0u),
            accountKeys = keys,
            recentBlockhash = "EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k",
            instructions = emptyList()
        )

        val versionedMessage = VersionedMessage.Legacy(message)
        val versionedTx = VersionedTransaction(versionedMessage)

        val invalidSignature = ByteArray(32)
        versionedTx.addSignature(keys[0], invalidSignature)
    }

    @Test(expected = VersionedTransaction.VersionedTransactionError.SignerNotRequired::class)
    fun `test addSignature with non-required signer`() {
        val keys = TestHelpers.createTestPublicKeys(2)
        val message = Message(
            header = MessageHeader(1u, 0u, 1u),
            accountKeys = keys,
            recentBlockhash = "EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k",
            instructions = emptyList()
        )

        val versionedMessage = VersionedMessage.Legacy(message)
        val versionedTx = VersionedTransaction(versionedMessage)

        val signature = ByteArray(64)
        versionedTx.addSignature(keys[1], signature)
    }

    @Test
    fun `test multiple signers`() {
        val privateKey1Hex = "94c8ae05bf067d52a6c88e85993c3f1bcf7a3b560d42bba4bada7c8a45c7f93e"
        val privateKey2Hex = "a4c8ae05bf067d52a6c88e85993c3f1bcf7a3b560d42bba4bada7c8a45c7f93f"

        val privateKey1 = PrivateKey.createWithPrivateKey(privateKey1Hex.hexToByteArray(), Network.SOLANA)
        val privateKey2 = PrivateKey.createWithPrivateKey(privateKey2Hex.hexToByteArray(), Network.SOLANA)

        val publicKey1 = privateKey1.publicKey()!!
        val publicKey2 = privateKey2.publicKey()!!
        val keys = listOf(publicKey1, publicKey2)

        val message = Message(
            header = MessageHeader(2u, 0u, 0u),
            accountKeys = keys,
            recentBlockhash = "EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k",
            instructions = emptyList()
        )

        val versionedMessage = VersionedMessage.Legacy(message)
        val versionedTx = VersionedTransaction(versionedMessage)

        versionedTx.sign(listOf(privateKey1, privateKey2))

        assertEquals(2, versionedTx.signatures.size)
        assertFalse(versionedTx.signatures[0].all { it == 0.toByte() })
        assertFalse(versionedTx.signatures[1].all { it == 0.toByte() })
    }

    @Test
    fun `test fromTransaction with Legacy transaction`() {
        val privateKeyHex = "94c8ae05bf067d52a6c88e85993c3f1bcf7a3b560d42bba4bada7c8a45c7f93e"
        val privateKey = PrivateKey.createWithPrivateKey(privateKeyHex.hexToByteArray(), Network.SOLANA)
        val publicKey = privateKey.publicKey()!!

        // Create a Transaction using builder pattern
        val transaction = Transaction(
            feePayer = publicKey,
            recentBlockhash = "EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k"
        )

        // Add a transfer instruction
        val recipient = TestHelpers.createTestPublicKeys(1)[0]
        val transferInstruction = SystemProgram.transfer(
            fromPubkey = publicKey,
            toPubkey = recipient,
            lamports = 1000000uL
        )
        transaction.add(transferInstruction)

        // Convert to VersionedTransaction
        val versionedTx = VersionedTransaction.fromTransaction(transaction)

        // Verify version is Legacy
        assertEquals(TransactionVersion.LEGACY, versionedTx.version)
        assertEquals("EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k", versionedTx.recentBlockhash)

        // Verify can sign
        versionedTx.sign(privateKey)
        assertFalse(versionedTx.signatures[0].all { it == 0.toByte() })
        assertEquals(64, versionedTx.signatures[0].size)

        // Verify can serialize
        val serialized = versionedTx.serialize()
        assertTrue(serialized.isNotEmpty())
    }
}
