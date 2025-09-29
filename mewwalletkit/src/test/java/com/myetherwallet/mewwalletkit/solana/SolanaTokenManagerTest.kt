package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.solana.crypto.SolanaPrivateKey
import com.myetherwallet.mewwalletkit.solana.rpc.SolanaNetwork
import com.myetherwallet.mewwalletkit.solana.rpc.SolanaRpcClient
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Comprehensive tests for the SolanaTokenManager
 */
class SolanaTokenManagerTest {

    private lateinit var tokenManager: SolanaTokenManager
    private lateinit var rpcClient: SolanaRpcClient
    private lateinit var testPrivateKey: SolanaPrivateKey
    private lateinit var testPublicKey: PublicKey
    private lateinit var mint: PublicKey

    @Before
    fun setup() {
        rpcClient = SolanaRpcClient.builder()
            .setNetwork(SolanaNetwork.DEVNET)
            .build()

        tokenManager = SolanaTokenManager(rpcClient)
        testPrivateKey = SolanaPrivateKey.generate()
        testPublicKey = testPrivateKey.publicKey()
        mint = SolanaPrivateKey.generate().publicKey()
    }

    @Test
    fun testTokenInfo() {
        // Test TokenInfo data class
        val tokenInfo = SolanaTokenManager.TokenInfo(
            mint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
            symbol = "USDC",
            name = "USD Coin",
            decimals = 6,
            supply = 1000000000L
        )

        assertEquals("USDC", tokenInfo.symbol)
        assertEquals("USD Coin", tokenInfo.name)
        assertEquals(6, tokenInfo.decimals)
        assertEquals(1000000000L, tokenInfo.supply)
        assertNotNull(tokenInfo.mint)
    }

    @Test
    fun testTokenBalance() {
        // Test TokenBalance data class and methods
        val tokenBalance = SolanaTokenManager.TokenBalance(
            mint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
            amount = 1500000L, // 1.5 USDC (6 decimals)
            decimals = 6,
            uiAmount = 1.5
        )

        assertEquals("EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v", tokenBalance.mint)
        assertEquals(1500000L, tokenBalance.amount)
        assertEquals(6, tokenBalance.decimals)
        assertEquals(1.5, tokenBalance.uiAmount, 0.000001)

        // Test display amount formatting
        val displayAmount = tokenBalance.getDisplayAmount()
        assertEquals("1.500000", displayAmount)

        // Test different decimals
        val tokenBalance9Decimals = SolanaTokenManager.TokenBalance(
            mint = "So11111111111111111111111111111111111111112",
            amount = 1000000000L, // 1 SOL (9 decimals)
            decimals = 9,
            uiAmount = 1.0
        )

        val displayAmount9 = tokenBalance9Decimals.getDisplayAmount()
        assertEquals("1.000000000", displayAmount9)

        // Test zero decimals
        val tokenBalance0Decimals = SolanaTokenManager.TokenBalance(
            mint = "TokenWithNoDecimals",
            amount = 42L,
            decimals = 0,
            uiAmount = 42.0
        )

        val displayAmount0 = tokenBalance0Decimals.getDisplayAmount()
        assertEquals("42", displayAmount0)
    }

    @Test
    fun testGetTokenAccounts() = runBlocking {
        // Test with PublicKey
        val tokenAccounts = tokenManager.getTokenAccounts(testPublicKey)
        assertNotNull("Token accounts should not be null", tokenAccounts)
        assertTrue("Token accounts should be a list", tokenAccounts is List)

        // Test with string address
        val address = testPublicKey.toSolanaBase58()!!
        val tokenAccountsFromAddress = tokenManager.getTokenAccounts(address)
        assertNotNull("Token accounts should not be null", tokenAccountsFromAddress)
        assertTrue("Token accounts should be a list", tokenAccountsFromAddress is List)

        // Results should be consistent
        assertEquals("Results should be the same",
                    tokenAccounts.size, tokenAccountsFromAddress.size)
    }

    @Test
    fun testGetTokenBalance() = runBlocking {
        // Test getting token balance (might return null if account doesn't exist)
        val balance = tokenManager.getTokenBalance(testPublicKey, mint)

        if (balance != null) {
            assertEquals(mint.toSolanaBase58()!!, balance.mint)
            assertTrue("Amount should be non-negative", balance.amount >= 0)
            assertTrue("Decimals should be valid", balance.decimals >= 0 && balance.decimals <= 9)
            assertTrue("UI amount should be non-negative", balance.uiAmount >= 0.0)
        }
        // If null, that's also acceptable (account doesn't exist)
    }

