package com.myetherwallet.mewwalletkit.core.extension

import org.junit.Assert
import org.junit.Test

/**
 * Created by BArtWell on 21.05.2019.
 */
class ByteArrayKtTest {

    // length == 3 bytes/24 bits
    val data = byteArrayOf(0b00001111.toByte(), 0b00110011.toByte(), 0b11001100.toByte())

    @Test
    fun toBits() {
        Assert.assertNull(data.toBits(250, 20))
        Assert.assertNull(data.toBits(-1, 10))
        Assert.assertNull(data.toBits(0, -1))

        Assert.assertEquals(0b0000, data.toBits(0, 4))
        Assert.assertEquals(0b0000, data.toBits(0 until 4))
        Assert.assertEquals(0b00001111, data.toBits(0, 8))
        Assert.assertEquals(0b00001111, data.toBits(0 until 8))
        Assert.assertEquals(0b00001111_0011, data.toBits(0, 12))
        Assert.assertEquals(0b00001111_0011, data.toBits(0 until 12))
        Assert.assertEquals(0b10011_110011, data.toBits(11, 11))
        Assert.assertEquals(0b10011_110011, data.toBits(11 until 22))
        Assert.assertEquals(0b00001111_00110011_11001100, data.toBits(0, 24))
        Assert.assertEquals(0b00001111_00110011_11001100, data.toBits(0 until 24))
        Assert.assertEquals(0b11001100, data.toBits(16, 8))
        Assert.assertEquals(0b11001100, data.toBits(16 until 24))
    }

    @Test
    fun sha256() {
        val data = "00000000000000000000000000000000".hexToByteArray()
        val expected = "374708fff7719dd5979ec875d56cd2286f6d3cf7ec317a3b25632aab28ec37bb".hexToByteArray()
        Assert.assertArrayEquals(expected, data.sha256())
    }

    @Test
    fun md5() {
        val data = "test".toByteArray()
        val expected = "098f6bcd4621d373cade4e832627b4f6".hexToByteArray()
        Assert.assertArrayEquals(expected, data.md5())
    }

    @Test
    fun `Base58 tests`() {
        val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        val testVectors = arrayOf(
            Base58TestVector("", ""),
            Base58TestVector("61", "2g"),
            Base58TestVector("626262", "a3gV"),
            Base58TestVector("636363", "aPEr"),
            Base58TestVector("73696d706c792061206c6f6e6720737472696e67", "2cFupjhnEsSn59qHXstmK2ffpLv2"),
            Base58TestVector("00eb15231dfceb60925886b67d065299925915aeb172c06647", "1NS17iag9jJgTHD1VXjvLCEnZuQ3rJDE9L"),
            Base58TestVector("516b6fcd0f", "ABnLTmg"),
            Base58TestVector("bf4f89001e670274dd", "3SEo3LWLoPntC"),
            Base58TestVector("572e4794", "3EFU7m"),
            Base58TestVector("ecac89cad93923c02321", "EJDM8drfXA6uyA"),
            Base58TestVector("10c8511e", "Rt5zm"),
            Base58TestVector("00000000000000000000", "1111111111"),
            Base58TestVector(
                "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff",
                "1cWB5HCBdLjAuqGGReWE3R3CguuwSjw6RHn39s2yuDRTS5NsBgNiFpWgAnEx6VQi8csexkgYw3mdYrMHr8x9i7aEwP8kZ7vccXWqKDvGv3u1GxFKPuAkn8JCPPGDMf3vMMnbzm6Nh9zh1gcNsMvH3ZNLmP5fSG6DGbbi2tuwMWPthr4boWwCxf7ewSgNQeacyozhKDDQQ1qL5fQFUW52QKUZDZ5fw3KXNQJMcNTcaB723LchjeKun7MuGW5qyCBZYzA1KjofN1gYBV3NqyhQJ3Ns746GNuf9N2pQPmHz4xpnSrrfCvy6TVVz5d4PdrjeshsWQwpZsZGzvbdAdN8MKV5QsBDY"
            ),
            Base58TestVector("000111d38e5fc9071ffcd20b4a763cc9ae4f252bb4e48fd66a835e252ada93ff480d6dd43dc62a641155a5", "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz")
        )

        // Should encode correctly
        for (vector in testVectors) {
            val encoded = vector.decoded.encodeBase58String(alphabet)
            Assert.assertEquals(vector.encoded, encoded)
        }

        // Should decode correctly
        for (vector in testVectors) {
            val decoded = vector.encoded.decodeBase58(alphabet)
            Assert.assertArrayEquals("Decode error (" + vector.encoded + ")", vector.decoded, decoded)
        }
    }

