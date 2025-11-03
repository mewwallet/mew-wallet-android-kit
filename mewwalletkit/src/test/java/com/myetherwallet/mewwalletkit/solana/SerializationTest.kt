package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.core.util.ShortVecCodec
import com.myetherwallet.mewwalletkit.core.util.toLittleEndianBytes
import com.myetherwallet.mewwalletkit.solana.serialization.MessageSerializer
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for Solana serialization (Phase 3)
 */
class SerializationTest {

    @Test
    fun `test ShortVecCodec encode length 0`() {
        val encoded = ShortVecCodec.encodeLength(0)
        assertArrayEquals("Length 0 should be [0x00]", byteArrayOf(0x00), encoded)
    }

    @Test
    fun `test ShortVecCodec encode length 127`() {
        val encoded = ShortVecCodec.encodeLength(127)
        assertArrayEquals("Length 127 should be [0x7f]", byteArrayOf(0x7f), encoded)
    }

    @Test
    fun `test ShortVecCodec encode length 128`() {
        val encoded = ShortVecCodec.encodeLength(128)
        assertArrayEquals("Length 128 should be [0x80, 0x01]",
            byteArrayOf(0x80.toByte(), 0x01), encoded)
    }

    @Test
    fun `test ShortVecCodec encode length 255`() {
        val encoded = ShortVecCodec.encodeLength(255)
        assertArrayEquals("Length 255 should be [0xff, 0x01]",
            byteArrayOf(0xff.toByte(), 0x01), encoded)
    }

    @Test
    fun `test ShortVecCodec encode length 256`() {
        val encoded = ShortVecCodec.encodeLength(256)
        assertArrayEquals("Length 256 should be [0x80, 0x02]",
            byteArrayOf(0x80.toByte(), 0x02), encoded)
    }

    @Test
    fun `test ShortVecCodec encode length 16383`() {
        val encoded = ShortVecCodec.encodeLength(16383)
        assertArrayEquals("Length 16383 should be [0xff, 0x7f]",
            byteArrayOf(0xff.toByte(), 0x7f), encoded)
    }

