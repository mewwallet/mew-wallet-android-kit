package com.myetherwallet.mewwalletkit.eip.eip2930

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
 * Comprehensive tests for EIP2930 transactions (Type 1)
 * Testing the access list functionality for gas optimization
 */
class Eip2930TransactionTest {

    private lateinit var testAddress: Address
    private lateinit var testSignature: TransactionSignature

    @Before
    fun setup() {
        testAddress = Address("0x1234567890123456789012345678901234567890", Network.ETHEREUM)
        testSignature = TransactionSignature(
            BigInteger.valueOf(27),
            BigInteger("0x1234567890abcdef", 16),
            BigInteger("0xabcdef1234567890", 16)
        )
    }

    @Test
    fun testBasicEip2930TransactionCreation() {
        val transaction = Eip2930Transaction(
            nonce = BigInteger.valueOf(42),
            gasPrice = BigInteger.valueOf(20000000000), // 20 gwei
            gasLimit = BigInteger.valueOf(21000),
            to = testAddress,
            value = BigInteger.valueOf(1000000000000000000), // 1 ETH
            data = "0x".hexToByteArray(),
            accessList = emptyArray(),
            chainId = BigInteger.valueOf(1) // Mainnet
        )

        assertNotNull("Transaction should not be null", transaction)
        assertEquals("Nonce should be 42", BigInteger.valueOf(42), transaction.nonce)
        assertEquals("Gas price should be 20 gwei", BigInteger.valueOf(20000000000), transaction.gasPrice)
        assertEquals("Gas limit should be 21000", BigInteger.valueOf(21000), transaction.gasLimit)
        assertEquals("Value should be 1 ETH", BigInteger.valueOf(1000000000000000000), transaction.value)
        assertEquals("Chain ID should be 1", BigInteger.valueOf(1), transaction.chainId)
        assertEquals("To address should match", testAddress, transaction.to)
        assertNotNull("Access list should not be null", transaction.accessList)
        assertEquals("Access list should be empty", 0, transaction.accessList?.size)
    }

    @Test
    fun testEip2930TransactionWithAccessList() {
        val storageSlots = arrayOf(
            "0x0000000000000000000000000000000000000000000000000000000000000001".hexToByteArray(),
            "0x0000000000000000000000000000000000000000000000000000000000000002".hexToByteArray()
        )

        val accessList = arrayOf(
            AccessList(
                address = Address("0x1234567890123456789012345678901234567890", Network.ETHEREUM),
                slots = storageSlots
            ),
            AccessList(
                address = Address("0xabcdefabcdefabcdefabcdefabcdefabcdefabcd", Network.ETHEREUM),
                slots = arrayOf("0x0000000000000000000000000000000000000000000000000000000000000003".hexToByteArray())
            )
        )

        val transaction = Eip2930Transaction(
            nonce = BigInteger.valueOf(100),
            gasPrice = BigInteger.valueOf(15000000000), // 15 gwei
            gasLimit = BigInteger.valueOf(50000),
            to = testAddress,
            value = BigInteger.ZERO,
            data = "0xa9059cbb".hexToByteArray(), // transfer function selector
            accessList = accessList,
            chainId = BigInteger.valueOf(1)
        )

        assertNotNull("Transaction with access list should not be null", transaction)
        assertEquals("Access list should have 2 items", 2, transaction.accessList?.size)
        assertEquals("First access list item should have 2 storage slots",
                    2, transaction.accessList?.get(0)?.slots?.size)
        assertEquals("Second access list item should have 1 storage slot",
                    1, transaction.accessList?.get(1)?.slots?.size)
    }

    @Test
    fun testRlpEncodingUnsignedTransaction() {
        val transaction = Eip2930Transaction(
            nonce = BigInteger.valueOf(9),
            gasPrice = BigInteger.valueOf(10000000000), // 10 gwei
            gasLimit = BigInteger.valueOf(21000),
            to = Address("0x3535353535353535353535353535353535353535", Network.ETHEREUM),
            value = BigInteger.valueOf(1000000000000000000), // 1 ETH
            data = "0x".hexToByteArray(),
            accessList = emptyArray(),
            chainId = BigInteger.valueOf(1)
        )

        val encoded = transaction.rlpEncode()
        assertNotNull("RLP encoding should not be null", encoded)
        assertTrue("RLP encoding should not be empty", encoded.isNotEmpty())

        // EIP-2930 transactions should start with type byte 0x01
        assertEquals("First byte should be transaction type 1", 0x01.toByte(), encoded[0])
    }

