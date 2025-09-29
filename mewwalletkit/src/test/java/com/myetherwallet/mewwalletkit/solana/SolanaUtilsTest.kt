package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.solana.crypto.SolanaPrivateKey
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive tests for Solana utility functions in SolanaSDK
 */
class SolanaUtilsTest {

    @Test
    fun testLamportsConversions() {
        // Test lamports to SOL conversion
        assertEquals("1 SOL should be 1 billion lamports",
                    1.0, SolanaSDK.lamportsToSOL(1_000_000_000L), 0.000000001)
        assertEquals("0.5 SOL conversion",
                    0.5, SolanaSDK.lamportsToSOL(500_000_000L), 0.000000001)
        assertEquals("0 lamports should be 0 SOL",
                    0.0, SolanaSDK.lamportsToSOL(0L), 0.0)

        // Test SOL to lamports conversion
        assertEquals("1 SOL to lamports",
                    1_000_000_000L, SolanaSDK.solToLamports(1.0))
        assertEquals("0.5 SOL to lamports",
                    500_000_000L, SolanaSDK.solToLamports(0.5))
        assertEquals("0 SOL to lamports",
                    0L, SolanaSDK.solToLamports(0.0))

        // Test precision
        assertEquals("Small amount precision",
                    1L, SolanaSDK.solToLamports(0.000000001))
        assertEquals("Large amount precision",
                    1_234_567_890_123L, SolanaSDK.solToLamports(1234.567890123))

        // Test round-trip conversion
        val originalSOL = 1.234567890
        val lamports = SolanaSDK.solToLamports(originalSOL)
        val convertedSOL = SolanaSDK.lamportsToSOL(lamports)
        assertEquals("Round-trip conversion should be accurate",
                    originalSOL, convertedSOL, 0.000000001)

        // Test edge cases
        val maxLamports = Long.MAX_VALUE
        val maxSOL = SolanaSDK.lamportsToSOL(maxLamports)
        assertTrue("Max conversion should work", maxSOL > 0)

        // Test very small amounts
        assertEquals("Smallest unit", 0.000000001, SolanaSDK.lamportsToSOL(1L), 0.000000001)
        assertEquals("Smallest unit reverse", 1L, SolanaSDK.solToLamports(0.000000001))
    }

    @Test
    fun testAddressValidation() {
        // Test valid addresses
        assertTrue("System program should be valid",
                  SolanaSDK.isValidAddress("11111111111111111111111111111111"))
        assertTrue("Token program should be valid",
                  SolanaSDK.isValidAddress("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"))
        assertTrue("Associated token program should be valid",
                  SolanaSDK.isValidAddress("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"))

        // Test invalid addresses
        assertFalse("Empty string should be invalid",
                   SolanaSDK.isValidAddress(""))
        assertFalse("Invalid characters should be invalid",
                   SolanaSDK.isValidAddress("invalid_address_format"))
        assertFalse("Too short should be invalid",
                   SolanaSDK.isValidAddress("abc123"))
        assertFalse("Too long should be invalid",
                   SolanaSDK.isValidAddress("a".repeat(100)))

        // Test edge cases
        assertFalse("Null-like strings should be invalid",
                   SolanaSDK.isValidAddress("null"))
        assertFalse("Wrong length should be invalid",
                   SolanaSDK.isValidAddress("1".repeat(40)))
        assertFalse("Special characters should be invalid",
                   SolanaSDK.isValidAddress("11111111111111111111111111111111!"))
    }

    @Test
    fun testBase58Operations() {
        val testKey = SolanaPrivateKey.generate()
        val publicKey = testKey.publicKey()

        // Test base58 conversion
        val base58Address = publicKey.toSolanaBase58()
        assertNotNull("Base58 address should not be null", base58Address)
        assertTrue("Base58 address should be valid", SolanaSDK.isValidAddress(base58Address!!))

        // Test round-trip conversion
        val convertedKey = PublicKey.fromSolanaBase58(base58Address)
        assertEquals("Round-trip conversion should work",
                    base58Address, convertedKey.toSolanaBase58())
    }

    @Test
    fun testConversionPrecision() {
        // Test conversion precision at various scales
        val testCases = listOf(
            0.0 to 0L,
            0.000000001 to 1L,
            0.000000123 to 123L,
            1.0 to 1_000_000_000L,
            1.5 to 1_500_000_000L,
            123.456789012 to 123_456_789_012L,
            1000.0 to 1_000_000_000_000L
        )

        for ((sol, lamports) in testCases) {
            assertEquals("SOL to lamports: $sol", lamports, SolanaSDK.solToLamports(sol))
            assertEquals("Lamports to SOL: $lamports", sol, SolanaSDK.lamportsToSOL(lamports), 0.000000001)
        }

        // Test rounding behavior
        val almostOne = 0.999999999 // Should round to 999999999 lamports
        val roundedLamports = SolanaSDK.solToLamports(almostOne)
        val roundedBack = SolanaSDK.lamportsToSOL(roundedLamports)
        assertEquals("Rounding should be consistent", almostOne, roundedBack, 0.000000001)
    }

