package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.core.extension.toHexString
import com.myetherwallet.mewwalletkit.solana.serialization.MessageSerializer
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for Solana v0 (versioned) transaction messages with Address Lookup Table support.
 */
class MessageV0Test {

    @Test
    fun `test TransactionVersion fromByte - Legacy`() {
        val version = TransactionVersion.fromByte(0x01)
        assertEquals("Should detect Legacy version", TransactionVersion.LEGACY, version)
    }

    @Test
    fun `test TransactionVersion fromByte - V0`() {
        val version = TransactionVersion.fromByte(0x80.toByte())
        assertEquals("Should detect V0 version", TransactionVersion.V0, version)
    }

    @Test
    fun `test TransactionVersion fromByte - unsupported`() {
        try {
            TransactionVersion.fromByte(0x81.toByte())
            fail("Should throw UnsupportedOperationException for version 1")
        } catch (e: UnsupportedOperationException) {
            assertTrue("Error message should mention version", e.message!!.contains("version"))
        }
    }

    @Test
    fun `test MessageV0 creation without ALTs`() {
        val publicKeys = TestHelpers.createTestPublicKeys(3)
        val blockhash = TestHelpers.createTestBlockhash()

        val message = MessageV0(
            header = MessageHeader(
                numRequiredSignatures = 1u,
                numReadonlySignedAccounts = 0u,
                numReadonlyUnsignedAccounts = 1u
            ),
            staticAccountKeys = publicKeys,
            recentBlockhash = blockhash,
            compiledInstructions = listOf(
                CompiledInstruction(
                    programIdIndex = 2u,
                    accounts = listOf(0u, 1u),
                    data = byteArrayOf(0x02, 0x00, 0x00, 0x00, 0x40, 0x42, 0x0f, 0x00, 0x00, 0x00, 0x00, 0x00)
                )
            )
        )

        assertEquals("Version should be V0", TransactionVersion.V0, message.version)
        assertEquals("Should have 3 static account keys", 3, message.staticAccountKeys.size)
        assertEquals("Should have 0 accounts from lookups", 0, message.numAccountKeysFromLookups)
        assertEquals("Should have 3 total account keys", 3, message.numAccountKeys)
        assertEquals("Should have 1 instruction", 1, message.compiledInstructions.size)
    }

    @Test
    fun `test MessageV0 isAccountSigner`() {
        val publicKeys = TestHelpers.createTestPublicKeys(3)
        val blockhash = TestHelpers.createTestBlockhash()

        val message = MessageV0(
            header = MessageHeader(
                numRequiredSignatures = 2u, // First 2 accounts are signers
                numReadonlySignedAccounts = 0u,
                numReadonlyUnsignedAccounts = 1u
            ),
            staticAccountKeys = publicKeys,
            recentBlockhash = blockhash,
            compiledInstructions = emptyList()
        )

        assertTrue("Account 0 should be a signer", message.isAccountSigner(0))
        assertTrue("Account 1 should be a signer", message.isAccountSigner(1))
        assertFalse("Account 2 should not be a signer", message.isAccountSigner(2))
    }

    @Test
    fun `test MessageV0 isAccountWritable - static accounts`() {
        val publicKeys = TestHelpers.createTestPublicKeys(4)
        val blockhash = TestHelpers.createTestBlockhash()

        val message = MessageV0(
            header = MessageHeader(
                numRequiredSignatures = 2u,
                numReadonlySignedAccounts = 1u,    // Account 1 is readonly signer
                numReadonlyUnsignedAccounts = 1u   // Account 3 is readonly unsigned
            ),
            staticAccountKeys = publicKeys,
            recentBlockhash = blockhash,
            compiledInstructions = emptyList()
        )

        // Signed accounts: 0 (writable), 1 (readonly)
        assertTrue("Account 0 should be writable", message.isAccountWritable(0))
        assertFalse("Account 1 should be readonly", message.isAccountWritable(1))

        // Unsigned accounts: 2 (writable), 3 (readonly)
        assertTrue("Account 2 should be writable", message.isAccountWritable(2))
        assertFalse("Account 3 should be readonly", message.isAccountWritable(3))
    }