    @Test
    fun `test ShortVecCodec encode length 16384`() {
        val encoded = ShortVecCodec.encodeLength(16384)
        assertArrayEquals("Length 16384 should be [0x80, 0x80, 0x01]",
            byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x01), encoded)
    }

    @Test
    fun `test ShortVecCodec decode length`() {
        // Test various lengths
        val testCases = listOf(0, 1, 127, 128, 255, 256, 16383, 16384, 100000)

        for (length in testCases) {
            val encoded = ShortVecCodec.encodeLength(length)
            val (decoded, bytesRead) = ShortVecCodec.decodeLength(encoded)

            assertEquals("Decoded length should match original for $length", length, decoded)
            assertEquals("Bytes read should match encoded length for $length",
                encoded.size, bytesRead)
        }
    }

    @Test
    fun `test ShortVecCodec decode length with offset`() {
        val prefix = byteArrayOf(0x11, 0x22, 0x33)
        val lengthBytes = ShortVecCodec.encodeLength(12345)
        val combined = prefix + lengthBytes

        val (decoded, bytesRead) = ShortVecCodec.decodeLength(combined, offset = 3)

        assertEquals("Should decode correct length", 12345, decoded)
        assertEquals("Should read correct number of bytes", lengthBytes.size, bytesRead)
    }

    @Test
    fun `test ShortVecCodec encodeBytes`() {
        val data = byteArrayOf(0x01, 0x02, 0x03)
        val encoded = ShortVecCodec.encodeBytes(data)

        // Should be: [0x03] (length) + [0x01, 0x02, 0x03] (data)
        assertArrayEquals(byteArrayOf(0x03, 0x01, 0x02, 0x03), encoded)
    }

    @Test
    fun `test ShortVecCodec encodeList`() {
        val items = listOf(1, 2, 3)
        val encoded = ShortVecCodec.encodeList(items) { it.toByte().toLittleEndianBytes() }

        // Should be: [0x03] (length) + [0x01, 0x02, 0x03] (items)
        assertArrayEquals(byteArrayOf(0x03, 0x01, 0x02, 0x03), encoded)
    }

    @Test
    fun `test LittleEndianEncoder UByte`() {
        val value: UByte = 0xABu
        val bytes = value.toLittleEndianBytes()
        assertArrayEquals(byteArrayOf(0xAB.toByte()), bytes)
    }

    @Test
    fun `test LittleEndianEncoder UShort`() {
        val value: UShort = 0x1234u
        val bytes = value.toLittleEndianBytes()
        // Little-endian: low byte first
        assertArrayEquals(byteArrayOf(0x34, 0x12), bytes)
    }

    @Test
    fun `test LittleEndianEncoder UInt`() {
        val value: UInt = 0x12345678u
        val bytes = value.toLittleEndianBytes()
        // Little-endian: low byte first
        assertArrayEquals(byteArrayOf(0x78, 0x56, 0x34, 0x12), bytes)
    }

    @Test
    fun `test LittleEndianEncoder ULong`() {
        val value: ULong = 0x123456789ABCDEF0u
        val bytes = value.toLittleEndianBytes()
        // Little-endian: low byte first
        assertArrayEquals(
            byteArrayOf(0xF0.toByte(), 0xDE.toByte(), 0xBC.toByte(), 0x9A.toByte(),
                0x78, 0x56, 0x34, 0x12),
            bytes
        )
    }

    @Test
    fun `test MessageHeader serialization`() {
        val header = MessageHeader(
            numRequiredSignatures = 2u,
            numReadonlySignedAccounts = 1u,
            numReadonlyUnsignedAccounts = 3u
        )

        val serialized = MessageSerializer.serializeHeader(header)

        assertArrayEquals("Header should be 3 bytes",
            byteArrayOf(0x02, 0x01, 0x03), serialized)
    }

    @Test
    fun `test PublicKey serialization`() {
        val publicKey = TestHelpers.createTestPublicKeys(1)[0]
        val serialized = MessageSerializer.serializePublicKey(publicKey)

        assertEquals("PublicKey should be 32 bytes", 32, serialized.size)
        assertArrayEquals("Should match publicKey.data()", publicKey.data(), serialized)
    }

    @Test
    fun `test CompiledInstruction serialization`() {
        val instruction = CompiledInstruction(
            programIdIndex = 5u,
            accounts = listOf(0u, 1u, 2u),
            data = byteArrayOf(0x02, 0x00, 0x00, 0x00, 0xE8.toByte(), 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        )

        val serialized = MessageSerializer.serializeInstruction(instruction)

        // Format: [programId:1] [accountsLen:1] [accounts:N] [dataLen:1] [data:N]
        // [0x05] [0x03] [0x00, 0x01, 0x02] [0x0c] [12 bytes of data]
        assertEquals("First byte should be program ID index", 0x05, serialized[0].toInt() and 0xFF)
        assertEquals("Second byte should be accounts length", 0x03, serialized[1].toInt() and 0xFF)
    }

    @Test
    fun `test Message serialization structure`() {
        val publicKey = TestHelpers.createTestPublicKeys(1)[0]

        val message = Message(
            header = MessageHeader(1u, 0u, 1u),
            accountKeys = listOf(publicKey, publicKey), // Dummy keys for testing
            recentBlockhash = TestHelpers.createTestBlockhash(),
            instructions = listOf(
                CompiledInstruction(
                    programIdIndex = 1u,
                    accounts = listOf(0u),
                    data = byteArrayOf(0x02, 0x00, 0x00, 0x00)
                )
            )
        )

        val serialized = MessageSerializer.serializeMessage(message)

        // Verify structure:
        // [header:3] [accountKeys compact array] [blockhash:32] [instructions compact array]
        assertNotNull("Serialized message should not be null", serialized)
        assertTrue("Message should have reasonable size", serialized.size > 3 + 32)

        // First 3 bytes are header
        assertEquals("Header byte 1", 0x01, serialized[0].toInt() and 0xFF)
        assertEquals("Header byte 2", 0x00, serialized[1].toInt() and 0xFF)
        assertEquals("Header byte 3", 0x01, serialized[2].toInt() and 0xFF)
    }

    @Test
    fun `test Transaction serializeMessage`() {
        val publicKey = TestHelpers.createTestPublicKeys(1)[0]

        val transaction = Transaction(
            feePayer = publicKey,
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(publicKey, isSigner = true, isWritable = true)),
                programId = publicKey,
                data = byteArrayOf(0x02, 0x00, 0x00, 0x00)
            )
        )

        val serialized = transaction.serializeMessage()

        assertNotNull("Serialized message should not be null", serialized)
        assertTrue("Message should have content", serialized.isNotEmpty())

        // Should be deterministic - calling again should give same result
        val serialized2 = transaction.serializeMessage()
        assertArrayEquals("Serialization should be deterministic", serialized, serialized2)
    }

    @Test
    fun `test Transaction serialize requires all signatures by default`() {
        val publicKey = TestHelpers.createTestPublicKeys(1)[0]

        val transaction = Transaction(
            feePayer = publicKey,
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(publicKey, isSigner = true, isWritable = true)),
                programId = publicKey,
                data = byteArrayOf()
            )
        )

        try {
            transaction.serialize() // Should fail - no signatures
            fail("Should throw exception when signatures are missing")
        } catch (e: IllegalStateException) {
            assertTrue("Should mention missing signatures",
                e.message?.contains("Missing signatures") == true)
        }
    }

    @Test
    fun `test Transaction serialize allows missing signatures when disabled`() {
        val publicKey = TestHelpers.createTestPublicKeys(1)[0]

        val transaction = Transaction(
            feePayer = publicKey,
            recentBlockhash = TestHelpers.createTestBlockhash()
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(publicKey, isSigner = true, isWritable = true)),
                programId = publicKey,
                data = byteArrayOf()
            )
        )

        // Should not throw with requireAllSignatures = false
        val serialized = transaction.serialize(requireAllSignatures = false, verifySignatures = false)

        assertNotNull("Should serialize even without signatures", serialized)
        assertTrue("Should have content", serialized.isNotEmpty())
    }

    @Test
    fun `test SignaturePubkeyPair serialization with null signature`() {
        val publicKey = TestHelpers.createTestPublicKeys(1)[0]
        val pairs = listOf(SignaturePubkeyPair(null, publicKey))

        val serialized = MessageSerializer.serializeSignatures(pairs)

        // Format: [length:1] [64 zeros for unsigned]
        assertEquals("Should have length prefix + 64 bytes", 1 + 64, serialized.size)
        assertEquals("First byte should be length 1", 0x01, serialized[0].toInt() and 0xFF)

        // All signature bytes should be zero
        for (i in 1..64) {
            assertEquals("Unsigned signature should be zero", 0x00, serialized[i].toInt() and 0xFF)
        }
    }

    @Test
    fun `test SignaturePubkeyPair serialization with actual signature`() {
        val publicKey = TestHelpers.createTestPublicKeys(1)[0]
        val signature = ByteArray(64) { it.toByte() }
        val pairs = listOf(SignaturePubkeyPair(signature, publicKey))

        val serialized = MessageSerializer.serializeSignatures(pairs)

        // Format: [length:1] [64 signature bytes]
        assertEquals("Should have length prefix + 64 bytes", 1 + 64, serialized.size)
        assertEquals("First byte should be length 1", 0x01, serialized[0].toInt() and 0xFF)

        // Signature bytes should match
        for (i in 0 until 64) {
            assertEquals("Signature byte $i should match",
                signature[i], serialized[i + 1])
        }
    }
}
