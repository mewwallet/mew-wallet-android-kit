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
 * Comprehensive tests for the high-level SolanaSDK
 */
class SolanaSDKTest {

    private lateinit var sdk: SolanaSDK
    private lateinit var testPrivateKey: SolanaPrivateKey
    private lateinit var testPublicKey: PublicKey
    private lateinit var recipient: PublicKey

    @Before
    fun setup() {
        sdk = SolanaSDK.create(SolanaNetwork.DEVNET)
        testPrivateKey = SolanaPrivateKey.generate()
        testPublicKey = testPrivateKey.publicKey()
        recipient = SolanaPrivateKey.generate().publicKey()
    }

    @Test
    fun testSDKCreation() {
        // Test default creation
        val defaultSDK = SolanaSDK.create()
        assertEquals(SolanaNetwork.DEVNET, defaultSDK.network)

        // Test specific network creation
        val mainnetSDK = SolanaSDK.create(SolanaNetwork.MAINNET_BETA)
        assertEquals(SolanaNetwork.MAINNET_BETA, mainnetSDK.network)

        // Test custom RPC client creation
        val customClient = SolanaRpcClient.builder()
            .setNetwork(SolanaNetwork.TESTNET)
            .setTimeoutMs(10000L)
            .build()

        val customSDK = SolanaSDK.create(customClient, SolanaNetwork.TESTNET)
        assertEquals(SolanaNetwork.TESTNET, customSDK.network)
    }

    @Test
    fun testGetBalance() = runBlocking {
        // Test balance with string address
        val address = testPublicKey.toSolanaBase58()!!
        val balance = sdk.getBalance(address)
        assertTrue("Balance should be non-negative", balance >= 0)

        // Test balance with PublicKey
        val balanceFromKey = sdk.getBalance(testPublicKey)
        assertEquals("Should return same balance", balance, balanceFromKey)

        // Test system program address (should exist)
        val systemProgramBalance = sdk.getBalance("11111111111111111111111111111111")
        assertTrue("System program should have positive balance", systemProgramBalance > 0)
    }

    @Test
    fun testTransferSOL() = runBlocking {
        // Test transfer with PublicKey recipient
        val signature = sdk.transferSOL(testPrivateKey, recipient, 1000000L)
        assertNotNull("Signature should not be null", signature)
        assertTrue("Signature should not be empty", signature.isNotEmpty())

        // Test transfer with string address
        val recipientAddress = recipient.toSolanaBase58()!!
        val signature2 = sdk.transferSOL(testPrivateKey, recipientAddress, 500000L)
        assertNotNull("Signature should not be null", signature2)
        assertTrue("Signature should not be empty", signature2.isNotEmpty())
    }

    @Test
    fun testAssociatedTokenAddress() {
        val mint = SolanaPrivateKey.generate().publicKey()

        // Test with PublicKey objects
        val ataAddress = sdk.getAssociatedTokenAddress(testPublicKey, mint)
        assertNotNull("ATA address should not be null", ataAddress)

        // Test with string addresses
        val ownerAddress = testPublicKey.toSolanaBase58()!!
        val mintAddress = mint.toSolanaBase58()!!
        val ataAddressString = sdk.getAssociatedTokenAddress(ownerAddress, mintAddress)
        assertNotNull("ATA address string should not be null", ataAddressString)
        assertTrue("ATA address should not be empty", ataAddressString.isNotEmpty())

        // Test consistency between methods
        assertEquals("ATA addresses should match",
                    ataAddress.toSolanaBase58()!!, ataAddressString)
    }

    @Test
    fun testCreateAssociatedTokenAccount() = runBlocking {
        val mint = SolanaPrivateKey.generate().publicKey()

        val signature = sdk.createAssociatedTokenAccount(testPrivateKey, testPublicKey, mint)
        assertNotNull("Signature should not be null", signature)
        assertTrue("Signature should not be empty", signature.isNotEmpty())
    }

    @Test
    fun testTransferToken() = runBlocking {
        val mint = SolanaPrivateKey.generate().publicKey()

        // Test transfer with PublicKey recipient
        val signature = sdk.transferToken(testPrivateKey, recipient, mint, 1000000L)
        assertNotNull("Signature should not be null", signature)
        assertTrue("Signature should not be empty", signature.isNotEmpty())

        // Test transfer with string addresses
        val recipientAddress = recipient.toSolanaBase58()!!
        val mintAddress = mint.toSolanaBase58()!!
        val signature2 = sdk.transferToken(testPrivateKey, recipientAddress, mintAddress, 500000L)
        assertNotNull("Signature should not be null", signature2)
        assertTrue("Signature should not be empty", signature2.isNotEmpty())
    }