    @Test
    fun `test MessageV0 with Address Lookup Tables`() {
        val staticKeys = TestHelpers.createTestPublicKeys(2)
        val altKey = TestHelpers.createTestPublicKeys(3)[2] // Use 3rd test key as ALT account key
        val blockhash = TestHelpers.createTestBlockhash()

        val message = MessageV0(
            header = MessageHeader(
                numRequiredSignatures = 1u,
                numReadonlySignedAccounts = 0u,
                numReadonlyUnsignedAccounts = 0u
            ),
            staticAccountKeys = staticKeys,
            recentBlockhash = blockhash,
            compiledInstructions = emptyList(),
            addressTableLookups = listOf(
                MessageAddressTableLookup(
                    accountKey = altKey,
                    writableIndexes = listOf(0u, 1u),
                    readonlyIndexes = listOf(2u, 3u)
                )
            )
        )

        assertEquals("Should have 2 static account keys", 2, message.staticAccountKeys.size)
        assertEquals("Should have 4 accounts from lookups", 4, message.numAccountKeysFromLookups)
        assertEquals("Should have 6 total account keys", 6, message.numAccountKeys)
    }

    @Test
    fun `test MessageV0 resolveAddressTableLookups`() {
        val staticKeys = TestHelpers.createTestPublicKeys(2)
        val altKey = TestHelpers.createTestPublicKeys(3)[2]
        val blockhash = TestHelpers.createTestBlockhash()

        // Create ALT with 5 addresses
        val altAddresses = TestHelpers.createTestPublicKeys(8).drop(3) // Use keys 3-7
        val altAccount = AddressLookupTableAccount(
            key = altKey,
            state = AddressLookupTableState(
                deactivationSlot = ULong.MAX_VALUE,
                lastExtendedSlot = 100u,
                lastExtendedSlotStartIndex = 0u,
                authority = staticKeys[0],
                addresses = altAddresses
            )
        )

        val message = MessageV0(
            header = MessageHeader(
                numRequiredSignatures = 1u,
                numReadonlySignedAccounts = 0u,
                numReadonlyUnsignedAccounts = 0u
            ),
            staticAccountKeys = staticKeys,
            recentBlockhash = blockhash,
            compiledInstructions = emptyList(),
            addressTableLookups = listOf(
                MessageAddressTableLookup(
                    accountKey = altKey,
                    writableIndexes = listOf(0u, 1u),
                    readonlyIndexes = listOf(2u, 3u)
                )
            )
        )

        val resolved = message.resolveAddressTableLookups(listOf(altAccount))

        assertEquals("Should have 2 writable accounts", 2, resolved.writable.size)
        assertEquals("Should have 2 readonly accounts", 2, resolved.readonly.size)
        assertEquals("Writable account 0 should match", altAddresses[0], resolved.writable[0])
        assertEquals("Writable account 1 should match", altAddresses[1], resolved.writable[1])
        assertEquals("Readonly account 0 should match", altAddresses[2], resolved.readonly[0])
        assertEquals("Readonly account 1 should match", altAddresses[3], resolved.readonly[1])
    }

    @Test
    fun `test MessageV0 resolveAddressTableLookups - missing ALT`() {
        val staticKeys = TestHelpers.createTestPublicKeys(2)
        val altKey = TestHelpers.createTestPublicKeys(3)[2]
        val blockhash = TestHelpers.createTestBlockhash()

        val message = MessageV0(
            header = MessageHeader(
                numRequiredSignatures = 1u,
                numReadonlySignedAccounts = 0u,
                numReadonlyUnsignedAccounts = 0u
            ),
            staticAccountKeys = staticKeys,
            recentBlockhash = blockhash,
            compiledInstructions = emptyList(),
            addressTableLookups = listOf(
                MessageAddressTableLookup(
                    accountKey = altKey,
                    writableIndexes = listOf(0u),
                    readonlyIndexes = emptyList()
                )
            )
        )

        try {
            message.resolveAddressTableLookups(emptyList())
            fail("Should throw IllegalArgumentException when ALT is missing")
        } catch (e: IllegalArgumentException) {
            assertTrue("Error message should mention lookup table", e.message!!.contains("lookup table"))
        }
    }

