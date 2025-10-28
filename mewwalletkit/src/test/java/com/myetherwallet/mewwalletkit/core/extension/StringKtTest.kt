package com.myetherwallet.mewwalletkit.core.extension

import org.junit.Test
import org.junit.Assert.*
import java.math.BigInteger

/**
 * Comprehensive tests for String extension functions
 * Testing hex conversion, EIP55 checksum, Base58 decoding, and validation functions
 */
class StringKtTest {

    @Test
    fun `hexToByteArray tests`() {
        // Test empty string
        assertArrayEquals(byteArrayOf(), "".hexToByteArray())

        // Test simple hex strings
        assertArrayEquals(byteArrayOf(0x00), "00".hexToByteArray())
        assertArrayEquals(byteArrayOf(0xff.toByte()), "ff".hexToByteArray())
        assertArrayEquals(byteArrayOf(0x01, 0x23, 0x45), "012345".hexToByteArray())

        // Test with 0x prefix
        assertArrayEquals(byteArrayOf(0xde.toByte(), 0xad.toByte(), 0xbe.toByte(), 0xef.toByte()), "0xdeadbeef".hexToByteArray())

        // Test single character (should be padded)
        assertArrayEquals(byteArrayOf(0x05), "5".hexToByteArray())
        assertArrayEquals(byteArrayOf(0x0a), "a".hexToByteArray())

        // Test case insensitive
        assertArrayEquals(byteArrayOf(0xab.toByte(), 0xcd.toByte()), "ABCD".hexToByteArray())
        assertArrayEquals(byteArrayOf(0xab.toByte(), 0xcd.toByte()), "abcd".hexToByteArray())

        // Test mixed case
        assertArrayEquals(byteArrayOf(0xab.toByte(), 0xcd.toByte()), "AbCd".hexToByteArray())
    }

    @Test(expected = IllegalStateException::class)
    fun `hexToByteArray throws on odd length without padding`() {
        "123".hexToByteArray()
    }

    @Test(expected = NumberFormatException::class)
    fun `hexToByteArray throws on invalid hex characters`() {
        "gg".hexToByteArray()
    }

    @Test
    fun `toBigIntegerExponential tests`() {
        // Test simple numbers
        assertEquals(BigInteger.valueOf(123), "123".toBigIntegerExponential())
        assertEquals(BigInteger.ZERO, "0".toBigIntegerExponential())

        // Test exponential notation
        assertEquals(BigInteger.valueOf(1000), "1e3".toBigIntegerExponential())
        assertEquals(BigInteger.valueOf(1230000), "1.23e6".toBigIntegerExponential())

        // Test decimal values (should truncate)
        assertEquals(BigInteger.valueOf(123), "123.456".toBigIntegerExponential())
        assertEquals(BigInteger.valueOf(999), "999.999".toBigIntegerExponential())

        // Test negative values
        assertEquals(BigInteger.valueOf(-123), "-123".toBigIntegerExponential())
        assertEquals(BigInteger.valueOf(-1000), "-1e3".toBigIntegerExponential())
    }

    @Test
    fun `hexToBigInteger tests`() {
        // Test empty and zero
        assertEquals(BigInteger.ZERO, "".hexToBigInteger())
        assertEquals(BigInteger.ZERO, "0".hexToBigInteger())
        assertEquals(BigInteger.ZERO, "0x0".hexToBigInteger())

        // Test simple values
        assertEquals(BigInteger.valueOf(255), "ff".hexToBigInteger())
        assertEquals(BigInteger.valueOf(255), "0xff".hexToBigInteger())
        assertEquals(BigInteger.valueOf(4660), "1234".hexToBigInteger())

        // Test large values
        assertEquals(BigInteger("18446744073709551615"), "ffffffffffffffff".hexToBigInteger())

        // Test negative values
        assertEquals(BigInteger.valueOf(-255), "-ff".hexToBigInteger())
        assertEquals(BigInteger.valueOf(-255), "-0xff".hexToBigInteger())

        // Test case insensitive
        assertEquals(BigInteger.valueOf(255), "FF".hexToBigInteger())
        assertEquals(BigInteger.valueOf(255), "Ff".hexToBigInteger())
    }

    @Test
    fun `isHex tests`() {
        // Test valid hex strings
        assertTrue("Valid hex should pass", "123abc".isHex())
        assertTrue("Valid hex with prefix should pass", "0x123abc".isHex())
        assertTrue("Empty string should be valid hex", "".isHex())
        assertTrue("Single digit should be valid hex", "a".isHex())

        // Test invalid hex strings
        assertFalse("Invalid character should fail", "123g".isHex())
        assertFalse("Space should fail", "12 34".isHex())
        assertFalse("Special character should fail", "12@34".isHex())

        // Test with prefix checking
        assertTrue("With prefix and checkPrefix=true should pass", "0x123".isHex(true))
        assertFalse("Without prefix and checkPrefix=true should fail", "123".isHex(true))
        assertTrue("Without prefix and checkPrefix=false should pass", "123".isHex(false))

        // Test case insensitive
        assertTrue("Uppercase should be valid", "ABCDEF".isHex())
        assertTrue("Mixed case should be valid", "aBcDeF".isHex())
    }

