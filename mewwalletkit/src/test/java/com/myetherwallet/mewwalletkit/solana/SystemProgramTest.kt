package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.core.extension.toHexString
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for Solana System Program (Phase 4)
 */
class SystemProgramTest {

    @Test
    fun `test SystemProgram program ID is correct`() {
        assertEquals("Program ID should be the system program address",
            "11111111111111111111111111111111",
            SystemProgram.PROGRAM_ID)
    }

    @Test
    fun `test SystemProgram programId returns valid PublicKey`() {
        val programId = SystemProgram.programId()
        assertNotNull("Program ID should parse to PublicKey", programId)
        assertEquals("Program ID address should match",
            "11111111111111111111111111111111",
            programId.address()?.address)
    }

    @Test
    fun `test transfer instruction creates correct accounts`() {
        val keys = TestHelpers.createTestPublicKeys(2)
        val fromPubkey = keys[0]
        val toPubkey = keys[1]
        val lamports = 1000000uL

        val instruction = SystemProgram.transfer(fromPubkey, toPubkey, lamports)

        assertEquals("Should have 2 accounts", 2, instruction.keys.size)

        // Account 0: from (signer, writable)
        assertEquals("First account should be from", fromPubkey, instruction.keys[0].pubkey)
        assertTrue("From should be signer", instruction.keys[0].isSigner)
        assertTrue("From should be writable", instruction.keys[0].isWritable)

        // Account 1: to (writable, not signer)
        assertEquals("Second account should be to", toPubkey, instruction.keys[1].pubkey)
        assertFalse("To should not be signer", instruction.keys[1].isSigner)
        assertTrue("To should be writable", instruction.keys[1].isWritable)
    }

    @Test
    fun `test transfer instruction has correct program ID`() {
        val keys = TestHelpers.createTestPublicKeys(2)
        val instruction = SystemProgram.transfer(keys[0], keys[1], 1000000uL)

        assertEquals("Program ID should be system program",
            "11111111111111111111111111111111",
            instruction.programId.address()?.address)
    }

    @Test
    fun `test transfer instruction data encoding`() {
        val keys = TestHelpers.createTestPublicKeys(2)
        val lamports = 1000000uL // 0.001 SOL

        val instruction = SystemProgram.transfer(keys[0], keys[1], lamports)

        // Instruction data format:
        // Bytes 0-3: instruction index (2 for Transfer) as u32 little-endian
        // Bytes 4-11: lamports as u64 little-endian

        assertEquals("Instruction data should be 12 bytes", 12, instruction.data.size)

        // Check instruction index (2 as u32 little-endian)
        assertEquals("Byte 0 should be 0x02", 0x02, instruction.data[0].toInt() and 0xFF)
        assertEquals("Byte 1 should be 0x00", 0x00, instruction.data[1].toInt() and 0xFF)
        assertEquals("Byte 2 should be 0x00", 0x00, instruction.data[2].toInt() and 0xFF)
        assertEquals("Byte 3 should be 0x00", 0x00, instruction.data[3].toInt() and 0xFF)

        // Check lamports (1000000 as u64 little-endian = 0x000000000000F240)
        // Little-endian: 40 F2 00 00 00 00 00 00
        assertEquals("Byte 4 should be 0x40", 0x40, instruction.data[4].toInt() and 0xFF)
        assertEquals("Byte 5 should be 0x42", 0x42, instruction.data[5].toInt() and 0xFF)
        assertEquals("Byte 6 should be 0x0F", 0x0F, instruction.data[6].toInt() and 0xFF)
        assertEquals("Byte 7 should be 0x00", 0x00, instruction.data[7].toInt() and 0xFF)
    }

    @Test
    fun `test transfer instruction with 1 SOL`() {
        val keys = TestHelpers.createTestPublicKeys(2)
        val lamports = 1000000000uL // 1 SOL

        val instruction = SystemProgram.transfer(keys[0], keys[1], lamports)

        // 1 SOL = 1,000,000,000 = 0x3B9ACA00
        // Little-endian: 00 CA 9A 3B 00 00 00 00
        assertEquals("Byte 4 should be 0x00", 0x00, instruction.data[4].toInt() and 0xFF)
        assertEquals("Byte 5 should be 0xCA", 0xCA, instruction.data[5].toInt() and 0xFF)
        assertEquals("Byte 6 should be 0x9A", 0x9A, instruction.data[6].toInt() and 0xFF)
        assertEquals("Byte 7 should be 0x3B", 0x3B, instruction.data[7].toInt() and 0xFF)
        assertEquals("Byte 8 should be 0x00", 0x00, instruction.data[8].toInt() and 0xFF)
    }

