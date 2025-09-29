package com.myetherwallet.mewwalletkit.integration

import com.myetherwallet.mewwalletkit.bip.bip39.BIP39
import com.myetherwallet.mewwalletkit.bip.bip44.Address
import com.myetherwallet.mewwalletkit.bip.bip44.Network
import com.myetherwallet.mewwalletkit.bip.bip44.PrivateKey
import com.myetherwallet.mewwalletkit.core.extension.*
import com.myetherwallet.mewwalletkit.eip.eip155.LegacyTransaction
import com.myetherwallet.mewwalletkit.eip.eip1559.Eip1559Transaction
import com.myetherwallet.mewwalletkit.eip.eip2930.Eip2930Transaction
import com.myetherwallet.mewwalletkit.eip.eip712.Eip712JsonParser
import org.junit.Test
import org.junit.Assert.*
import java.math.BigInteger

/**
 * Cross-module integration tests to verify component interactions
 * Tests real-world workflows that span multiple modules
 */
class CrossModuleIntegrationTest {

    @Test
    fun `wallet creation to transaction signing workflow`() {
        // 1. Generate mnemonic using BIP39
        val mnemonic = BIP39.generateMnemonic()
        assertNotNull("Mnemonic should be generated", mnemonic)
        assertTrue("Mnemonic should be valid", BIP39.validateMnemonic(mnemonic))

        // 2. Generate seed from mnemonic
        val seed = BIP39.mnemonicToSeed(mnemonic)
        assertNotNull("Seed should be generated", seed)
        assertEquals("Seed should be 64 bytes", 64, seed.size)

        // 3. Create private key from seed using BIP44
        val privateKey = PrivateKey(seed, Network.ETHEREUM)
        assertNotNull("Private key should be created", privateKey)

        // 4. Generate address from private key
        val address = privateKey.address
        assertNotNull("Address should be generated", address)
        assertTrue("Address should be valid Ethereum address",
                  address.address.length == 42 && address.address.startsWith("0x"))

        // 5. Create and sign a legacy transaction
        val legacyTx = LegacyTransaction(
            nonce = BigInteger.ONE,
            gasPrice = BigInteger.valueOf(20000000000),
            gasLimit = BigInteger.valueOf(21000),
            to = Address("0x742d35cc6bf4532c623ca39fd3ad35a3a3e1e3d3", Network.ETHEREUM),
            value = BigInteger.valueOf(1000000000000000000),
            data = "0x".hexToByteArray(),
            chainId = BigInteger.ONE
        )

        val signedLegacyTx = privateKey.signTransaction(legacyTx)
        assertNotNull("Legacy transaction should be signed", signedLegacyTx.signature)

        // 6. Create and sign EIP1559 transaction
        val eip1559Tx = Eip1559Transaction(
            nonce = BigInteger.valueOf(2),
            maxPriorityFeePerGas = BigInteger.valueOf(2000000000),
            maxFeePerGas = BigInteger.valueOf(20000000000),
            gasLimit = BigInteger.valueOf(21000),
            to = Address("0x742d35cc6bf4532c623ca39fd3ad35a3a3e1e3d3", Network.ETHEREUM),
            value = BigInteger.valueOf(500000000000000000),
            data = "0x".hexToByteArray(),
            chainId = BigInteger.ONE
        )

        val signedEip1559Tx = privateKey.signTransaction(eip1559Tx)
        assertNotNull("EIP1559 transaction should be signed", signedEip1559Tx.signature)

        // 7. Verify transaction hashes are different
        val legacyHash = signedLegacyTx.hash()
        val eip1559Hash = signedEip1559Tx.hash()
        assertNotNull("Legacy hash should exist", legacyHash)
        assertNotNull("EIP1559 hash should exist", eip1559Hash)
        assertFalse("Transaction hashes should be different",
                   legacyHash?.contentEquals(eip1559Hash) ?: false)
    }

