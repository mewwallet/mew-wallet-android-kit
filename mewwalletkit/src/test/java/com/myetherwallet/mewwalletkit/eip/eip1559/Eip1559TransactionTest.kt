package com.myetherwallet.mewwalletkit.eip.eip1559

import com.myetherwallet.mewwalletkit.bip.bip44.Address
import com.myetherwallet.mewwalletkit.bip.bip44.Network
import com.myetherwallet.mewwalletkit.core.extension.hexToByteArray
import com.myetherwallet.mewwalletkit.core.extension.toHexString
import com.myetherwallet.mewwalletkit.eip.eip155.TransactionSignature
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.math.BigInteger

/**
 * Comprehensive tests for EIP1559 transactions (Type 2)
 * Testing the new fee market mechanism with priority fees
 */
class Eip1559TransactionTest {

    private lateinit var testAddress: Address
    private lateinit var testSignature: TransactionSignature

    @Before
    fun setup() {
        testAddress = Address("0x1234567890123456789012345678901234567890")
        testSignature = TransactionSignature(
            "1234567890abcdef".hexToByteArray(),
            "abcdef1234567890".hexToByteArray(),
            BigInteger.valueOf(27),
            BigInteger.valueOf(1)
        )
    }

    @Test
    fun testBasicEip1559TransactionCreation() {
        val transaction = Eip1559Transaction(
            nonce = BigInteger.valueOf(42),
            maxPriorityFeePerGas = BigInteger.valueOf(2000000000), // 2 gwei
            maxFeePerGas = BigInteger.valueOf(20000000000), // 20 gwei
            gasLimit = BigInteger.valueOf(21000),
            to = testAddress,
            value = BigInteger.valueOf(1000000000000000000), // 1 ETH
            data = "0x".hexToByteArray(),
            accessList = null,
            chainId = BigInteger.valueOf(1) // Mainnet
        )

        assertNotNull("Transaction should not be null", transaction)
        assertEquals("Nonce should be 42", BigInteger.valueOf(42), transaction.nonce)
        assertEquals("Max priority fee should be 2 gwei", BigInteger.valueOf(2000000000), transaction.maxPriorityFeePerGas)
        assertEquals("Max fee should be 20 gwei", BigInteger.valueOf(20000000000), transaction.maxFeePerGas)
        assertEquals("Gas limit should be 21000", BigInteger.valueOf(21000), transaction.gasLimit)
        assertEquals("Value should be 1 ETH", BigInteger.valueOf(1000000000000000000), transaction.value)
        assertEquals("Chain ID should be 1", BigInteger.valueOf(1), transaction.chainId)
        assertEquals("To address should match", testAddress, transaction.to)
    }

    @Test
    fun testEip1559TransactionWithAccessList() {
        val accessList = arrayOf(
            com.myetherwallet.mewwalletkit.eip.eip2930.AccessList(
                address = Address("0x1234567890123456789012345678901234567890"),
                slots = arrayOf(
                    "0000000000000000000000000000000000000000000000000000000000000001".hexToByteArray(),
                    "0000000000000000000000000000000000000000000000000000000000000002".hexToByteArray()
                )
            ),
            com.myetherwallet.mewwalletkit.eip.eip2930.AccessList(
                address = Address("0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"),
                slots = arrayOf(
                    "0000000000000000000000000000000000000000000000000000000000000003".hexToByteArray()
                )
            )
        )

        val transaction = Eip1559Transaction(
            nonce = BigInteger.valueOf(100),
            maxPriorityFeePerGas = BigInteger.valueOf(1500000000), // 1.5 gwei
            maxFeePerGas = BigInteger.valueOf(30000000000), // 30 gwei
            gasLimit = BigInteger.valueOf(50000),
            to = testAddress,
            value = BigInteger.ZERO,
            data = "0xa9059cbb".hexToByteArray(), // transfer function selector
            accessList = accessList,
            chainId = BigInteger.valueOf(1)
        )

        assertNotNull("Transaction with access list should not be null", transaction)
        assertEquals("Access list should have 2 items", 2, transaction.accessList?.size)
        assertEquals("First access list item should have 2 storage keys",
                    2, transaction.accessList?.get(0)?.slots?.size)
        assertEquals("Second access list item should have 1 storage key",
                    1, transaction.accessList?.get(1)?.slots?.size)
    }