    @Test
    fun `toHexString tests`() {
        // Test empty array
        Assert.assertEquals("", byteArrayOf().toHexString())

        // Test single byte
        Assert.assertEquals("00", byteArrayOf(0x00).toHexString())
        Assert.assertEquals("ff", byteArrayOf(0xff.toByte()).toHexString())
        Assert.assertEquals("7f", byteArrayOf(0x7f).toHexString())

        // Test multiple bytes
        Assert.assertEquals("010203", byteArrayOf(0x01, 0x02, 0x03).toHexString())
        Assert.assertEquals("deadbeef", byteArrayOf(0xde.toByte(), 0xad.toByte(), 0xbe.toByte(), 0xef.toByte()).toHexString())

        // Test all possible byte values
        val allBytes = (0..255).map { it.toByte() }.toByteArray()
        val expectedHex = (0..255).joinToString("") { "%02x".format(it) }
        Assert.assertEquals(expectedHex, allBytes.toHexString())
    }

    @Test
    fun `toBigInteger tests`() {
        // Test empty array
        Assert.assertEquals(java.math.BigInteger.ZERO, byteArrayOf().toBigInteger())

        // Test single byte values
        Assert.assertEquals(java.math.BigInteger.ZERO, byteArrayOf(0x00).toBigInteger())
        Assert.assertEquals(java.math.BigInteger.ONE, byteArrayOf(0x01).toBigInteger())
        Assert.assertEquals(java.math.BigInteger.valueOf(255), byteArrayOf(0xff.toByte()).toBigInteger())

        // Test multi-byte values
        Assert.assertEquals(java.math.BigInteger.valueOf(256), byteArrayOf(0x01, 0x00).toBigInteger())
        Assert.assertEquals(java.math.BigInteger.valueOf(65535), byteArrayOf(0xff.toByte(), 0xff.toByte()).toBigInteger())

        // Test large value
        val largeBytes = byteArrayOf(0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        Assert.assertEquals(java.math.BigInteger("72057594037927936"), largeBytes.toBigInteger())
    }

    @Test
    fun `prefix tests`() {
        val testData = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)

        // Test normal prefix
        Assert.assertArrayEquals(byteArrayOf(0x01), testData.prefix(1))
        Assert.assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03), testData.prefix(3))

        // Test edge cases
        Assert.assertArrayEquals(byteArrayOf(), testData.prefix(0))
        Assert.assertArrayEquals(testData, testData.prefix(5))

        // Test with empty array
        Assert.assertArrayEquals(byteArrayOf(), byteArrayOf().prefix(0))
    }

    @Test
    fun `padLeft tests`() {
        val testData = byteArrayOf(0x01, 0x02)

        // Test padding with default zero byte
        Assert.assertArrayEquals(byteArrayOf(0x00, 0x00, 0x01, 0x02), testData.padLeft(4))
        Assert.assertArrayEquals(byteArrayOf(0x00, 0x01, 0x02), testData.padLeft(3))

        // Test no padding needed
        Assert.assertArrayEquals(testData, testData.padLeft(2))
        Assert.assertArrayEquals(testData, testData.padLeft(1))

        // Test padding with custom byte
        Assert.assertArrayEquals(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x01, 0x02), testData.padLeft(4, 0xff.toByte()))

        // Test empty array
        Assert.assertArrayEquals(byteArrayOf(0x00, 0x00), byteArrayOf().padLeft(2))
    }

    @Test
    fun `padRight tests`() {
        val testData = byteArrayOf(0x01, 0x02)

        // Test padding with default zero byte
        Assert.assertArrayEquals(byteArrayOf(0x01, 0x02, 0x00, 0x00), testData.padRight(4))
        Assert.assertArrayEquals(byteArrayOf(0x01, 0x02, 0x00), testData.padRight(3))

        // Test no padding needed
        Assert.assertArrayEquals(testData, testData.padRight(2))
        Assert.assertArrayEquals(testData, testData.padRight(1))

        // Test padding with custom byte
        Assert.assertArrayEquals(byteArrayOf(0x01, 0x02, 0xff.toByte(), 0xff.toByte()), testData.padRight(4, 0xff.toByte()))

        // Test empty array
        Assert.assertArrayEquals(byteArrayOf(0x00, 0x00), byteArrayOf().padRight(2))
    }

    @Test
    fun `sha512 tests`() {
        // Test empty data
        val emptyData = byteArrayOf()
        val expectedEmpty = "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e".hexToByteArray()
        Assert.assertArrayEquals(expectedEmpty, emptyData.sha512())

        // Test known input
        val testData = "test".toByteArray()
        val expectedTest = "ee26b0dd4af7e749aa1a8ee3c10ae9923f618980772e473f8819a5d4940e0db27ac185f8a0e1d5f84f88bc887fd67b143732c304cc5fa9ad8e6f57f50028a8ff".hexToByteArray()
        Assert.assertArrayEquals(expectedTest, testData.sha512())
    }

    @Test
    fun `keccak256 tests`() {
        // Test empty data
        val emptyData = byteArrayOf()
        val expectedEmpty = "c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470".hexToByteArray()
        Assert.assertArrayEquals(expectedEmpty, emptyData.keccak256())

        // Test known input
        val testData = "test".toByteArray()
        val expectedTest = "9c22ff5f21f0b81b113e63f7db6da94fedef11b2119b4088b89664fb9a3cb658".hexToByteArray()
        Assert.assertArrayEquals(expectedTest, testData.keccak256())

        // Test Ethereum address calculation
        val publicKeyData = "04b2e2230c7f2e6d8e7b5b73c1b7e4b8a9c8f4e1d9b2c5e7f1a4c9e8b2d6f3a8c9e2b5f8a1d4c7e0b3a6f9c2e5d8a1b4c7e0a3d6f9b2e5c8a1b4d7e0a3c6f9b2e5".hexToByteArray()
        val address = publicKeyData.keccak256().copyOfRange(12, 32)
        Assert.assertEquals(20, address.size)
    }

    @Test
    fun `ripemd160 tests`() {
        // Test empty data
        val emptyData = byteArrayOf()
        val emptyResult = emptyData.ripemd160()
        Assert.assertEquals(20, emptyResult.size)

        // Test known input
        val testData = "test".toByteArray()
        val testResult = testData.ripemd160()
        Assert.assertEquals(20, testResult.size)

        // RIPEMD160 should always produce 20-byte output
        val largeData = ByteArray(1000) { it.toByte() }
        val largeResult = largeData.ripemd160()
        Assert.assertEquals(20, largeResult.size)
    }

    @Test
    fun `eip55 tests`() {
        // Test standard Ethereum address (20 bytes = 40 hex chars)
        val addressBytes = "5aaeb6053f3e94c9b9a09f33669435e7ef1beaed".hexToByteArray()
        val eip55Result = addressBytes.eip55()
        Assert.assertNotNull("EIP55 result should not be null", eip55Result)
        Assert.assertEquals("EIP55 result should be 40 chars", 40, eip55Result?.length)
        Assert.assertTrue("EIP55 result should be valid hex string", eip55Result?.matches(Regex("[0-9a-fA-F]{40}")) == true)

        // Test empty array (returns empty string, like String.eip55() does)
        val emptyResult = byteArrayOf().eip55()
        Assert.assertEquals("Empty array should return empty string", "", emptyResult)
    }

    @Test
    fun `encodeBase58 edge cases`() {
        val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

        // Test null result for empty data
        val emptyResult = byteArrayOf().encodeBase58(alphabet)
        Assert.assertArrayEquals(byteArrayOf(), emptyResult)

        // Test single zero byte
        val singleZero = byteArrayOf(0x00)
        val singleZeroResult = singleZero.encodeBase58String(alphabet)
        Assert.assertEquals("1", singleZeroResult)

        // Test multiple leading zeros
        val multipleZeros = byteArrayOf(0x00, 0x00, 0x00, 0x01)
        val multipleZerosResult = multipleZeros.encodeBase58String(alphabet)
        Assert.assertTrue("Should start with multiple '1's", multipleZerosResult?.startsWith("111") ?: false)
    }

    @Test
    fun `secureCompare tests`() {
        val data1 = byteArrayOf(0x01, 0x02, 0x03)
        val data2 = byteArrayOf(0x01, 0x02, 0x03)
        val data3 = byteArrayOf(0x01, 0x02, 0x04)
        val data4 = byteArrayOf(0x01, 0x02)

        // Test equal arrays
        Assert.assertTrue("Identical arrays should be equal", data1.secureCompare(data2))
        Assert.assertTrue("Same array should be equal to itself", data1.secureCompare(data1))

        // Test different content
        Assert.assertFalse("Different content should not be equal", data1.secureCompare(data3))

        // Test different lengths
        Assert.assertFalse("Different lengths should not be equal", data1.secureCompare(data4))

        // Test empty arrays
        Assert.assertTrue("Empty arrays should be equal", byteArrayOf().secureCompare(byteArrayOf()))

        // Test empty vs non-empty
        Assert.assertFalse("Empty vs non-empty should not be equal", byteArrayOf().secureCompare(data1))
    }

    @Test
    fun `hashPersonalMessage tests`() {
        // Test known Ethereum personal message
        val message = "test message".toByteArray()
        val hashedMessage = message.hashPersonalMessage()

        Assert.assertEquals(32, hashedMessage.size)
        Assert.assertNotNull("Hashed message should not be null", hashedMessage)

        // Test empty message
        val emptyMessage = byteArrayOf()
        val hashedEmpty = emptyMessage.hashPersonalMessage()
        Assert.assertEquals(32, hashedEmpty.size)

        // Test consistency
        val hashedAgain = message.hashPersonalMessage()
        Assert.assertArrayEquals("Hash should be consistent", hashedMessage, hashedAgain)

        // Test different messages produce different hashes
        val differentMessage = "different message".toByteArray()
        val hashedDifferent = differentMessage.hashPersonalMessage()
        Assert.assertFalse("Different messages should produce different hashes",
                          hashedMessage.contentEquals(hashedDifferent))
    }

    @Test
    fun `isNullOrEmpty tests`() {
        // Test null
        val nullArray: ByteArray? = null
        Assert.assertTrue("Null array should be null or empty", nullArray.isNullOrEmpty())

        // Test empty
        Assert.assertTrue("Empty array should be null or empty", byteArrayOf().isNullOrEmpty())

        // Test non-empty
        Assert.assertFalse("Non-empty array should not be null or empty", byteArrayOf(0x01).isNullOrEmpty())

        // Test single byte
        Assert.assertFalse("Single byte array should not be null or empty", byteArrayOf(0x00).isNullOrEmpty())
    }

    @Test
    fun `secp256k1 function parameter validation`() {
        val validHash = ByteArray(32) { 0x01 }
        val validPrivateKey = ByteArray(32) { 0x02 }
        val validSignature = ByteArray(65) { 0x03 }

        // Test secp256k1RecoverableSign with invalid parameters
        val invalidHash = ByteArray(31) { 0x01 }
        val invalidPrivateKey = ByteArray(31) { 0x02 }

        Assert.assertNull("Invalid hash size should return null",
                         invalidHash.secp256k1RecoverableSign(validPrivateKey))
        Assert.assertNull("Invalid private key size should return null",
                         validHash.secp256k1RecoverableSign(invalidPrivateKey))

        // Test secp256k1SerializeSignature with invalid size
        val invalidSignature = ByteArray(64) { 0x03 }
        Assert.assertNull("Invalid signature size should return null",
                         invalidSignature.secp256k1SerializeSignature())

        // Test secp256k1ParseSignature with invalid size
        Assert.assertNull("Invalid signature size should return null",
                         invalidSignature.secp256k1ParseSignature())

        // Test secp256k1RecoverPublicKey with invalid parameters
        Assert.assertNull("Invalid signature size should return null",
                         invalidSignature.secp256k1RecoverPublicKey(validHash, true))
        Assert.assertNull("Invalid hash size should return null",
                         validSignature.secp256k1RecoverPublicKey(invalidHash, true))
    }

    @Test
    fun `toBits edge cases`() {
        val data = byteArrayOf(0xff.toByte(), 0x00, 0xaa.toByte())

        // Test invalid ranges
        Assert.assertNull("Negative start should return null", data.toBits(-1, 4))
        Assert.assertNull("Out of bounds should return null", data.toBits(24, 1))
        Assert.assertNull("Invalid range should return null", data.toBits(10, 15))

        // Test zero length (empty range returns null in original implementation)
        Assert.assertNull("Zero length should return null", data.toBits(0, 0))

        // Test single bit
        Assert.assertEquals("First bit should be 1", 1, data.toBits(0, 1))
        Assert.assertEquals("Ninth bit should be 0", 0, data.toBits(8, 1))

        // Test full byte boundaries
        Assert.assertEquals("First byte should be 0xff", 0xff, data.toBits(0, 8))
        Assert.assertEquals("Second byte should be 0x00", 0x00, data.toBits(8, 8))
        Assert.assertEquals("Third byte should be 0xaa", 0xaa, data.toBits(16, 8))
    }

    private data class Base58TestVector(
        private val decodedString: String,
        val encoded: String
    ) {
        val decoded: ByteArray by lazy { decodedString.hexToByteArray() }
    }
}