    @Test
    fun `test MessageV0 resolveAddressTableLookups - invalid index`() {
        val staticKeys = TestHelpers.createTestPublicKeys(2)
        val altKey = TestHelpers.createTestPublicKeys(3)[2]
        val blockhash = TestHelpers.createTestBlockhash()

        // ALT with only 2 addresses
        val altAddresses = TestHelpers.createTestPublicKeys(5).drop(3).take(2)
        val altAccount = AddressLookupTableAccount(
            key = altKey,
            state = AddressLookupTableState(
                deactivationSlot = ULong.MAX_VALUE,
                lastExtendedSlot = 100u,
                lastExtendedSlotStartIndex = 0u,
                authority = null,
                addresses = altAddresses
            )
        )

        val message = MessageV0(
            header = MessageHeader(
                numRequiredSignatures = 1u,
                numReadonlySignedAccounts = 0u,
                numReadonlyUnsignedAccounts = 0u
            ),
            staticAccountKeys = staticKeys,
            recentBlockhash = blockhash,
            compiledInstructions = emptyList(),
            addressTableLookups = listOf(
                MessageAddressTableLookup(
                    accountKey = altKey,
                    writableIndexes = listOf(5u), // Invalid: ALT only has 2 addresses
                    readonlyIndexes = emptyList()
                )
            )
        )

        try {
            message.resolveAddressTableLookups(listOf(altAccount))
            fail("Should throw IllegalArgumentException for invalid index")
        } catch (e: IllegalArgumentException) {
            assertTrue("Error message should mention index", e.message!!.contains("index"))
        }
    }

    @Test
    fun `test MessageV0 getAllAccountKeys`() {
        val staticKeys = TestHelpers.createTestPublicKeys(2)
        val altKey = TestHelpers.createTestPublicKeys(3)[2]
        val blockhash = TestHelpers.createTestBlockhash()

        val altAddresses = TestHelpers.createTestPublicKeys(6).drop(3).take(3)
        val altAccount = AddressLookupTableAccount(
            key = altKey,
            state = AddressLookupTableState(
                deactivationSlot = ULong.MAX_VALUE,
                lastExtendedSlot = 100u,
                lastExtendedSlotStartIndex = 0u,
                authority = null,
                addresses = altAddresses
            )
        )

        val message = MessageV0(
            header = MessageHeader(
                numRequiredSignatures = 1u,
                numReadonlySignedAccounts = 0u,
                numReadonlyUnsignedAccounts = 0u
            ),
            staticAccountKeys = staticKeys,
            recentBlockhash = blockhash,
            compiledInstructions = emptyList(),
            addressTableLookups = listOf(
                MessageAddressTableLookup(
                    accountKey = altKey,
                    writableIndexes = listOf(0u),
                    readonlyIndexes = listOf(1u, 2u)
                )
            )
        )

        val allKeys = message.getAllAccountKeys(listOf(altAccount))

        assertEquals("Should have 5 total keys", 5, allKeys.size)
        assertEquals("First should be static key 0", staticKeys[0], allKeys[0])
        assertEquals("Second should be static key 1", staticKeys[1], allKeys[1])
        assertEquals("Third should be writable ALT key", altAddresses[0], allKeys[2])
        assertEquals("Fourth should be readonly ALT key 0", altAddresses[1], allKeys[3])
        assertEquals("Fifth should be readonly ALT key 1", altAddresses[2], allKeys[4])
    }

    @Test
    fun `test MessageV0 isAccountWritable - with ALT accounts`() {
        val staticKeys = TestHelpers.createTestPublicKeys(2)
        val altKey = TestHelpers.createTestPublicKeys(3)[2]
        val blockhash = TestHelpers.createTestBlockhash()

        val message = MessageV0(
            header = MessageHeader(
                numRequiredSignatures = 1u,
                numReadonlySignedAccounts = 0u,
                numReadonlyUnsignedAccounts = 0u
            ),
            staticAccountKeys = staticKeys,
            recentBlockhash = blockhash,
            compiledInstructions = emptyList(),
            addressTableLookups = listOf(
                MessageAddressTableLookup(
                    accountKey = altKey,
                    writableIndexes = listOf(0u, 1u),
                    readonlyIndexes = listOf(2u, 3u)
                )
            )
        )

        // Static accounts: 0 (writable signer), 1 (writable unsigned)
        assertTrue("Static account 0 should be writable", message.isAccountWritable(0))
        assertTrue("Static account 1 should be writable", message.isAccountWritable(1))

        // ALT accounts: 2-3 (writable), 4-5 (readonly)
        assertTrue("ALT account 2 should be writable", message.isAccountWritable(2))
        assertTrue("ALT account 3 should be writable", message.isAccountWritable(3))
        assertFalse("ALT account 4 should be readonly", message.isAccountWritable(4))
        assertFalse("ALT account 5 should be readonly", message.isAccountWritable(5))
    }