    @Test
    fun `test createAccount instruction creates correct accounts`() {
        val keys = TestHelpers.createTestPublicKeys(2)
        val fromPubkey = keys[0]
        val newAccountPubkey = keys[1]
        val lamports = 1000000uL
        val space = 165uL
        val owner = SystemProgram.programId()

        val instruction = SystemProgram.createAccount(
            fromPubkey, newAccountPubkey, lamports, space, owner
        )

        assertEquals("Should have 2 accounts", 2, instruction.keys.size)

        // Account 0: from (signer, writable)
        assertEquals("First account should be from", fromPubkey, instruction.keys[0].pubkey)
        assertTrue("From should be signer", instruction.keys[0].isSigner)
        assertTrue("From should be writable", instruction.keys[0].isWritable)

        // Account 1: new account (signer, writable)
        assertEquals("Second account should be new account", newAccountPubkey, instruction.keys[1].pubkey)
        assertTrue("New account should be signer", instruction.keys[1].isSigner)
        assertTrue("New account should be writable", instruction.keys[1].isWritable)
    }

    @Test
    fun `test createAccount instruction data encoding`() {
        val keys = TestHelpers.createTestPublicKeys(2)
        val lamports = 1000000uL
        val space = 165uL
        val owner = SystemProgram.programId()

        val instruction = SystemProgram.createAccount(
            keys[0], keys[1], lamports, space, owner
        )

        // Instruction data format:
        // Bytes 0-3: instruction index (0 for CreateAccount) as u32 little-endian
        // Bytes 4-11: lamports as u64 little-endian
        // Bytes 12-19: space as u64 little-endian
        // Bytes 20-51: owner program ID (32 bytes)

        assertEquals("Instruction data should be 52 bytes", 52, instruction.data.size)

        // Check instruction index (0 as u32 little-endian)
        assertEquals("Byte 0 should be 0x00", 0x00, instruction.data[0].toInt() and 0xFF)
        assertEquals("Byte 1 should be 0x00", 0x00, instruction.data[1].toInt() and 0xFF)
        assertEquals("Byte 2 should be 0x00", 0x00, instruction.data[2].toInt() and 0xFF)
        assertEquals("Byte 3 should be 0x00", 0x00, instruction.data[3].toInt() and 0xFF)

        // Check lamports (bytes 4-11)
        assertEquals("Byte 4 should be 0x40", 0x40, instruction.data[4].toInt() and 0xFF)

        // Check space (bytes 12-19)
        // 165 = 0xA5
        assertEquals("Byte 12 should be 0xA5", 0xA5, instruction.data[12].toInt() and 0xFF)
        assertEquals("Byte 13 should be 0x00", 0x00, instruction.data[13].toInt() and 0xFF)

        // Check owner program ID (bytes 20-51, should be 32 bytes)
        val ownerBytes = instruction.data.copyOfRange(20, 52)
        assertArrayEquals("Owner should match", owner.data(), ownerBytes)
    }

    @Test
    fun `test transfer instruction in transaction`() {
        val keys = TestHelpers.createTestPublicKeys(2)
        val fromPubkey = keys[0]
        val toPubkey = keys[1]
        val lamports = 1000000uL

        val transaction = Transaction(
            feePayer = fromPubkey,
            recentBlockhash = "11111111111111111111111111111111" // Dummy blockhash
        )

        transaction.add(SystemProgram.transfer(fromPubkey, toPubkey, lamports))

        assertEquals("Transaction should have 1 instruction", 1, transaction.getInstructions().size)

        val message = transaction.compileMessage()

        assertNotNull("Message should compile", message)
        assertEquals("Message should have 1 instruction", 1, message.instructions.size)
    }

    @Test
    fun `test SystemInstruction transfer directly`() {
        val keys = TestHelpers.createTestPublicKeys(2)
        val instruction = SystemInstruction.transfer(keys[0], keys[1], 5000000000uL)

        assertEquals("Should have correct program ID",
            SystemProgram.PROGRAM_ID,
            instruction.programId.address()?.address)
        assertEquals("Should have 2 accounts", 2, instruction.keys.size)
        assertEquals("Data should be 12 bytes", 12, instruction.data.size)
    }

    @Test
    fun `test SystemInstruction createAccount directly`() {
        val keys = TestHelpers.createTestPublicKeys(2)
        val owner = SystemProgram.programId()
        val instruction = SystemInstruction.createAccount(
            keys[0], keys[1], 1000000uL, 165uL, owner
        )

        assertEquals("Should have correct program ID",
            SystemProgram.PROGRAM_ID,
            instruction.programId.address()?.address)
        assertEquals("Should have 2 accounts", 2, instruction.keys.size)
        assertEquals("Data should be 52 bytes", 52, instruction.data.size)
    }
}