    @Test
    fun testRlpEncodingUnsignedTransaction() {
        val transaction = Eip1559Transaction(
            nonce = BigInteger.valueOf(9),
            maxPriorityFeePerGas = BigInteger.valueOf(1000000000), // 1 gwei
            maxFeePerGas = BigInteger.valueOf(10000000000), // 10 gwei
            gasLimit = BigInteger.valueOf(21000),
            to = Address("0x3535353535353535353535353535353535353535"),
            value = BigInteger.valueOf(1000000000000000000), // 1 ETH
            data = "0x".hexToByteArray(),
            accessList = null,
            chainId = BigInteger.valueOf(1)
        )

        val encoded = transaction.serialize()
        assertNotNull("RLP encoding should not be null", encoded)
        assertTrue("RLP encoding should not be empty", encoded!!.isNotEmpty())

        // EIP-1559 transactions should start with type byte 0x02
        assertEquals("First byte should be transaction type 2", 0x02.toByte(), encoded[0])
    }

    @Test
    fun testRlpEncodingSignedTransaction() {
        val transaction = Eip1559Transaction(
            nonce = BigInteger.valueOf(9),
            maxPriorityFeePerGas = BigInteger.valueOf(1000000000),
            maxFeePerGas = BigInteger.valueOf(10000000000),
            gasLimit = BigInteger.valueOf(21000),
            to = Address("0x3535353535353535353535353535353535353535"),
            value = BigInteger.valueOf(1000000000000000000),
            data = "0x".hexToByteArray(),
            accessList = null,
            chainId = BigInteger.valueOf(1),
            signature = testSignature
        )

        val encoded = transaction.serialize()
        assertNotNull("Signed RLP encoding should not be null", encoded)
        assertTrue("Signed RLP encoding should not be empty", encoded!!.isNotEmpty())
        assertEquals("First byte should be transaction type 2", 0x02.toByte(), encoded[0])
    }

    @Test
    fun testFeeValidation() {
        // Test valid fee structure: maxFeePerGas >= maxPriorityFeePerGas
        val validTransaction = Eip1559Transaction(
            nonce = BigInteger.valueOf(1),
            maxPriorityFeePerGas = BigInteger.valueOf(2000000000), // 2 gwei
            maxFeePerGas = BigInteger.valueOf(20000000000), // 20 gwei
            gasLimit = BigInteger.valueOf(21000),
            to = testAddress,
            value = BigInteger.ZERO,
            data = "0x".hexToByteArray(),
            accessList = null,
            chainId = BigInteger.valueOf(1)
        )

        assertNotNull("Valid fee transaction should be created", validTransaction)
        assertTrue("Max fee should be >= max priority fee",
                  validTransaction.maxFeePerGas >= validTransaction.maxPriorityFeePerGas)
    }

    @Test
    fun testTransactionHash() {
        val transaction = Eip1559Transaction(
            nonce = BigInteger.valueOf(0),
            maxPriorityFeePerGas = BigInteger.valueOf(1000000000),
            maxFeePerGas = BigInteger.valueOf(10000000000),
            gasLimit = BigInteger.valueOf(21000),
            to = Address("0x3535353535353535353535353535353535353535"),
            value = BigInteger.valueOf(1000000000000000000),
            data = "0x".hexToByteArray(),
            accessList = null,
            chainId = BigInteger.valueOf(1)
        )

        val hash1 = transaction.hash()
        val hash2 = transaction.hash()

        assertNotNull("Transaction hash should not be null", hash1)
        assertEquals("Hash should be consistent", hash1?.toHexString(), hash2?.toHexString())
        assertEquals("Hash should be 32 bytes", 32, hash1?.size)
    }