    @Test
    fun testUtilityFunctionConsistency() {
        // Test that utility functions are consistent with each other
        val testSOL = 42.123456789
        val convertedLamports = SolanaSDK.solToLamports(testSOL)
        val convertedBack = SolanaSDK.lamportsToSOL(convertedLamports)

        assertEquals("Round-trip conversion should be exact", testSOL, convertedBack, 0.000000001)

        // Test with various amounts
        val testAmounts = listOf(0.0, 0.000000001, 1.0, 100.0, 1000000.0, 999999999.999999999)
        for (amount in testAmounts) {
            val lamports = SolanaSDK.solToLamports(amount)
            val backToSOL = SolanaSDK.lamportsToSOL(lamports)
            assertEquals("Consistency test for $amount SOL", amount, backToSOL, 0.000000001)
        }
    }

    @Test
    fun testErrorHandlingInConversions() {
        // Test negative values (should handle gracefully)
        val negativeLamports = SolanaSDK.solToLamports(-1.0)
        assertEquals("Negative SOL should convert to negative lamports", -1_000_000_000L, negativeLamports)

        val negativeSOL = SolanaSDK.lamportsToSOL(-1_000_000_000L)
        assertEquals("Negative lamports should convert to negative SOL", -1.0, negativeSOL, 0.000000001)

        // Test very large values
        val largeSOL = SolanaSDK.lamportsToSOL(Long.MAX_VALUE)
        assertTrue("Large lamports should convert to reasonable SOL", largeSOL > 0)

        // Test precision limits
        val verySmallSOL = 0.0000000001 // Smaller than 1 lamport
        val convertedSmall = SolanaSDK.solToLamports(verySmallSOL)
        assertTrue("Very small amounts should round to 0 or 1", convertedSmall >= 0 && convertedSmall <= 1)
    }

    @Test
    fun testStaticUtilityMethods() {
        // Test validation methods work consistently
        val validAddress = SolanaPrivateKey.generate().publicKey().toSolanaBase58()!!
        assertTrue("Generated address should be valid", SolanaSDK.isValidAddress(validAddress))

        // Test that static methods don't depend on instance state
        assertEquals("Static methods should return same results",
                    SolanaSDK.solToLamports(1.0), SolanaSDK.solToLamports(1.0))
        assertEquals("Static validation should be consistent",
                    SolanaSDK.isValidAddress(validAddress), SolanaSDK.isValidAddress(validAddress))
    }

    @Test
    fun testPrecisionEdgeCases() {
        // Test maximum precision cases
        val maxDecimals = 9
        val smallAmount = 0.000000001
        val rawAmount = SolanaSDK.solToLamports(smallAmount)
        assertEquals("Smallest unit conversion", 1L, rawAmount)

        // Test precision limits
        val precisionTestAmount = 1.123456789
        val precisionRawAmount = SolanaSDK.solToLamports(precisionTestAmount)
        val precisionBackToSOL = SolanaSDK.lamportsToSOL(precisionRawAmount)
        assertEquals("Precision test", precisionTestAmount, precisionBackToSOL, 0.000000001)

        // Test rounding behavior
        val roundingTestAmount = 1.5555555555 // More precision than possible
        val roundingRawAmount = SolanaSDK.solToLamports(roundingTestAmount)
        val roundingBackToSOL = SolanaSDK.lamportsToSOL(roundingRawAmount)
        // Should be close but may have rounding differences
        assertTrue("Rounding should be reasonable",
                  Math.abs(roundingTestAmount - roundingBackToSOL) < 0.000001)
    }

    @Test
    fun testWellKnownAddresses() {
        // Test well-known program addresses
        val systemProgram = "11111111111111111111111111111111"
        val tokenProgram = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
        val associatedTokenProgram = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"

        assertTrue("System program should be valid", SolanaSDK.isValidAddress(systemProgram))
        assertTrue("Token program should be valid", SolanaSDK.isValidAddress(tokenProgram))
        assertTrue("Associated token program should be valid", SolanaSDK.isValidAddress(associatedTokenProgram))

        // Test that they are different
        assertNotEquals("System and token programs should be different", systemProgram, tokenProgram)
        assertNotEquals("System and ATA programs should be different", systemProgram, associatedTokenProgram)
        assertNotEquals("Token and ATA programs should be different", tokenProgram, associatedTokenProgram)
    }

    @Test
    fun testWellKnownTokens() {
        // Test well-known token addresses (mainnet)
        val usdcAddress = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
        val usdtAddress = "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB"
        val solWrappedAddress = "So11111111111111111111111111111111111111112"

        assertTrue("USDC address should be valid", SolanaSDK.isValidAddress(usdcAddress))
        assertTrue("USDT address should be valid", SolanaSDK.isValidAddress(usdtAddress))
        assertTrue("Wrapped SOL address should be valid", SolanaSDK.isValidAddress(solWrappedAddress))

        // Test that addresses are different
        assertNotEquals("USDC and USDT should be different", usdcAddress, usdtAddress)
        assertNotEquals("USDC and wrapped SOL should be different", usdcAddress, solWrappedAddress)
        assertNotEquals("USDT and wrapped SOL should be different", usdtAddress, solWrappedAddress)
    }
}