    @Test
    fun `hasHexPrefix tests`() {
        assertTrue("String with 0x prefix should return true", "0x123".hasHexPrefix())
        assertFalse("String without prefix should return false", "123".hasHexPrefix())
        assertFalse("Empty string should return false", "".hasHexPrefix())
        assertFalse("String with partial prefix should return false", "0".hasHexPrefix())
        assertTrue("Just prefix should return true", "0x".hasHexPrefix())
    }

    @Test
    fun `removeHexPrefix tests`() {
        assertEquals("Should remove 0x prefix", "123", "0x123".removeHexPrefix())
        assertEquals("Should not modify string without prefix", "123", "123".removeHexPrefix())
        assertEquals("Should handle empty string", "", "".removeHexPrefix())
        assertEquals("Should handle just prefix", "", "0x".removeHexPrefix())
        assertEquals("Should not remove partial prefix", "0", "0".removeHexPrefix())
    }

    @Test
    fun `addHexPrefix tests`() {
        assertEquals("Should add prefix to string without it", "0x123", "123".addHexPrefix())
        assertEquals("Should not modify string with prefix", "0x123", "0x123".addHexPrefix())
        assertEquals("Should handle empty string", "0x", "".addHexPrefix())
        assertEquals("Should not duplicate prefix", "0x123", "0x123".addHexPrefix())
    }

    @Test
    fun `eip55 tests`() {
        // Test known EIP55 addresses
        val address1 = "5aaeb6053f3e94c9b9a09f33669435e7ef1beaed"
        val expected1 = "5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed"
        assertEquals("EIP55 should work for known address", expected1, address1.eip55())

        val address2 = "0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed"
        val expected2 = "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed"
        assertEquals("EIP55 should work with prefix", expected2, address2.eip55())

        // Test with uppercase input
        val uppercaseAddress = "5AAEB6053F3E94C9B9A09F33669435E7EF1BEAED"
        assertEquals("EIP55 should work with uppercase input", expected1, uppercaseAddress.eip55())

        // Test mixed case input
        val mixedAddress = "5aAeB6053f3E94c9B9a09F33669435e7Ef1beAed"
        assertEquals("EIP55 should work with mixed case input", expected1, mixedAddress.eip55())

        // Test empty string
        assertEquals("EIP55 should handle empty string", "", "".eip55())

        // Test invalid length (should still process)
        val shortAddress = "123456"
        assertNotNull("EIP55 should handle short address", shortAddress.eip55())
    }

    @Test
    fun `decodeBase58 tests`() {
        val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

        // Test empty string
        assertArrayEquals("Empty string should decode to empty array",
                         byteArrayOf(), "".decodeBase58(alphabet))

        // Test simple cases
        assertArrayEquals("Single character should decode correctly",
                         byteArrayOf(0x61), "2g".decodeBase58(alphabet))

        // Test leading zeros
        assertArrayEquals("Leading zeros should be preserved",
                         byteArrayOf(0x00, 0x00, 0x00, 0x61), "1112g".decodeBase58(alphabet))

        // Test known vectors
        val testVectors = mapOf(
            "2g" to "61",
            "a3gV" to "626262",
            "aPEr" to "636363",
            "1111111111" to "00000000000000000000"
        )

        for ((encoded, expectedHex) in testVectors) {
            val expected = expectedHex.hexToByteArray()
            val actual = encoded.decodeBase58(alphabet)
            assertArrayEquals("Base58 decode should match expected for $encoded", expected, actual)
        }

        // Test invalid characters
        assertNull("Invalid character should return null", "0".decodeBase58(alphabet))
        assertNull("Invalid character should return null", "O".decodeBase58(alphabet))
        assertNull("Invalid character should return null", "I".decodeBase58(alphabet))
        assertNull("Invalid character should return null", "l".decodeBase58(alphabet))
    }

    @Test
    fun `hashPersonalMessage tests`() {
        // Test basic functionality
        val message = "test message"
        val hashedMessage = message.hashPersonalMessage()

        assertEquals("Hash should be 32 bytes", 32, hashedMessage.size)

        // Test consistency
        val hashedAgain = message.hashPersonalMessage()
        assertArrayEquals("Hash should be consistent", hashedMessage, hashedAgain)

        // Test different messages produce different hashes
        val differentMessage = "different message"
        val hashedDifferent = differentMessage.hashPersonalMessage()
        assertFalse("Different messages should produce different hashes",
                   hashedMessage.contentEquals(hashedDifferent))

        // Test empty message
        val emptyMessage = ""
        val hashedEmpty = emptyMessage.hashPersonalMessage()
        assertEquals("Empty message hash should be 32 bytes", 32, hashedEmpty.size)

        // Test special characters
        val specialMessage = "Hello, 世界! 🌍"
        val hashedSpecial = specialMessage.hashPersonalMessage()
        assertEquals("Special character message hash should be 32 bytes", 32, hashedSpecial.size)

        // Test that it's different from regular keccak256
        val regularHash = message.toByteArray().keccak256()
        assertFalse("Personal message hash should differ from regular keccak256",
                   hashedMessage.contentEquals(regularHash))
    }

