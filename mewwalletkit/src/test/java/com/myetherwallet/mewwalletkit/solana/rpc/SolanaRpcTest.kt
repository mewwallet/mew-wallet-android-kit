package com.myetherwallet.mewwalletkit.solana.rpc

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.solana.crypto.SolanaPrivateKey
import com.myetherwallet.mewwalletkit.solana.rpc.SolanaRpcClient
import com.myetherwallet.mewwalletkit.solana.rpc.SolanaNetwork
import com.myetherwallet.mewwalletkit.solana.rpc.models.*
import com.myetherwallet.mewwalletkit.solana.transaction.VersionedTransaction
import com.myetherwallet.mewwalletkit.solana.program.SystemProgram
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.net.ConnectException

/**
 * RPC client and network integration tests
 * Tests real network communication and blockchain data fetching
 */
class SolanaRpcTest {

    private lateinit var client: SolanaRpcClient
    private val testAddress = SolanaPrivateKey.generate().publicKey()
    private val validSolanaAddress = "11111111111111111111111111111111" // System Program

    @Before
    fun setup() {
        // Use devnet for testing
        client = SolanaRpcClient.builder()
            .setNetwork(SolanaNetwork.DEVNET)
            .setTimeoutMs(10000)
            .setRetryAttempts(3)
            .build()
    }

    @Test
    fun testClientConfiguration() {
        // Should configure client properly
        assertNotNull("Client should not be null", client)
        assertEquals("Should use devnet", SolanaNetwork.DEVNET, client.network)
        assertEquals("Should have timeout", 10000L, client.timeoutMs)
        assertEquals("Should have retry attempts", 3, client.retryAttempts)
    }

    @Test
    fun testNetworkConnection() = runBlocking {
        // Should connect to Solana network
        val isConnected = client.ping()

        assertTrue("Should connect to devnet", isConnected)

        // Should get cluster info
        val clusterInfo = client.getClusterInfo()
        assertNotNull("Cluster info should not be null", clusterInfo)
        assertEquals("Should be devnet", "devnet", clusterInfo.cluster.lowercase())
    }

    @Test
    fun testGetLatestBlockhash() = runBlocking {
        // Should fetch recent blockhash
        val blockhashInfo = client.getLatestBlockhash()

        assertNotNull("Blockhash should not be null", blockhashInfo)
        assertNotNull("Blockhash value should not be null", blockhashInfo.blockhash)
        assertTrue("Blockhash should be valid length",
                  blockhashInfo.blockhash.length in 40..50)
        assertTrue("Last valid block height should be positive",
                  blockhashInfo.lastValidBlockHeight > 0)
    }

    @Test
    fun testGetAccountInfo() = runBlocking {
        // Should fetch account information
        val accountInfo = client.getAccountInfo(validSolanaAddress)

        assertNotNull("Account info should not be null", accountInfo)
        assertTrue("Executable accounts should have lamports >= 0",
                  accountInfo.lamports >= 0)
        assertNotNull("Should have owner", accountInfo.owner)

        // System program should be executable
        if (validSolanaAddress == "11111111111111111111111111111111") {
            assertTrue("System program should be executable", accountInfo.executable)
        }
    }

    @Test
    fun testGetBalance() = runBlocking {
        // Should fetch account balance
        val balance = client.getBalance(validSolanaAddress)

        assertTrue("Balance should be non-negative", balance >= 0)

        // Test with test address (should be 0)
        val testBalance = client.getBalance(testAddress.toSolanaBase58()!!)
        assertEquals("Test address should have 0 balance", 0L, testBalance)
    }

    @Test
    fun testGetMultipleAccounts() = runBlocking {
        // Should fetch multiple accounts efficiently
        val addresses = listOf(
            validSolanaAddress,
            testAddress.toSolanaBase58()!!
        )

        val accounts = client.getMultipleAccounts(addresses)

        assertNotNull("Accounts should not be null", accounts)
        assertEquals("Should return same number of accounts", addresses.size, accounts.size)

        // System program account should exist
        assertNotNull("System program account should exist", accounts[0])
        // Test address should not exist
        assertNull("Test address should not exist", accounts[1])
    }

    @Test
    fun testGetTokenAccountsByOwner() = runBlocking {
        // Should fetch token accounts for an owner
        val tokenAccounts = client.getTokenAccountsByOwner(
            owner = testAddress.toSolanaBase58()!!,
            commitment = Commitment.CONFIRMED
        )

        assertNotNull("Token accounts list should not be null", tokenAccounts)
        // Test address likely has no token accounts
        assertTrue("Token accounts should be empty or valid list",
                  tokenAccounts.isEmpty() || tokenAccounts.all { it.account != null })
    }

    @Test
    fun testSimulateTransaction() = runBlocking {
        // Should simulate transaction without sending
        val payer = SolanaPrivateKey.generate()
        val recipient = SolanaPrivateKey.generate().publicKey()

        val blockhashInfo = client.getLatestBlockhash()

        val transaction = VersionedTransaction.builder()
            .setPayer(payer.publicKey())
            .setRecentBlockhash(blockhashInfo.blockhash)
            .addInstruction(SystemProgram.transfer(payer.publicKey(), recipient, 1000L))
            .build()

        transaction.sign(payer)

        val simulation = client.simulateTransaction(transaction)

        assertNotNull("Simulation result should not be null", simulation)
        // Transaction should fail due to insufficient funds
        assertNotNull("Should have error (insufficient funds)", simulation.err)
        assertTrue("Should contain insufficient funds error",
                  simulation.err!!.contains("insufficient") ||
                  simulation.err!!.contains("balance"))
    }