    @Test
    fun testCreateToken() = runBlocking {
        val mintAuthority = testPublicKey
        val freezeAuthority = SolanaPrivateKey.generate().publicKey()

        // Test token creation
        val (mintAddress, signature) = tokenManager.createToken(
            payer = testPrivateKey,
            mintAuthority = mintAuthority,
            decimals = 6,
            freezeAuthority = freezeAuthority
        )

        assertNotNull("Mint address should not be null", mintAddress)
        assertNotNull("Transaction signature should not be null", signature)
        assertTrue("Signature should not be empty", signature.isNotEmpty())

        // Test token creation without freeze authority
        val (mintAddress2, signature2) = tokenManager.createToken(
            payer = testPrivateKey,
            mintAuthority = mintAuthority,
            decimals = 9,
            freezeAuthority = null
        )

        assertNotNull("Mint address should not be null", mintAddress2)
        assertNotNull("Transaction signature should not be null", signature2)
        assertTrue("Signature should not be empty", signature2.isNotEmpty())
        assertNotEquals("Mint addresses should be different", mintAddress, mintAddress2)
    }

    @Test
    fun testMintTokens() = runBlocking {
        val destination = testPublicKey
        val amount = 1000000L

        val signature = tokenManager.mintTokens(
            mintAuthority = testPrivateKey,
            mint = mint,
            destination = destination,
            amount = amount
        )

        assertNotNull("Transaction signature should not be null", signature)
        assertTrue("Signature should not be empty", signature.isNotEmpty())
    }

    @Test
    fun testBurnTokens() = runBlocking {
        val amount = 500000L

        val signature = tokenManager.burnTokens(
            owner = testPrivateKey,
            mint = mint,
            amount = amount
        )

        assertNotNull("Transaction signature should not be null", signature)
        assertTrue("Signature should not be empty", signature.isNotEmpty())
    }

    @Test
    fun testApproveTokens() = runBlocking {
        val delegate = SolanaPrivateKey.generate().publicKey()
        val amount = 1000000L

        val signature = tokenManager.approveTokens(
            owner = testPrivateKey,
            mint = mint,
            delegate = delegate,
            amount = amount
        )

        assertNotNull("Transaction signature should not be null", signature)
        assertTrue("Signature should not be empty", signature.isNotEmpty())
    }

    @Test
    fun testRevokeTokenApproval() = runBlocking {
        val signature = tokenManager.revokeTokenApproval(
            owner = testPrivateKey,
            mint = mint
        )

        assertNotNull("Transaction signature should not be null", signature)
        assertTrue("Signature should not be empty", signature.isNotEmpty())
    }

    @Test
    fun testCloseTokenAccount() = runBlocking {
        // Test with default destination (owner)
        val signature1 = tokenManager.closeTokenAccount(
            owner = testPrivateKey,
            mint = mint
        )

        assertNotNull("Transaction signature should not be null", signature1)
        assertTrue("Signature should not be empty", signature1.isNotEmpty())

        // Test with custom destination
        val destination = SolanaPrivateKey.generate().publicKey()
        val signature2 = tokenManager.closeTokenAccount(
            owner = testPrivateKey,
            mint = mint,
            destination = destination
        )

        assertNotNull("Transaction signature should not be null", signature2)
        assertTrue("Signature should not be empty", signature2.isNotEmpty())
    }

    @Test
    fun testAmountConversions() {
        // Test UI amount to raw amount conversion
        val uiAmount = 1.5
        val decimals = 6
        val expectedRawAmount = 1500000L

        val rawAmount = tokenManager.uiAmountToRawAmount(uiAmount, decimals)
        assertEquals("UI to raw conversion should be correct", expectedRawAmount, rawAmount)

        // Test raw amount to UI amount conversion
        val convertedUiAmount = tokenManager.rawAmountToUiAmount(rawAmount, decimals)
        assertEquals("Raw to UI conversion should be correct", uiAmount, convertedUiAmount, 0.000001)

        // Test with 9 decimals (SOL)
        val solUiAmount = 0.000000001 // 1 lamport
        val solDecimals = 9
        val expectedSolRawAmount = 1L

        val solRawAmount = tokenManager.uiAmountToRawAmount(solUiAmount, solDecimals)
        assertEquals("SOL UI to raw conversion", expectedSolRawAmount, solRawAmount)

        val convertedSolUiAmount = tokenManager.rawAmountToUiAmount(solRawAmount, solDecimals)
        assertEquals("SOL raw to UI conversion", solUiAmount, convertedSolUiAmount, 0.000000001)

        // Test with 0 decimals
        val noDecimalUiAmount = 42.0
        val noDecimalDecimals = 0
        val expectedNoDecimalRawAmount = 42L

        val noDecimalRawAmount = tokenManager.uiAmountToRawAmount(noDecimalUiAmount, noDecimalDecimals)
        assertEquals("No decimal UI to raw conversion", expectedNoDecimalRawAmount, noDecimalRawAmount)

        val convertedNoDecimalUiAmount = tokenManager.rawAmountToUiAmount(noDecimalRawAmount, noDecimalDecimals)
        assertEquals("No decimal raw to UI conversion", noDecimalUiAmount, convertedNoDecimalUiAmount, 0.0)

        // Test with large amounts
        val largeUiAmount = 1000000.123456
        val largeRawAmount = tokenManager.uiAmountToRawAmount(largeUiAmount, 6)
        val convertedLargeUiAmount = tokenManager.rawAmountToUiAmount(largeRawAmount, 6)
        assertEquals("Large amount round-trip", largeUiAmount, convertedLargeUiAmount, 0.000001)

        // Test edge cases
        assertEquals("Zero UI amount", 0L, tokenManager.uiAmountToRawAmount(0.0, 6))
        assertEquals("Zero raw amount", 0.0, tokenManager.rawAmountToUiAmount(0L, 6), 0.0)
    }