    @Test
    fun `crc32 tests`() {
        // Test empty string
        val emptyCrc = "".crc32()
        assertEquals("Empty string CRC32 should be 8 characters", 8, emptyCrc.length)
        assertEquals("Empty string CRC32 should be all uppercase", emptyCrc, emptyCrc.uppercase())

        // Test known values
        val testCrc = "test".crc32()
        assertEquals("Test CRC32 should be 8 characters", 8, testCrc.length)
        assertTrue("CRC32 should be valid hex", testCrc.isHex())

        // Test consistency
        val testCrcAgain = "test".crc32()
        assertEquals("CRC32 should be consistent", testCrc, testCrcAgain)

        // Test different strings produce different CRC32
        val differentCrc = "different".crc32()
        assertNotEquals("Different strings should have different CRC32", testCrc, differentCrc)

        // Test format (should be uppercase hex)
        assertTrue("CRC32 should be valid hex", testCrc.matches(Regex("[0-9A-F]{8}")))

        // Test special characters
        val specialCrc = "Hello, 世界! 🌍".crc32()
        assertEquals("Special character CRC32 should be 8 characters", 8, specialCrc.length)
        assertTrue("Special character CRC32 should be valid hex", specialCrc.isHex())
    }

    @Test
    fun `isAllCharsHex private function behavior through isHex`() {
        // Test all valid hex characters
        val validHexChars = "0123456789ABCDEFabcdef"
        assertTrue("All valid hex chars should pass", validHexChars.isHex())

        // Test each invalid character
        val invalidChars = "GHIJKLMNOPQRSTUVWXYZ!@#$%^&*()_+-=[]{}|;':\",./<>?"
        for (char in invalidChars) {
            assertFalse("Character '$char' should make hex invalid", char.toString().isHex())
        }

        // Test mixed valid and invalid
        assertFalse("Mixed valid/invalid should fail", "123G".isHex())
        assertFalse("Mixed valid/invalid should fail", "ABC!".isHex())
    }

    @Test
    fun `edge cases and error conditions`() {
        // Test very long hex string
        val longHex = "a".repeat(1000)
        assertTrue("Very long hex should be valid", longHex.isHex())

        val longHexBytes = longHex.hexToByteArray()
        assertEquals("Long hex should convert correctly", 500, longHexBytes.size)

        // Test very large BigInteger conversion
        val largeHex = "f".repeat(64) // 256 bits of 1s
        val largeBigInt = largeHex.hexToBigInteger()
        assertTrue("Large BigInteger should be positive", largeBigInt > BigInteger.ZERO)

        // Test negative hex conversion edge cases
        assertEquals("Negative zero should be zero", BigInteger.ZERO, "-0".hexToBigInteger())
        assertEquals("Negative empty should be zero", BigInteger.ZERO, "-".hexToBigInteger())

        // Test EIP55 with non-standard length
        val shortHex = "123"
        val shortEip55 = shortHex.eip55()
        assertNotNull("Short address should still process", shortEip55)
        assertEquals("Short address length should be preserved", 3, shortEip55?.length)

        // Test Base58 with different alphabets
        val customAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val customDecoded = "ABC".decodeBase58(customAlphabet)
        assertNotNull("Custom alphabet should work", customDecoded)

        // Test CRC32 with very long string
        val longString = "x".repeat(10000)
        val longCrc = longString.crc32()
        assertEquals("Long string CRC32 should still be 8 chars", 8, longCrc.length)
    }

    @Test
    fun `prefix handling edge cases`() {
        // Test multiple prefixes
        assertEquals("Should only remove first prefix", "0x123", "0x0x123".removeHexPrefix())

        // Test prefix-like strings
        assertFalse("0X should not be recognized as prefix", "0X123".hasHexPrefix())
        assertEquals("0X should not be removed", "0X123", "0X123".removeHexPrefix())

        // Test prefix addition idempotency
        val withPrefix = "0x123"
        assertEquals("Adding prefix should be idempotent", withPrefix, withPrefix.addHexPrefix().addHexPrefix())
    }

    @Test
    fun `unicode and special character handling`() {
        // Test unicode in CRC32
        val unicodeString = "Hello 世界 🌍"
        val unicodeCrc = unicodeString.crc32()
        assertEquals("Unicode CRC32 should be 8 characters", 8, unicodeCrc.length)

        // Test unicode in personal message hashing
        val unicodeHash = unicodeString.hashPersonalMessage()
        assertEquals("Unicode hash should be 32 bytes", 32, unicodeHash.size)

        // Test very long unicode string
        val longUnicode = "🌍".repeat(100)
        val longUnicodeHash = longUnicode.hashPersonalMessage()
        assertEquals("Long unicode hash should be 32 bytes", 32, longUnicodeHash.size)
    }
}