    @Test
    fun testTransactionSigning() {
        val transaction = Eip1559Transaction(
            nonce = BigInteger.valueOf(5),
            maxPriorityFeePerGas = BigInteger.valueOf(1500000000),
            maxFeePerGas = BigInteger.valueOf(15000000000),
            gasLimit = BigInteger.valueOf(25000),
            to = testAddress,
            value = BigInteger.valueOf(500000000000000000), // 0.5 ETH
            data = "0x".hexToByteArray(),
            accessList = null,
            chainId = BigInteger.valueOf(1)
        )

        // Test unsigned transaction
        assertNull("Unsigned transaction should have no signature", transaction.signature)

        // Add signature
        transaction.signature = testSignature

        assertNotNull("Signed transaction should have signature", transaction.signature)
        assertEquals("Signature v should match", testSignature.v, transaction.signature?.v)
        assertEquals("Signature r should match", testSignature.r, transaction.signature?.r)
        assertEquals("Signature s should match", testSignature.s, transaction.signature?.s)
    }

    @Test
    fun testAccessListItemCreation() {
        val storageSlots = arrayOf(
            "0000000000000000000000000000000000000000000000000000000000000001".hexToByteArray(),
            "0000000000000000000000000000000000000000000000000000000000000002".hexToByteArray()
        )

        val accessListItem = com.myetherwallet.mewwalletkit.eip.eip2930.AccessList(
            address = Address("0x1234567890123456789012345678901234567890"),
            slots = storageSlots
        )

        assertEquals("Address should match", "0x1234567890123456789012345678901234567890", accessListItem.address?.address)
        assertEquals("Should have 2 storage keys", 2, accessListItem.slots?.size)
    }

    @Test
    fun testEmptyAccessList() {
        val transaction = Eip1559Transaction(
            nonce = BigInteger.valueOf(1),
            maxPriorityFeePerGas = BigInteger.valueOf(1000000000),
            maxFeePerGas = BigInteger.valueOf(10000000000),
            gasLimit = BigInteger.valueOf(21000),
            to = testAddress,
            value = BigInteger.ZERO,
            data = "0x".hexToByteArray(),
            accessList = emptyArray(),
            chainId = BigInteger.valueOf(1)
        )

        assertTrue("Access list should be empty", transaction.accessList?.isEmpty() == true)

        val encoded = transaction.serialize()
        assertNotNull("Transaction with empty access list should encode", encoded)
    }

    @Test
    fun testContractDeploymentTransaction() {
        val contractBytecode = "0x608060405234801561001057600080fd5b50600080fd5b50".hexToByteArray()

        val transaction = Eip1559Transaction(
            nonce = BigInteger.valueOf(0),
            maxPriorityFeePerGas = BigInteger.valueOf(2000000000),
            maxFeePerGas = BigInteger.valueOf(25000000000),
            gasLimit = BigInteger.valueOf(500000), // Higher gas for contract deployment
            to = null, // null address for contract deployment
            value = BigInteger.ZERO,
            data = contractBytecode,
            accessList = null,
            chainId = BigInteger.valueOf(1)
        )

        assertNull("Contract deployment should have null to address", transaction.to)
        assertTrue("Contract deployment should have non-empty data", transaction.data.isNotEmpty())
        assertEquals("Bytecode should match", contractBytecode.toHexString(), transaction.data.toHexString())
    }

    @Test
    fun testLargeValueTransaction() {
        // Test with very large ETH value (1000 ETH)
        val largeValue = BigInteger("1000000000000000000000") // 1000 ETH in wei

        val transaction = Eip1559Transaction(
            nonce = BigInteger.valueOf(10),
            maxPriorityFeePerGas = BigInteger.valueOf(5000000000), // 5 gwei
            maxFeePerGas = BigInteger.valueOf(50000000000), // 50 gwei
            gasLimit = BigInteger.valueOf(21000),
            to = testAddress,
            value = largeValue,
            data = "0x".hexToByteArray(),
            accessList = null,
            chainId = BigInteger.valueOf(1)
        )

        assertEquals("Large value should be preserved", largeValue, transaction.value)

        val encoded = transaction.serialize()
        assertNotNull("Large value transaction should encode", encoded)
    }

