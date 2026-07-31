package com.myetherwallet.mewwalletkit.bip.bip44

import com.myetherwallet.mewwalletkit.bip.bip44.exception.InvalidDataException
import com.myetherwallet.mewwalletkit.core.extension.encodeBase58String
import com.myetherwallet.mewwalletkit.core.extension.toHexString
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for PrivateKey initialization methods: createWithHex and createWithBase58
 */
class PrivateKeyInitializersTest {

    // Test private key (32 bytes) - from standard test mnemonic
    private val testPrivateKeyHex = "c0b9355922b6df97e88b04058dee478908328dd959adf61991d7ca64e4d27a8c"
    private val testPrivateKeyBytes = byteArrayOf(
        0xc0.toByte(), 0xb9.toByte(), 0x35, 0x59, 0x22, 0xb6.toByte(), 0xdf.toByte(), 0x97.toByte(),
        0xe8.toByte(), 0x8b.toByte(), 0x04, 0x05, 0x8d.toByte(), 0xee.toByte(), 0x47, 0x89.toByte(),
        0x08, 0x32, 0x8d.toByte(), 0xd9.toByte(), 0x59, 0xad.toByte(), 0xf6.toByte(), 0x19,
        0x91.toByte(), 0xd7.toByte(), 0xca.toByte(), 0x64, 0xe4.toByte(), 0xd2.toByte(), 0x7a, 0x8c.toByte()
    )

    @Test
    fun `test createWithHex creates valid PrivateKey`() {
        val privateKey = PrivateKey.createWithHex(testPrivateKeyHex, Network.SOLANA)

        assertNotNull("PrivateKey should not be null", privateKey)
        assertArrayEquals("Private key data should match", testPrivateKeyBytes, privateKey.data())
        assertEquals("Private key hex should match", testPrivateKeyHex, privateKey.data().toHexString())
    }

    @Test
    fun `test createWithHex handles 0x prefix`() {
        val privateKeyWithPrefix = "0x$testPrivateKeyHex"
        val privateKey = PrivateKey.createWithHex(privateKeyWithPrefix, Network.SOLANA)

        assertNotNull("PrivateKey should not be null", privateKey)
        assertArrayEquals("Private key data should match", testPrivateKeyBytes, privateKey.data())
    }

    @Test
    fun `test createWithHex works with Ethereum network`() {
        val privateKey = PrivateKey.createWithHex(testPrivateKeyHex, Network.ETHEREUM)

        assertNotNull("PrivateKey should not be null", privateKey)
        assertEquals("Network should be Ethereum", Network.ETHEREUM, privateKey.network)
        assertArrayEquals("Private key data should match", testPrivateKeyBytes, privateKey.data())
    }

    @Test
    fun `test createWithHex works with Bitcoin network`() {
        val privateKey = PrivateKey.createWithHex(testPrivateKeyHex, Network.BITCOIN)

        assertNotNull("PrivateKey should not be null", privateKey)
        assertEquals("Network should be Bitcoin", Network.BITCOIN, privateKey.network)
        assertArrayEquals("Private key data should match", testPrivateKeyBytes, privateKey.data())
    }

    @Test(expected = InvalidDataException::class)
    fun `test createWithHex throws on invalid hex`() {
        PrivateKey.createWithHex("invalid_hex_string", Network.SOLANA)
    }

    @Test(expected = InvalidDataException::class)
    fun `test createWithHex throws on odd length hex`() {
        PrivateKey.createWithHex("abc", Network.SOLANA)
    }

    @Test
    fun `test createWithBase58 creates valid PrivateKey for Solana`() {
        val alphabet = Network.SOLANA.alphabet()!!
        val base58Encoded = testPrivateKeyBytes.encodeBase58String(alphabet)!!

        val privateKey = PrivateKey.createWithBase58(base58Encoded, Network.SOLANA)

        assertNotNull("PrivateKey should not be null", privateKey)
        assertArrayEquals("Private key data should match", testPrivateKeyBytes, privateKey.data())
        assertEquals("Network should be Solana", Network.SOLANA, privateKey.network)
    }