    @Test
    fun `test MessageV0 serialization`() {
        val publicKeys = TestHelpers.createTestPublicKeys(3)
        val blockhash = TestHelpers.createTestBlockhash()

        val message = MessageV0(
            header = MessageHeader(
                numRequiredSignatures = 1u,
                numReadonlySignedAccounts = 0u,
                numReadonlyUnsignedAccounts = 1u
            ),
            staticAccountKeys = publicKeys,
            recentBlockhash = blockhash,
            compiledInstructions = listOf(
                CompiledInstruction(
                    programIdIndex = 2u,
                    accounts = listOf(0u, 1u),
                    data = byteArrayOf(0x02, 0x00, 0x00, 0x00)
                )
            )
        )

        val serialized = MessageSerializer.serializeMessageV0(message)

        // Check version prefix
        assertEquals("First byte should be 0x80", 0x80.toByte(), serialized[0])

        // Check header (bytes 1-3)
        assertEquals("Byte 1 should be numRequiredSignatures", 1, serialized[1].toInt())
        assertEquals("Byte 2 should be numReadonlySignedAccounts", 0, serialized[2].toInt())
        assertEquals("Byte 3 should be numReadonlyUnsignedAccounts", 1, serialized[3].toInt())

        // Verify serialization is complete
        assertTrue("Serialized message should be non-empty", serialized.isNotEmpty())
        assertTrue("Serialized message should have reasonable size", serialized.size > 100)
    }

    @Test
    fun `test MessageV0 serialization with ALTs`() {
        val staticKeys = TestHelpers.createTestPublicKeys(2)
        val altKey = TestHelpers.createTestPublicKeys(3)[2]
        val blockhash = TestHelpers.createTestBlockhash()

        val message = MessageV0(
            header = MessageHeader(
                numRequiredSignatures = 1u,
                numReadonlySignedAccounts = 0u,
                numReadonlyUnsignedAccounts = 0u
            ),
            staticAccountKeys = staticKeys,
            recentBlockhash = blockhash,
            compiledInstructions = emptyList(),
            addressTableLookups = listOf(
                MessageAddressTableLookup(
                    accountKey = altKey,
                    writableIndexes = listOf(0u, 1u),
                    readonlyIndexes = listOf(2u)
                )
            )
        )

        val serialized = MessageSerializer.serializeMessageV0(message)

        // Check version prefix
        assertEquals("First byte should be 0x80", 0x80.toByte(), serialized[0])

        // Verify we have a complete serialized message
        assertTrue("Serialized message should be non-empty", serialized.isNotEmpty())
        assertTrue("Should have version + header + keys + blockhash + instructions + ALTs",
            serialized.size > 100)
    }

    @Test
    fun `test VersionedMessage Legacy wrapper`() {
        val publicKeys = TestHelpers.createTestPublicKeys(3)
        val blockhash = TestHelpers.createTestBlockhash()

        val legacyMessage = Message(
            header = MessageHeader(
                numRequiredSignatures = 1u,
                numReadonlySignedAccounts = 0u,
                numReadonlyUnsignedAccounts = 1u
            ),
            accountKeys = publicKeys,
            recentBlockhash = blockhash,
            instructions = emptyList()
        )

        val versionedMessage = VersionedMessage.legacy(legacyMessage)

        assertEquals("Version should be LEGACY", TransactionVersion.LEGACY, versionedMessage.version)
        assertEquals("Header should match", legacyMessage.header, versionedMessage.header)
        assertEquals("Account keys should match", legacyMessage.accountKeys, versionedMessage.staticAccountKeys)
        assertEquals("Blockhash should match", legacyMessage.recentBlockhash, versionedMessage.recentBlockhash)
    }

    @Test
    fun `test VersionedMessage V0 wrapper`() {
        val publicKeys = TestHelpers.createTestPublicKeys(3)
        val blockhash = TestHelpers.createTestBlockhash()

        val v0Message = MessageV0(
            header = MessageHeader(
                numRequiredSignatures = 1u,
                numReadonlySignedAccounts = 0u,
                numReadonlyUnsignedAccounts = 1u
            ),
            staticAccountKeys = publicKeys,
            recentBlockhash = blockhash,
            compiledInstructions = emptyList()
        )

        val versionedMessage = VersionedMessage.v0(v0Message)

        assertEquals("Version should be V0", TransactionVersion.V0, versionedMessage.version)
        assertEquals("Header should match", v0Message.header, versionedMessage.header)
        assertEquals("Static account keys should match", v0Message.staticAccountKeys, versionedMessage.staticAccountKeys)
        assertEquals("Blockhash should match", v0Message.recentBlockhash, versionedMessage.recentBlockhash)
    }