    @Test
    fun testRlpEncodingSignedTransaction() {
        val transaction = Eip2930Transaction(
            nonce = BigInteger.valueOf(9),
            gasPrice = BigInteger.valueOf(10000000000),
            gasLimit = BigInteger.valueOf(21000),
            to = Address("0x3535353535353535353535353535353535353535", Network.ETHEREUM),
            value = BigInteger.valueOf(1000000000000000000),
            data = "0x".hexToByteArray(),
            accessList = emptyArray(),
            chainId = BigInteger.valueOf(1),
            signature = testSignature
        )

        val encoded = transaction.rlpEncode()
        assertNotNull("Signed RLP encoding should not be null", encoded)
        assertTrue("Signed RLP encoding should not be empty", encoded.isNotEmpty())
        assertEquals("First byte should be transaction type 1", 0x01.toByte(), encoded[0])
    }

    @Test
    fun testTransactionHash() {
        val transaction = Eip2930Transaction(
            nonce = BigInteger.valueOf(0),
            gasPrice = BigInteger.valueOf(10000000000),
            gasLimit = BigInteger.valueOf(21000),
            to = Address("0x3535353535353535353535353535353535353535", Network.ETHEREUM),
            value = BigInteger.valueOf(1000000000000000000),
            data = "0x".hexToByteArray(),
            accessList = emptyArray(),
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
        val transaction = Eip2930Transaction(
            nonce = BigInteger.valueOf(5),
            gasPrice = BigInteger.valueOf(15000000000),
            gasLimit = BigInteger.valueOf(25000),
            to = testAddress,
            value = BigInteger.valueOf(500000000000000000), // 0.5 ETH
            data = "0x".hexToByteArray(),
            accessList = emptyArray(),
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
    fun testAccessListCreation() {
        val storageSlots = arrayOf(
            "0x0000000000000000000000000000000000000000000000000000000000000001".hexToByteArray(),
            "0x0000000000000000000000000000000000000000000000000000000000000002".hexToByteArray()
        )

        val accessListItem = AccessList(
            address = Address("0x1234567890123456789012345678901234567890", Network.ETHEREUM),
            slots = storageSlots
        )

        assertEquals("Address should match",
                    "0x1234567890123456789012345678901234567890",
                    accessListItem.address?.address)
        assertEquals("Storage slots should match", storageSlots, accessListItem.slots)
        assertEquals("Should have 2 storage slots", 2, accessListItem.slots?.size)
    }

    @Test
    fun testEmptyAccessList() {
        val transaction = Eip2930Transaction(
            nonce = BigInteger.valueOf(1),
            gasPrice = BigInteger.valueOf(10000000000),
            gasLimit = BigInteger.valueOf(21000),
            to = testAddress,
            value = BigInteger.ZERO,
            data = "0x".hexToByteArray(),
            accessList = emptyArray(),
            chainId = BigInteger.valueOf(1)
        )

        assertNotNull("Access list should not be null", transaction.accessList)
        assertTrue("Access list should be empty", transaction.accessList?.isEmpty() ?: false)

        val encoded = transaction.rlpEncode()
        assertNotNull("Transaction with empty access list should encode", encoded)
    }

    @Test
    fun testNullAccessList() {
        val transaction = Eip2930Transaction(
            nonce = BigInteger.valueOf(1),
            gasPrice = BigInteger.valueOf(10000000000),
            gasLimit = BigInteger.valueOf(21000),
            to = testAddress,
            value = BigInteger.ZERO,
            data = "0x".hexToByteArray(),
            accessList = null,
            chainId = BigInteger.valueOf(1)
        )

        assertNull("Access list should be null", transaction.accessList)

        val encoded = transaction.rlpEncode()
        assertNotNull("Transaction with null access list should encode", encoded)
    }

    @Test
    fun testContractDeploymentTransaction() {
        val contractBytecode = "0x608060405234801561001057600080fd5b50600080fd5b50".hexToByteArray()

        val transaction = Eip2930Transaction(
            nonce = BigInteger.valueOf(0),
            gasPrice = BigInteger.valueOf(25000000000), // 25 gwei
            gasLimit = BigInteger.valueOf(500000), // Higher gas for contract deployment
            to = null, // null address for contract deployment
            value = BigInteger.ZERO,
            data = contractBytecode,
            accessList = emptyArray(),
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

        val transaction = Eip2930Transaction(
            nonce = BigInteger.valueOf(10),
            gasPrice = BigInteger.valueOf(50000000000), // 50 gwei
            gasLimit = BigInteger.valueOf(21000),
            to = testAddress,
            value = largeValue,
            data = "0x".hexToByteArray(),
            accessList = emptyArray(),
            chainId = BigInteger.valueOf(1)
        )

        assertEquals("Large value should be preserved", largeValue, transaction.value)

        val encoded = transaction.rlpEncode()
        assertNotNull("Large value transaction should encode", encoded)
    }

    @Test
    fun testHighNonceTransaction() {
        val highNonce = BigInteger("999999999999")

        val transaction = Eip2930Transaction(
            nonce = highNonce,
            gasPrice = BigInteger.valueOf(10000000000),
            gasLimit = BigInteger.valueOf(21000),
            to = testAddress,
            value = BigInteger.ZERO,
            data = "0x".hexToByteArray(),
            accessList = emptyArray(),
            chainId = BigInteger.valueOf(1)
        )

        assertEquals("High nonce should be preserved", highNonce, transaction.nonce)

        val encoded = transaction.rlpEncode()
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
            val transaction = Eip2930Transaction(
                nonce = BigInteger.valueOf(1),
                gasPrice = BigInteger.valueOf(10000000000),
                gasLimit = BigInteger.valueOf(21000),
                to = testAddress,
                value = BigInteger.ZERO,
                data = "0x".hexToByteArray(),
                accessList = emptyArray(),
                chainId = chainId
            )

            assertEquals("Chain ID should be preserved for $chainId", chainId, transaction.chainId)

            val encoded = transaction.rlpEncode()
            assertNotNull("Transaction should encode for chain ID $chainId", encoded)
        }
    }

    @Test
    fun testTransactionSize() {
        val transaction = Eip2930Transaction(
            nonce = BigInteger.valueOf(1),
            gasPrice = BigInteger.valueOf(10000000000),
            gasLimit = BigInteger.valueOf(21000),
            to = testAddress,
            value = BigInteger.valueOf(1000000000000000000),
            data = "0x".hexToByteArray(),
            accessList = emptyArray(),
            chainId = BigInteger.valueOf(1)
        )

        val encoded = transaction.rlpEncode()
        assertNotNull("Encoded transaction should not be null", encoded)

        // EIP-2930 transactions should be reasonably sized
        assertTrue("Transaction should be under 1KB for simple transfers", encoded.size < 1024)
        assertTrue("Transaction should be at least 50 bytes", encoded.size >= 50)
    }

    @Test
    fun testZeroValues() {
        val transaction = Eip2930Transaction(
            nonce = BigInteger.ZERO,
            gasPrice = BigInteger.ZERO,
            gasLimit = BigInteger.valueOf(21000), // Still need gas limit > 0
            to = testAddress,
            value = BigInteger.ZERO,
            data = "0x".hexToByteArray(),
            accessList = emptyArray(),
            chainId = BigInteger.valueOf(1)
        )

        assertEquals("Zero nonce should be allowed", BigInteger.ZERO, transaction.nonce)
        assertEquals("Zero gas price should be allowed", BigInteger.ZERO, transaction.gasPrice)
        assertEquals("Zero value should be allowed", BigInteger.ZERO, transaction.value)

        val encoded = transaction.rlpEncode()
        assertNotNull("Zero value transaction should encode", encoded)
    }

    @Test
    fun testStringConstructor() {
        val transaction = Eip2930Transaction(
            nonce = "0x2a", // 42 in hex
            gasPrice = "0x4a817c800", // 20 gwei in hex
            gasLimit = "0x5208", // 21000 in hex
            to = testAddress,
            value = "0xde0b6b3a7640000", // 1 ETH in hex
            data = "0x".hexToByteArray(),
            accessList = emptyArray(),
            chainId = BigInteger.valueOf(1).toByteArray()
        )

        assertEquals("Nonce should be 42", BigInteger.valueOf(42), transaction.nonce)
        assertEquals("Gas price should be 20 gwei", BigInteger.valueOf(20000000000), transaction.gasPrice)
        assertEquals("Gas limit should be 21000", BigInteger.valueOf(21000), transaction.gasLimit)
        assertEquals("Value should be 1 ETH", BigInteger.valueOf(1000000000000000000), transaction.value)
    }

    @Test
    fun testByteArrayConstructor() {
        val transaction = Eip2930Transaction(
            nonce = byteArrayOf(0x2a), // 42
            gasPrice = byteArrayOf(0x04, 0xa8.toByte(), 0x17, 0xc8.toByte(), 0x00), // 20 gwei
            gasLimit = byteArrayOf(0x52, 0x08), // 21000
            to = testAddress,
            value = byteArrayOf(0x0d, 0xe0.toByte(), 0xb6.toByte(), 0xb3.toByte(), 0xa7.toByte(), 0x64, 0x00, 0x00), // 1 ETH
            data = "0x".hexToByteArray(),
            accessList = emptyArray(),
            chainId = BigInteger.valueOf(1).toByteArray()
        )

        assertEquals("Nonce should be 42", BigInteger.valueOf(42), transaction.nonce)
        assertEquals("Gas price should be 20 gwei", BigInteger.valueOf(20000000000), transaction.gasPrice)
        assertEquals("Gas limit should be 21000", BigInteger.valueOf(21000), transaction.gasLimit)
        assertEquals("Value should be 1 ETH", BigInteger.valueOf(1000000000000000000), transaction.value)
    }

    @Test
    fun testAccessListRlpEncoding() {
        val storageSlot = "0x0000000000000000000000000000000000000000000000000000000000000001".hexToByteArray()
        val accessListItem = AccessList(
            address = Address("0x1234567890123456789012345678901234567890", Network.ETHEREUM),
            slots = arrayOf(storageSlot)
        )

        val encoded = accessListItem.rlpEncode()
        assertNotNull("Access list item should encode", encoded)
        assertTrue("Encoded access list should not be empty", encoded?.isNotEmpty() ?: false)
    }

    @Test
    fun testEmptyAccessListItem() {
        val emptyAccessListItem = AccessList.EMPTY

        assertNull("Empty access list item should have null address", emptyAccessListItem.address)
        assertNull("Empty access list item should have null slots", emptyAccessListItem.slots)

        val encoded = emptyAccessListItem.rlpEncode()
        assertNull("Empty access list item should not encode", encoded)
    }

    @Test
    fun testToStringMethod() {
        val transaction = Eip2930Transaction(
            nonce = BigInteger.valueOf(42),
            gasPrice = BigInteger.valueOf(20000000000),
            gasLimit = BigInteger.valueOf(21000),
            to = testAddress,
            value = BigInteger.valueOf(1000000000000000000),
            data = "0xa9059cbb".hexToByteArray(),
            accessList = emptyArray(),
            chainId = BigInteger.valueOf(1)
        )

        val description = transaction.toString()

        assertTrue("Description should contain EIP type", description.contains("EIP2930"))
        assertTrue("Description should contain nonce", description.contains("Nonce: 2a"))
        assertTrue("Description should contain gas price", description.contains("Gas price:"))
        assertTrue("Description should contain gas limit", description.contains("Gas Limit:"))
        assertTrue("Description should contain to address", description.contains("To:"))
        assertTrue("Description should contain value", description.contains("Value:"))
        assertTrue("Description should contain data", description.contains("Data:"))
        assertTrue("Description should contain chain ID", description.contains("ChainId:"))
        assertTrue("Description should contain access list", description.contains("Access list:"))
    }

    @Test
    fun testComplexAccessList() {
        val complexAccessList = arrayOf(
            AccessList(
                address = Address("0x1111111111111111111111111111111111111111", Network.ETHEREUM),
                slots = arrayOf(
                    "0x0000000000000000000000000000000000000000000000000000000000000001".hexToByteArray(),
                    "0x0000000000000000000000000000000000000000000000000000000000000002".hexToByteArray(),
                    "0x0000000000000000000000000000000000000000000000000000000000000003".hexToByteArray()
                )
            ),
            AccessList(
                address = Address("0x2222222222222222222222222222222222222222", Network.ETHEREUM),
                slots = arrayOf(
                    "0x0000000000000000000000000000000000000000000000000000000000000004".hexToByteArray()
                )
            ),
            AccessList(
                address = Address("0x3333333333333333333333333333333333333333", Network.ETHEREUM),
                slots = emptyArray()
            )
        )

        val transaction = Eip2930Transaction(
            nonce = BigInteger.valueOf(1),
            gasPrice = BigInteger.valueOf(10000000000),
            gasLimit = BigInteger.valueOf(100000),
            to = testAddress,
            value = BigInteger.ZERO,
            data = "0x".hexToByteArray(),
            accessList = complexAccessList,
            chainId = BigInteger.valueOf(1)
        )

        assertEquals("Access list should have 3 items", 3, transaction.accessList?.size)
        assertEquals("First item should have 3 slots", 3, transaction.accessList?.get(0)?.slots?.size)
        assertEquals("Second item should have 1 slot", 1, transaction.accessList?.get(1)?.slots?.size)
        assertEquals("Third item should have 0 slots", 0, transaction.accessList?.get(2)?.slots?.size)

        val encoded = transaction.rlpEncode()
        assertNotNull("Complex access list transaction should encode", encoded)
    }
}