    @Test
    fun testHighNonceTransaction() {
        val highNonce = BigInteger("999999999999")

        val transaction = Eip1559Transaction(
            nonce = highNonce,
            maxPriorityFeePerGas = BigInteger.valueOf(1000000000),
            maxFeePerGas = BigInteger.valueOf(10000000000),
            gasLimit = BigInteger.valueOf(21000),
            to = testAddress,
            value = BigInteger.ZERO,
            data = "0x".hexToByteArray(),
            accessList = null,
            chainId = BigInteger.valueOf(1)
        )

        assertEquals("High nonce should be preserved", highNonce, transaction.nonce)

        val encoded = transaction.serialize()
        assertNotNull("High nonce transaction should encode", encoded)
    }

    @Test
    fun testDifferentChainIds() {
        val chainIds = listOf(
            BigInteger.valueOf(1),    // Mainnet
            BigInteger.valueOf(3),    // Ropsten
            BigInteger.valueOf(4),    // Rinkeby
            BigInteger.valueOf(5),    // Goerli
            BigInteger.valueOf(137),  // Polygon
            BigInteger.valueOf(42161) // Arbitrum
        )

        chainIds.forEach { chainId ->
            val transaction = Eip1559Transaction(
                nonce = BigInteger.valueOf(1),
                maxPriorityFeePerGas = BigInteger.valueOf(1000000000),
                maxFeePerGas = BigInteger.valueOf(10000000000),
                gasLimit = BigInteger.valueOf(21000),
                to = testAddress,
                value = BigInteger.ZERO,
                data = "0x".hexToByteArray(),
                accessList = null,
                chainId = chainId
            )

            assertEquals("Chain ID should be preserved for $chainId", chainId, transaction.chainId)

            val encoded = transaction.serialize()
            assertNotNull("Transaction should encode for chain ID $chainId", encoded)
        }
    }

    @Test
    fun testTransactionSize() {
        val transaction = Eip1559Transaction(
            nonce = BigInteger.valueOf(1),
            maxPriorityFeePerGas = BigInteger.valueOf(1000000000),
            maxFeePerGas = BigInteger.valueOf(10000000000),
            gasLimit = BigInteger.valueOf(21000),
            to = testAddress,
            value = BigInteger.valueOf(1000000000000000000),
            data = "0x".hexToByteArray(),
            accessList = null,
            chainId = BigInteger.valueOf(1)
        )

        val encoded = transaction.serialize()
        assertNotNull("Encoded transaction should not be null", encoded)

        // EIP-1559 transactions should be reasonably sized
        assertTrue("Transaction should be under 1KB for simple transfers", encoded!!.size < 1024)
        assertTrue("Transaction should be at least 50 bytes", encoded.size >= 50)
    }

    @Test
    fun testZeroValues() {
        val transaction = Eip1559Transaction(
            nonce = BigInteger.ZERO,
            maxPriorityFeePerGas = BigInteger.ZERO,
            maxFeePerGas = BigInteger.ZERO,
            gasLimit = BigInteger.valueOf(21000), // Still need gas limit > 0
            to = testAddress,
            value = BigInteger.ZERO,
            data = "0x".hexToByteArray(),
            accessList = null,
            chainId = BigInteger.valueOf(1)
        )

        assertEquals("Zero nonce should be allowed", BigInteger.ZERO, transaction.nonce)
        assertEquals("Zero priority fee should be allowed", BigInteger.ZERO, transaction.maxPriorityFeePerGas)
        assertEquals("Zero max fee should be allowed", BigInteger.ZERO, transaction.maxFeePerGas)
        assertEquals("Zero value should be allowed", BigInteger.ZERO, transaction.value)

        val encoded = transaction.serialize()
        assertNotNull("Zero value transaction should encode", encoded)
    }
}