    @Test
    fun testWellKnownTokens() {
        // Test that well-known token addresses are valid
        // Test using constants from the companion object
        val usdcAddress = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
        val usdtAddress = "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB"
        val solWrappedAddress = "So11111111111111111111111111111111111111112"

        assertTrue("USDC address should be valid",
                  SolanaSDK.isValidAddress(usdcAddress))
        assertTrue("USDT address should be valid",
                  SolanaSDK.isValidAddress(usdtAddress))
        assertTrue("Wrapped SOL address should be valid",
                  SolanaSDK.isValidAddress(solWrappedAddress))

        // Test that addresses are different
        assertNotEquals("USDC and USDT should be different", usdcAddress, usdtAddress)
        assertNotEquals("USDC and wrapped SOL should be different", usdcAddress, solWrappedAddress)
        assertNotEquals("USDT and wrapped SOL should be different", usdtAddress, solWrappedAddress)
    }

    @Test
    fun testConversionEdgeCases() {
        // Test maximum decimals
        val maxDecimals = 9
        val smallAmount = 0.000000001
        val rawAmount = tokenManager.uiAmountToRawAmount(smallAmount, maxDecimals)
        assertEquals("Smallest unit conversion", 1L, rawAmount)

        // Test precision limits
        val precisionTestAmount = 1.123456789
        val precisionRawAmount = tokenManager.uiAmountToRawAmount(precisionTestAmount, 9)
        val precisionBackToUi = tokenManager.rawAmountToUiAmount(precisionRawAmount, 9)
        assertEquals("Precision test", precisionTestAmount, precisionBackToUi, 0.000000001)

        // Test with different decimal places
        for (decimals in 0..9) {
            val testAmount = 1.0
            val converted = tokenManager.uiAmountToRawAmount(testAmount, decimals)
            val expectedAmount = Math.pow(10.0, decimals.toDouble()).toLong()
            assertEquals("Decimal test for $decimals decimals", expectedAmount, converted)

            val backConverted = tokenManager.rawAmountToUiAmount(converted, decimals)
            assertEquals("Back conversion for $decimals decimals", testAmount, backConverted, 0.000000001)
        }

        // Test rounding behavior
        val roundingTestAmount = 1.5555555555 // More precision than possible
        val roundingRawAmount = tokenManager.uiAmountToRawAmount(roundingTestAmount, 6)
        val roundingBackToUi = tokenManager.rawAmountToUiAmount(roundingRawAmount, 6)
        // Should be close but may have rounding differences
        assertTrue("Rounding should be reasonable",
                  Math.abs(roundingTestAmount - roundingBackToUi) < 0.000001)
    }

    @Test
    fun testTokenOperationConsistency() = runBlocking {
        // Test that multiple operations on the same token work consistently
        val mint1 = SolanaPrivateKey.generate().publicKey()
        val mint2 = SolanaPrivateKey.generate().publicKey()

        // Create multiple tokens
        val (address1, sig1) = tokenManager.createToken(testPrivateKey, testPublicKey, 6)
        val (address2, sig2) = tokenManager.createToken(testPrivateKey, testPublicKey, 9)

        assertNotEquals("Different tokens should have different addresses", address1, address2)
        assertNotEquals("Different transactions should have different signatures", sig1, sig2)

        // Test operations on different mints
        val mintSig1 = tokenManager.mintTokens(testPrivateKey, mint1, testPublicKey, 1000000L)
        val mintSig2 = tokenManager.mintTokens(testPrivateKey, mint2, testPublicKey, 2000000L)

        assertNotNull("Mint signature 1 should not be null", mintSig1)
        assertNotNull("Mint signature 2 should not be null", mintSig2)
        assertTrue("Mint signature 1 should not be empty", mintSig1.isNotEmpty())
        assertTrue("Mint signature 2 should not be empty", mintSig2.isNotEmpty())
    }
}