    @Test
    fun `test createWithBase58 creates valid PrivateKey for Bitcoin`() {
        val alphabet = Network.BITCOIN.alphabet()!!
        val base58Encoded = testPrivateKeyBytes.encodeBase58String(alphabet)!!

        val privateKey = PrivateKey.createWithBase58(base58Encoded, Network.BITCOIN)

        assertNotNull("PrivateKey should not be null", privateKey)
        assertArrayEquals("Private key data should match", testPrivateKeyBytes, privateKey.data())
        assertEquals("Network should be Bitcoin", Network.BITCOIN, privateKey.network)
    }

    @Test(expected = InvalidDataException::class)
    fun `test createWithBase58 throws on invalid Base58`() {
        PrivateKey.createWithBase58("0OIl", Network.SOLANA) // Invalid Base58 characters
    }

    @Test(expected = InvalidDataException::class)
    fun `test createWithBase58 throws for network without Base58 support`() {
        val alphabet = Network.SOLANA.alphabet()!!
        val base58Encoded = testPrivateKeyBytes.encodeBase58String(alphabet)!!

        // Ethereum doesn't use Base58 (alphabet is null)
        PrivateKey.createWithBase58(base58Encoded, Network.ETHEREUM)
    }

    @Test
    fun `test createWithHex and createWithBase58 produce equivalent keys`() {
        // Create key from hex
        val keyFromHex = PrivateKey.createWithHex(testPrivateKeyHex, Network.SOLANA)

        // Create key from Base58
        val alphabet = Network.SOLANA.alphabet()!!
        val base58Encoded = testPrivateKeyBytes.encodeBase58String(alphabet)!!
        val keyFromBase58 = PrivateKey.createWithBase58(base58Encoded, Network.SOLANA)

        // Both should have identical private key data
        assertArrayEquals(
            "Keys created from hex and Base58 should have identical data",
            keyFromHex.data(),
            keyFromBase58.data()
        )

        // Both should produce same public key
        val pubkeyFromHex = keyFromHex.publicKey()
        val pubkeyFromBase58 = keyFromBase58.publicKey()

        assertNotNull("Public key from hex should not be null", pubkeyFromHex)
        assertNotNull("Public key from Base58 should not be null", pubkeyFromBase58)
        assertArrayEquals(
            "Public keys should match",
            pubkeyFromHex!!.data(),
            pubkeyFromBase58!!.data()
        )
    }

    @Test
    fun `test createWithHex produces same result as createWithPrivateKey`() {
        val keyFromHex = PrivateKey.createWithHex(testPrivateKeyHex, Network.SOLANA)
        val keyFromBytes = PrivateKey.createWithPrivateKey(testPrivateKeyBytes, Network.SOLANA)

        assertArrayEquals(
            "Keys should have identical private key data",
            keyFromHex.data(),
            keyFromBytes.data()
        )

        assertEquals(
            "Keys should have same network",
            keyFromHex.network,
            keyFromBytes.network
        )
    }

    @Test
    fun `test round-trip hex encoding`() {
        // Create key from hex
        val originalKey = PrivateKey.createWithHex(testPrivateKeyHex, Network.SOLANA)

        // Export as hex
        val exportedHex = originalKey.data().toHexString()

        // Re-import
        val reimportedKey = PrivateKey.createWithHex(exportedHex, Network.SOLANA)

        assertArrayEquals(
            "Round-trip should preserve private key data",
            originalKey.data(),
            reimportedKey.data()
        )
    }

    @Test
    fun `test round-trip Base58 encoding for Solana`() {
        val alphabet = Network.SOLANA.alphabet()!!

        // Create key from Base58
        val base58Encoded = testPrivateKeyBytes.encodeBase58String(alphabet)!!
        val originalKey = PrivateKey.createWithBase58(base58Encoded, Network.SOLANA)

        // Export as Base58
        val exportedBase58 = originalKey.data().encodeBase58String(alphabet)!!

        // Re-import
        val reimportedKey = PrivateKey.createWithBase58(exportedBase58, Network.SOLANA)

        assertArrayEquals(
            "Round-trip should preserve private key data",
            originalKey.data(),
            reimportedKey.data()
        )
    }
}