    @Test
    fun `EIP712 typed data signing workflow`() {
        // 1. Create private key
        val seed = ByteArray(64) { it.toByte() }
        val privateKey = PrivateKey(seed, Network.ETHEREUM)

        // 2. Create EIP712 typed data JSON
        val typedDataJson = """
        {
            "types": {
                "EIP712Domain": [
                    {"name": "name", "type": "string"},
                    {"name": "version", "type": "string"},
                    {"name": "chainId", "type": "uint256"},
                    {"name": "verifyingContract", "type": "address"}
                ],
                "Person": [
                    {"name": "name", "type": "string"},
                    {"name": "wallet", "type": "address"}
                ],
                "Mail": [
                    {"name": "from", "type": "Person"},
                    {"name": "to", "type": "Person"},
                    {"name": "contents", "type": "string"}
                ]
            },
            "primaryType": "Mail",
            "domain": {
                "name": "Ether Mail",
                "version": "1",
                "chainId": 1,
                "verifyingContract": "0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC"
            },
            "message": {
                "from": {
                    "name": "Cow",
                    "wallet": "0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826"
                },
                "to": {
                    "name": "Bob",
                    "wallet": "0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB"
                },
                "contents": "Hello, Bob!"
            }
        }
        """.trimIndent()

        // 3. Parse typed data using EIP712 parser
        val parser = Eip712JsonParser()
        val typedData = parser.parseJson(typedDataJson)
        assertNotNull("Typed data should be parsed", typedData)

        // 4. Sign typed data
        val signedMessage = privateKey.signMessage(typedData.encodeData().keccak256())
        assertNotNull("Typed data should be signed", signedMessage)
        assertEquals("Signature should be 65 bytes", 65, signedMessage.size)

        // 5. Verify signature components
        assertTrue("V component should be valid", signedMessage[64] == 27.toByte() || signedMessage[64] == 28.toByte())
    }

    @Test
    fun `multi-network address generation`() {
        val seed = ByteArray(64) { (it % 256).toByte() }

        // Test different networks
        val networks = listOf(
            Network.ETHEREUM,
            Network.BITCOIN,
            Network.ETHEREUM_CLASSIC
        )

        val addresses = mutableMapOf<Network, String>()

        for (network in networks) {
            val privateKey = PrivateKey(seed, network)
            val address = privateKey.address.address

            addresses[network] = address
            assertNotNull("Address should be generated for $network", address)

            // Verify address format based on network
            when (network) {
                Network.ETHEREUM, Network.ETHEREUM_CLASSIC -> {
                    assertTrue("Ethereum address should start with 0x", address.startsWith("0x"))
                    assertEquals("Ethereum address should be 42 chars", 42, address.length)
                    assertTrue("Ethereum address should be valid hex", address.removePrefix("0x").isHex())
                }
                Network.BITCOIN -> {
                    assertFalse("Bitcoin address should not start with 0x", address.startsWith("0x"))
                    assertTrue("Bitcoin address should be valid length", address.length >= 26 && address.length <= 35)
                }
            }
        }

        // Verify addresses are different for different networks
        val ethereumAddr = addresses[Network.ETHEREUM]
        val bitcoinAddr = addresses[Network.BITCOIN]
        val etcAddr = addresses[Network.ETHEREUM_CLASSIC]

        assertNotEquals("Ethereum and Bitcoin addresses should differ", ethereumAddr, bitcoinAddr)
        assertNotEquals("Ethereum and ETC addresses should differ", ethereumAddr, etcAddr)
    }

    @Test
    fun `cryptographic function integration`() {
        val testData = "Hello, blockchain world!".toByteArray()

        // 1. Test hash chain: SHA256 -> RIPEMD160
        val sha256Hash = testData.sha256()
        assertEquals("SHA256 should produce 32 bytes", 32, sha256Hash.size)

        val ripemd160Hash = testData.ripemd160() // This calls SHA256 internally then RIPEMD160
        assertEquals("RIPEMD160 should produce 20 bytes", 20, ripemd160Hash.size)

        // 2. Test Keccak256 for Ethereum
        val keccakHash = testData.keccak256()
        assertEquals("Keccak256 should produce 32 bytes", 32, keccakHash.size)

        // 3. Test personal message hashing
        val personalHash = testData.hashPersonalMessage()
        assertEquals("Personal message hash should be 32 bytes", 32, personalHash.size)

        // 4. Verify hashes are different
        assertFalse("SHA256 and Keccak256 should differ", sha256Hash.contentEquals(keccakHash))
        assertFalse("Keccak256 and personal hash should differ", keccakHash.contentEquals(personalHash))

        // 5. Test hex conversion round trip
        val hexString = testData.toHexString()
        val backToBytes = hexString.hexToByteArray()
        assertArrayEquals("Hex conversion should be reversible", testData, backToBytes)

        // 6. Test EIP55 checksum
        val address = keccakHash.copyOfRange(12, 32) // Last 20 bytes as address
        val eip55Address = address.eip55()
        assertNotNull("EIP55 address should be created", eip55Address)
        assertTrue("EIP55 address should be valid hex", eip55Address?.removePrefix("0x")?.isHex() ?: false)
    }