    @Test
    fun testSimulateTransaction() = runBlocking {
        val blockhash = sdk.rpcClient.getLatestBlockhash()
        val transaction = com.myetherwallet.mewwalletkit.solana.transaction.VersionedTransaction.builder()
            .setPayer(testPrivateKey.publicKey())
            .setRecentBlockhash(blockhash.blockhash)
            .addInstruction(
                com.myetherwallet.mewwalletkit.solana.program.SystemProgram.transfer(
                    testPrivateKey.publicKey(), recipient, 1000L
                )
            )
            .build()

        transaction.sign(testPrivateKey)

        val simulation = sdk.simulateTransaction(transaction)
        assertNotNull("Simulation should not be null", simulation)
        // Should likely have an error due to insufficient funds
        assertNotNull("Should have error (insufficient funds)", simulation.err)
    }

    @Test
    fun testGetTransactionStatus() = runBlocking {
        val fakeSignature = "1".repeat(88) // Valid length but fake
        val status = sdk.getTransactionStatus(fakeSignature)
        // Status might be null for non-existent transaction - this is expected
    }

    @Test
    fun testGetNetworkInfo() = runBlocking {
        val networkInfo = sdk.getNetworkInfo()
        assertNotNull("Network info should not be null", networkInfo)
        assertEquals("devnet", networkInfo.cluster.lowercase())
        assertNotNull("Version should not be null", networkInfo.version)
    }

    @Test
    fun testGetMinimumBalanceForRentExemption() = runBlocking {
        val rentExemption = sdk.getMinimumBalanceForRentExemption(165L) // Token account size
        assertTrue("Rent exemption should be positive", rentExemption > 0)
        assertTrue("Rent exemption should be reasonable (< 0.01 SOL)",
                  rentExemption < 10_000_000L)
    }

    @Test
    fun testStaticUtilityMethods() {
        // Test address validation
        assertTrue("Should validate valid address",
                  SolanaSDK.isValidAddress("11111111111111111111111111111111"))
        assertFalse("Should reject invalid address",
                   SolanaSDK.isValidAddress("invalid_address"))
        assertFalse("Should reject empty address",
                   SolanaSDK.isValidAddress(""))

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
    }

    @Test
    fun testEdgeCases() = runBlocking {
        // Test with zero amounts
        val zeroTransfer = sdk.transferSOL(testPrivateKey, recipient, 0L)
        assertNotNull("Zero transfer should work", zeroTransfer)

        // Test with maximum values
        val maxLamports = Long.MAX_VALUE
        val maxSOL = SolanaSDK.lamportsToSOL(maxLamports)
        assertTrue("Max conversion should work", maxSOL > 0)

        // Test address validation edge cases
        assertFalse("Should reject null-like strings",
                   SolanaSDK.isValidAddress("null"))
        assertFalse("Should reject very short strings",
                   SolanaSDK.isValidAddress("abc"))
        assertFalse("Should reject very long strings",
                   SolanaSDK.isValidAddress("a".repeat(100)))

        // Test conversion edge cases
        assertEquals("Zero conversion", 0.0, SolanaSDK.lamportsToSOL(0L), 0.0)
        assertEquals("Zero conversion reverse", 0L, SolanaSDK.solToLamports(0.0))
    }

    @Test
    fun testNetworkConfiguration() {
        val networks = listOf(
            SolanaNetwork.MAINNET_BETA,
            SolanaNetwork.DEVNET,
            SolanaNetwork.TESTNET,
            SolanaNetwork.LOCALHOST
        )

        for (network in networks) {
            val networkSDK = SolanaSDK.create(network)
            assertEquals("SDK should use specified network", network, networkSDK.network)
            assertNotNull("Network should have RPC URL", network.rpcUrl)
            assertNotNull("Network should have WebSocket URL", network.wsUrl)
            assertNotNull("Network should have display name", network.displayName)
        }
    }

    @Test
    fun testConcurrentOperations() = runBlocking {
        // Test multiple concurrent balance queries
        val addresses = (1..5).map { SolanaPrivateKey.generate().publicKey().toSolanaBase58()!! }

        val balances = addresses.map { address ->
            sdk.getBalance(address)
        }

        assertEquals("Should get balance for all addresses", addresses.size, balances.size)
        assertTrue("All balances should be non-negative", balances.all { it >= 0 })
    }

    @Test
    fun testInvalidInputHandling() = runBlocking {
        // Test invalid addresses - these should not crash but may return empty results
        try {
            val invalidBalance = sdk.getBalance("invalid_address_format")
            // If it doesn't throw, balance should be reasonable
            assertTrue("Invalid address balance should be 0", invalidBalance == 0L)
        } catch (e: Exception) {
            // Exceptions are also acceptable for invalid addresses
            assertTrue("Exception should be meaningful", e.message?.isNotEmpty() == true)
        }

        // Test empty addresses
        try {
            val emptyBalance = sdk.getBalance("")
            assertTrue("Empty address balance should be 0", emptyBalance == 0L)
        } catch (e: Exception) {
            // Exceptions are acceptable
            assertTrue("Exception should be meaningful", e.message?.isNotEmpty() == true)
        }
    }
}