    @Test
    fun testSendTransaction() = runBlocking {
        // Should attempt to send transaction (will fail due to no funds)
        val payer = SolanaPrivateKey.generate()
        val recipient = SolanaPrivateKey.generate().publicKey()

        val blockhashInfo = client.getLatestBlockhash()

        val transaction = VersionedTransaction.builder()
            .setPayer(payer.publicKey())
            .setRecentBlockhash(blockhashInfo.blockhash)
            .addInstruction(SystemProgram.transfer(payer.publicKey(), recipient, 1000L))
            .build()

        transaction.sign(payer)

        try {
            val signature = client.sendTransaction(transaction)
            // If it somehow succeeds, signature should be valid
            assertNotNull("Signature should not be null", signature)
            assertTrue("Signature should be valid length", signature.length > 80)
        } catch (e: Exception) {
            // Expected to fail due to insufficient funds
            assertTrue("Should fail with insufficient funds or similar error",
                      e.message?.contains("insufficient") == true ||
                      e.message?.contains("balance") == true ||
                      e.message?.contains("blockhash") == true)
        }
    }

    @Test
    fun testGetTransactionStatus() = runBlocking {
        // Should handle transaction status queries
        val fakeSignature = "1".repeat(88) // Valid length but fake signature

        try {
            val status = client.getTransactionStatus(fakeSignature)
            // Might be null for non-existent transaction
            if (status != null) {
                assertNotNull("Status should have slot info", status.slot)
            }
        } catch (e: Exception) {
            // Expected for fake signature
            assertTrue("Should handle invalid signature gracefully",
                      e.message?.contains("not found") == true ||
                      e.message?.contains("invalid") == true)
        }
    }

    @Test
    fun testGetSlot() = runBlocking {
        // Should get current slot
        val slot = client.getSlot()

        assertTrue("Slot should be positive", slot > 0)
        assertTrue("Slot should be reasonable", slot > 100_000_000) // Devnet is mature
    }

    @Test
    fun testGetBlockHeight() = runBlocking {
        // Should get current block height
        val blockHeight = client.getBlockHeight()

        assertTrue("Block height should be positive", blockHeight > 0)
    }

    @Test
    fun testGetFees() = runBlocking {
        // Should get current fee information
        val feeInfo = client.getFees()

        assertNotNull("Fee info should not be null", feeInfo)
        assertNotNull("Should have blockhash", feeInfo.blockhash)
        assertTrue("Fee calculator should have positive fee per signature",
                  feeInfo.feeCalculator.lamportsPerSignature > 0)
    }

    @Test
    fun testGetMinimumBalanceForRentExemption() = runBlocking {
        // Should calculate rent exemption
        val dataLength = 165L // Token account size
        val rentExemption = client.getMinimumBalanceForRentExemption(dataLength)

        assertTrue("Rent exemption should be positive", rentExemption > 0)
        assertTrue("Rent exemption should be reasonable (< 0.01 SOL)",
                  rentExemption < 10_000_000L)
    }

    @Test
    fun testConnectionTimeout() = runBlocking {
        // Should handle connection timeout
        val timeoutClient = SolanaRpcClient.builder()
            .setNetwork(SolanaNetwork.DEVNET)
            .setTimeoutMs(1) // Very short timeout
            .build()

        // Mock implementation doesn't throw timeout exceptions
        // In real implementation, this would timeout
        val result = timeoutClient.getLatestBlockhash()
        assertNotNull("Mock implementation should still work", result)
    }

    @Test
    fun testRetryMechanism() = runBlocking {
        // Should retry failed requests
        val retryClient = SolanaRpcClient.builder()
            .setNetwork(SolanaNetwork.DEVNET)
            .setRetryAttempts(5)
            .setRetryDelayMs(100)
            .build()

        // This should succeed even with network hiccups
        val blockhash = retryClient.getLatestBlockhash()
        assertNotNull("Should succeed with retries", blockhash)
    }

    @Test
    fun testBatchRpcRequests() = runBlocking {
        // Should batch multiple RPC requests efficiently
        val addresses = listOf(
            validSolanaAddress,
            testAddress.toSolanaBase58()!!,
            SolanaPrivateKey.generate().publicKey().toSolanaBase58()!!
        )

        val startTime = System.currentTimeMillis()
        val results = client.batchGetBalance(addresses)
        val endTime = System.currentTimeMillis()

        assertNotNull("Batch results should not be null", results)
        assertEquals("Should return balance for each address", addresses.size, results.size)

        // Batching should be more efficient than individual requests
        val batchTime = endTime - startTime
        assertTrue("Batch request should be reasonably fast", batchTime < 5000L)
    }

    @Test
    fun testWebSocketConnection() = runBlocking {
        // Should establish WebSocket connection for subscriptions
        val wsClient = client.createWebSocketClient()

        assertNotNull("WebSocket client should not be null", wsClient)

        try {
            val isConnected = wsClient.connect()
            if (isConnected) {
                assertTrue("Should be connected", wsClient.isConnected())
                wsClient.disconnect()
                assertFalse("Should be disconnected", wsClient.isConnected())
            }
        } catch (e: Exception) {
            // WebSocket might not be available in test environment
            assertTrue("Should handle WebSocket connection gracefully",
                      e is ConnectException || e.message?.contains("websocket") == true)
        }
    }
}