    @Test
    fun `transaction type compatibility`() {
        val privateKey = PrivateKey(ByteArray(64) { it.toByte() }, Network.ETHEREUM)
        val toAddress = Address("0x742d35cc6bf4532c623ca39fd3ad35a3a3e1e3d3", Network.ETHEREUM)

        // Create transactions of different types with same basic parameters
        val baseTxParams = mapOf(
            "nonce" to BigInteger.valueOf(1),
            "gasLimit" to BigInteger.valueOf(21000),
            "to" to toAddress,
            "value" to BigInteger.valueOf(1000000000000000000),
            "data" to "0x".hexToByteArray(),
            "chainId" to BigInteger.ONE
        )

        // 1. Legacy transaction
        val legacyTx = LegacyTransaction(
            nonce = baseTxParams["nonce"] as BigInteger,
            gasPrice = BigInteger.valueOf(20000000000),
            gasLimit = baseTxParams["gasLimit"] as BigInteger,
            to = baseTxParams["to"] as Address,
            value = baseTxParams["value"] as BigInteger,
            data = baseTxParams["data"] as ByteArray,
            chainId = baseTxParams["chainId"] as BigInteger
        )

        // 2. EIP2930 transaction (Type 1)
        val eip2930Tx = Eip2930Transaction(
            nonce = baseTxParams["nonce"] as BigInteger,
            gasPrice = BigInteger.valueOf(20000000000),
            gasLimit = baseTxParams["gasLimit"] as BigInteger,
            to = baseTxParams["to"] as Address,
            value = baseTxParams["value"] as BigInteger,
            data = baseTxParams["data"] as ByteArray,
            accessList = emptyArray(),
            chainId = baseTxParams["chainId"] as BigInteger
        )

        // 3. EIP1559 transaction (Type 2)
        val eip1559Tx = Eip1559Transaction(
            nonce = baseTxParams["nonce"] as BigInteger,
            maxPriorityFeePerGas = BigInteger.valueOf(2000000000),
            maxFeePerGas = BigInteger.valueOf(20000000000),
            gasLimit = baseTxParams["gasLimit"] as BigInteger,
            to = baseTxParams["to"] as Address,
            value = baseTxParams["value"] as BigInteger,
            data = baseTxParams["data"] as ByteArray,
            chainId = baseTxParams["chainId"] as BigInteger
        )

        // Sign all transactions
        val signedLegacy = privateKey.signTransaction(legacyTx)
        val signedEip2930 = privateKey.signTransaction(eip2930Tx)
        val signedEip1559 = privateKey.signTransaction(eip1559Tx)

        // Verify all are signed
        assertNotNull("Legacy transaction should be signed", signedLegacy.signature)
        assertNotNull("EIP2930 transaction should be signed", signedEip2930.signature)
        assertNotNull("EIP1559 transaction should be signed", signedEip1559.signature)

        // Verify RLP encoding produces different results
        val legacyEncoded = signedLegacy.rlpEncode()
        val eip2930Encoded = signedEip2930.rlpEncode()
        val eip1559Encoded = signedEip1559.rlpEncode()

        assertNotNull("Legacy encoding should exist", legacyEncoded)
        assertNotNull("EIP2930 encoding should exist", eip2930Encoded)
        assertNotNull("EIP1559 encoding should exist", eip1559Encoded)

        // Check transaction type prefixes
        // Legacy transactions don't have type prefix
        // EIP2930 should start with 0x01
        // EIP1559 should start with 0x02
        assertEquals("EIP2930 should have type 1 prefix", 0x01.toByte(), eip2930Encoded[0])
        assertEquals("EIP1559 should have type 2 prefix", 0x02.toByte(), eip1559Encoded[0])

        // Verify hashes are different
        val legacyHash = signedLegacy.hash()
        val eip2930Hash = signedEip2930.hash()
        val eip1559Hash = signedEip1559.hash()

        assertFalse("Legacy and EIP2930 hashes should differ",
                   legacyHash?.contentEquals(eip2930Hash) ?: false)
        assertFalse("EIP2930 and EIP1559 hashes should differ",
                   eip2930Hash?.contentEquals(eip1559Hash) ?: false)
        assertFalse("Legacy and EIP1559 hashes should differ",
                   legacyHash?.contentEquals(eip1559Hash) ?: false)
    }

