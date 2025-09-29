package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.solana.core.SolanaPublicKey
import com.myetherwallet.mewwalletkit.solana.crypto.SolanaPrivateKey
import com.myetherwallet.mewwalletkit.solana.encoding.ShortVec
import org.junit.Test
import org.junit.Assert.*

/**
 * Simplified tests for core Solana functionality
 */
class SolanaSimpleTest {

    @Test
    fun testPublicKeyFromBase58() {
        // Test creating public key from known Base58 string
        val base58Key = "11111111111111111111111111111111"
        val publicKey = PublicKey.fromSolanaBase58(base58Key)

        assertEquals(base58Key, publicKey.toSolanaBase58())
        assertEquals(32, publicKey.data().size)
        assertTrue(publicKey.data().all { it == 0.toByte() })
    }

    @Test
    fun testPublicKeyFromHex() {
        // Test creating public key from hex
        val hexKey = "0000000000000000000000000000000000000000000000000000000000000000"
        val publicKey = SolanaPublicKey.fromHex(hexKey)

        assertEquals(32, publicKey.data().size)
        // Basic validation that it's all zeros
        assertTrue(publicKey.data().all { it == 0.toByte() })
    }

    @Test
    fun testPrivateKeyGeneration() {
        // Test generating private key
        val privateKey = SolanaPrivateKey.generate()
        val publicKey = privateKey.publicKey()

        assertEquals(32, privateKey.seed().size)
        assertEquals(32, publicKey.data().size)
        assertNotNull(publicKey.toSolanaBase58())
    }

    @Test
    fun testPrivateKeyFromSeed() {
        // Test creating private key from seed
        val seed = ByteArray(32) { it.toByte() }
        val privateKey = SolanaPrivateKey.fromSeed(seed)
        val publicKey = privateKey.publicKey()

        assertEquals(32, publicKey.data().size)

        // Test that same seed produces same key
        val privateKey2 = SolanaPrivateKey.fromSeed(seed)
        assertEquals(publicKey, privateKey2.publicKey())
    }

    @Test
    fun testShortVecEncodingDecoding() {
        // Test various length encodings
        testShortVecLength(0)
        testShortVecLength(1)
        testShortVecLength(127)
        testShortVecLength(128)
        testShortVecLength(255)
        testShortVecLength(256)
        testShortVecLength(16383)
        testShortVecLength(16384)
    }

    private fun testShortVecLength(length: Int) {
        val encoded = ShortVec.encodeLength(length)
        val offset = intArrayOf(0)
        val decoded = ShortVec.decodeLength(encoded, offset)
        assertEquals("Length $length encoding/decoding failed", length, decoded)
        assertEquals("Offset not properly updated for length $length", encoded.size, offset[0])
    }

    @Test
    fun testShortVecArrayEncoding() {
        val items = listOf("hello", "world", "test")
        val encoded = ShortVec.encodeArray(items) { it.toByteArray() }

        assertTrue("Encoded array should not be empty", encoded.isNotEmpty())

        // Should start with length encoding (3 items = 0x03)
        assertEquals(3.toByte(), encoded[0])
    }

    @Test
    fun testBase58Roundtrip() {
        // Test Base58 encoding roundtrip
        val testBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
                                   17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32)

        val publicKey = SolanaPublicKey.fromBytes(testBytes)
        val base58 = publicKey.toSolanaBase58()!!
        val decoded = SolanaPublicKey.fromBase58(base58)

        assertArrayEquals(testBytes, decoded.data())
        assertEquals(publicKey, decoded)
    }

    @Test
    fun testHexRoundtrip() {
        // Test hex encoding roundtrip
        val testBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
                                   17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32)

        val publicKey = SolanaPublicKey.fromBytes(testBytes)
        val hex = "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20"
        val decoded = SolanaPublicKey.fromHex(hex)

        assertArrayEquals(testBytes, decoded.data())
        assertEquals(publicKey, decoded)
    }

    @Test
    fun testConstantsValidation() {
        // Test that our constants are correct
        assertEquals(32, com.myetherwallet.mewwalletkit.solana.core.SolanaConstants.PUBLIC_KEY_LENGTH)
        assertEquals(32, com.myetherwallet.mewwalletkit.solana.core.SolanaConstants.PRIVATE_KEY_LENGTH)
        assertEquals(64, com.myetherwallet.mewwalletkit.solana.core.SolanaConstants.SIGNATURE_LENGTH)
        assertEquals(501, com.myetherwallet.mewwalletkit.solana.core.SolanaConstants.COIN_TYPE)
    }

    @Test
    fun testSolanaUtilityMethods() {
        // Test conversion utilities
        val sol = 1.0
        val lamports = 1_000_000_000L

        assertEquals(lamports, com.myetherwallet.mewwalletkit.solana.Solana.solToLamports(sol))
        assertEquals(sol, com.myetherwallet.mewwalletkit.solana.Solana.lamportsToSol(lamports), 0.0001)
    }

    @Test
    fun testAddressValidation() {
        // Test address validation
        val validAddresses = listOf(
            "11111111111111111111111111111111",
            "9WzDXwBbmkg8ZTbNMqUxvQRAyrZzDsGYdLVL9zYtAWWM",
            "So11111111111111111111111111111111111111112"
        )

        validAddresses.forEach { address ->
            assertTrue("Address $address should be valid",
                      com.myetherwallet.mewwalletkit.solana.Solana.isValidAddress(address))
        }

        val invalidAddresses = listOf(
            "invalid",
            "123",
            "",
            "toolongaddressthatshouldnotbevalid123456789012345678901234567890"
        )

        invalidAddresses.forEach { address ->
            assertFalse("Address $address should be invalid",
                       com.myetherwallet.mewwalletkit.solana.Solana.isValidAddress(address))
        }
    }
}