    @Test
    fun `test VersionedMessage serialization - Legacy`() {
        val publicKeys = TestHelpers.createTestPublicKeys(3)
        val blockhash = TestHelpers.createTestBlockhash()

        val legacyMessage = Message(
            header = MessageHeader(
                numRequiredSignatures = 1u,
                numReadonlySignedAccounts = 0u,
                numReadonlyUnsignedAccounts = 1u
            ),
            accountKeys = publicKeys,
            recentBlockhash = blockhash,
            instructions = emptyList()
        )

        val versionedMessage = VersionedMessage.legacy(legacyMessage)
        val serialized = MessageSerializer.serializeVersionedMessage(versionedMessage)

        // Legacy messages don't have version prefix
        assertNotEquals("First byte should not be 0x80 for legacy", 0x80.toByte(), serialized[0])
        assertEquals("First byte should be numRequiredSignatures", 1, serialized[0].toInt())
    }

    @Test
    fun `test VersionedMessage serialization - V0`() {
        val publicKeys = TestHelpers.createTestPublicKeys(3)
        val blockhash = TestHelpers.createTestBlockhash()

        val v0Message = MessageV0(
            header = MessageHeader(
                numRequiredSignatures = 1u,
                numReadonlySignedAccounts = 0u,
                numReadonlyUnsignedAccounts = 1u
            ),
            staticAccountKeys = publicKeys,
            recentBlockhash = blockhash,
            compiledInstructions = emptyList()
        )

        val versionedMessage = VersionedMessage.v0(v0Message)
        val serialized = MessageSerializer.serializeVersionedMessage(versionedMessage)

        // V0 messages have version prefix
        assertEquals("First byte should be 0x80 for v0", 0x80.toByte(), serialized[0])
        assertEquals("Second byte should be numRequiredSignatures", 1, serialized[1].toInt())
    }

    @Test
    fun `test AddressLookupTableAccount isActive`() {
        val altKey = TestHelpers.createTestPublicKeys(1)[0]

        val activeALT = AddressLookupTableAccount(
            key = altKey,
            state = AddressLookupTableState(
                deactivationSlot = ULong.MAX_VALUE,
                lastExtendedSlot = 100u,
                lastExtendedSlotStartIndex = 0u,
                authority = null,
                addresses = emptyList()
            )
        )

        val deactivatedALT = AddressLookupTableAccount(
            key = altKey,
            state = AddressLookupTableState(
                deactivationSlot = 1000u,
                lastExtendedSlot = 100u,
                lastExtendedSlotStartIndex = 0u,
                authority = null,
                addresses = emptyList()
            )
        )

        assertTrue("ALT with MAX_VALUE deactivationSlot should be active", activeALT.isActive)
        assertFalse("ALT with non-MAX_VALUE deactivationSlot should be inactive", deactivatedALT.isActive)
    }

    @Test
    fun `test AddressLookupTableAccount getAddress`() {
        val altKey = TestHelpers.createTestPublicKeys(1)[0]
        val addresses = TestHelpers.createTestPublicKeys(5).drop(1)

        val altAccount = AddressLookupTableAccount(
            key = altKey,
            state = AddressLookupTableState(
                deactivationSlot = ULong.MAX_VALUE,
                lastExtendedSlot = 100u,
                lastExtendedSlotStartIndex = 0u,
                authority = null,
                addresses = addresses
            )
        )

        assertEquals("Index 0 should return first address", addresses[0], altAccount.getAddress(0u))
        assertEquals("Index 3 should return fourth address", addresses[3], altAccount.getAddress(3u))
    }

    @Test
    fun `test AddressLookupTableAccount getAddress - out of bounds`() {
        val altKey = TestHelpers.createTestPublicKeys(1)[0]
        val addresses = TestHelpers.createTestPublicKeys(3).drop(1)

        val altAccount = AddressLookupTableAccount(
            key = altKey,
            state = AddressLookupTableState(
                deactivationSlot = ULong.MAX_VALUE,
                lastExtendedSlot = 100u,
                lastExtendedSlotStartIndex = 0u,
                authority = null,
                addresses = addresses
            )
        )

        try {
            altAccount.getAddress(10u)
            fail("Should throw IllegalArgumentException for out of bounds index")
        } catch (e: IllegalArgumentException) {
            assertTrue("Error message should mention index", e.message!!.contains("Index"))
        }
    }
}