    @Test
    fun `key derivation and address consistency`() {
        val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val seed = BIP39.mnemonicToSeed(mnemonic)

        // Test multiple account indices
        val accountIndices = listOf(0, 1, 2, 10, 100)
        val addresses = mutableListOf<String>()

        for (accountIndex in accountIndices) {
            val privateKey = PrivateKey(seed, Network.ETHEREUM, accountIndex)
            val address = privateKey.address.address

            addresses.add(address)

            // Verify address is valid
            assertTrue("Address should be valid Ethereum format",
                      address.startsWith("0x") && address.length == 42)
            assertTrue("Address should be valid hex", address.removePrefix("0x").isHex())

            // Test that the same seed and index always produce the same address
            val privateKey2 = PrivateKey(seed, Network.ETHEREUM, accountIndex)
            val address2 = privateKey2.address.address
            assertEquals("Same seed and index should produce same address", address, address2)
        }

        // Verify all addresses are unique
        val uniqueAddresses = addresses.toSet()
        assertEquals("All addresses should be unique", addresses.size, uniqueAddresses.size)

        // Test known vector for account 0 (if available)
        val account0Address = addresses[0]
        assertNotNull("Account 0 address should exist", account0Address)
    }

    @Test
    fun `string and bytearray extension interoperability`() {
        val testHex = "deadbeef"
        val testBytes = byteArrayOf(0xde.toByte(), 0xad.toByte(), 0xbe.toByte(), 0xef.toByte())

        // Test conversion chain: String -> ByteArray -> String
        val hexToBytes = testHex.hexToByteArray()
        assertArrayEquals("Hex string should convert to correct bytes", testBytes, hexToBytes)

        val bytesToHex = testBytes.toHexString()
        assertEquals("Bytes should convert to correct hex string", testHex, bytesToHex)

        // Test with prefix
        val hexWithPrefix = "0x$testHex"
        val prefixToBytes = hexWithPrefix.hexToByteArray()
        assertArrayEquals("Hex with prefix should convert correctly", testBytes, prefixToBytes)

        val bytesToPrefixHex = testBytes.toHexString().addHexPrefix()
        assertEquals("Bytes to hex with prefix should work", hexWithPrefix, bytesToPrefixHex)

        // Test BigInteger conversion
        val bigIntFromHex = testHex.hexToBigInteger()
        val bigIntFromBytes = testBytes.toBigInteger()
        assertEquals("BigInteger from hex and bytes should match", bigIntFromHex, bigIntFromBytes)

        // Test EIP55 consistency
        val addressBytes = "5aaeb6053f3e94c9b9a09f33669435e7ef1beaed".hexToByteArray()
        val eip55FromBytes = addressBytes.eip55()
        val eip55FromString = "5aaeb6053f3e94c9b9a09f33669435e7ef1beaed".eip55()
        assertEquals("EIP55 from bytes and string should match", eip55FromBytes, eip55FromString)

        // Test hash consistency
        val messageString = "test message"
        val messageBytes = messageString.toByteArray()
        val hashFromString = messageString.hashPersonalMessage()
        val hashFromBytes = messageBytes.hashPersonalMessage()
        assertArrayEquals("Hash from string and bytes should match", hashFromString, hashFromBytes)
    }
}