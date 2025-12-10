package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.core.extension.toHexString
import org.junit.Assert.*
import org.junit.Test

/**
 * ComputeBudgetProgram tests.
 */
class ComputeBudgetProgramTest {

    @Test
    fun `test requestHeapFrame`() {
        // Request 33 KB heap (33 * 1024 = 33792 bytes)
        val bytes = 33u * 1024u
        val instruction = ComputeBudgetProgram.requestHeapFrame(bytes)

        // Verify program ID
        assertEquals(
            "ComputeBudget111111111111111111111111111111",
            instruction.programId.address()?.address
        )

        // Verify no accounts
        assertTrue(instruction.keys.isEmpty())

        // Verify instruction data
        // Format: [opcode: 1] + [bytes: u32 LE]
        // 33792 = 0x8400 in hex = [0x00, 0x84, 0x00, 0x00] in little-endian
        val expectedData = byteArrayOf(
            0x01.toByte(), // opcode = 1 (requestHeapFrame)
            0x00.toByte(), 0x84.toByte(), 0x00.toByte(), 0x00.toByte() // 33792 in little-endian u32
        )
        assertArrayEquals(expectedData, instruction.data)

        // Additional verification: decode the value back
        assertEquals(1, instruction.data[0].toInt())
        val decodedBytes = (instruction.data[1].toInt() and 0xFF) or
                ((instruction.data[2].toInt() and 0xFF) shl 8) or
                ((instruction.data[3].toInt() and 0xFF) shl 16) or
                ((instruction.data[4].toInt() and 0xFF) shl 24)
        assertEquals(bytes.toInt(), decodedBytes)
    }

    @Test
    fun `test setComputeUnitLimit`() {
        // Set compute unit limit to 50,000
        val units = 50_000u
        val instruction = ComputeBudgetProgram.setComputeUnitLimit(units)

        // Verify program ID
        assertEquals(
            "ComputeBudget111111111111111111111111111111",
            instruction.programId.address()?.address
        )

        // Verify no accounts
        assertTrue(instruction.keys.isEmpty())

        // Verify instruction data
        // Format: [opcode: 2] + [units: u32 LE]
        // 50000 = 0xC350 in hex = [0x50, 0xC3, 0x00, 0x00] in little-endian
        val expectedData = byteArrayOf(
            0x02.toByte(), // opcode = 2 (setComputeUnitLimit)
            0x50.toByte(), 0xC3.toByte(), 0x00.toByte(), 0x00.toByte() // 50000 in little-endian u32
        )
        assertArrayEquals(expectedData, instruction.data)

        // Additional verification: decode the value back
        assertEquals(2, instruction.data[0].toInt())
        val decodedUnits = ComputeBudgetProgram.getComputeUnitLimit(instruction.data)
        assertEquals(units, decodedUnits)
    }

    @Test
    fun `test setComputeUnitPrice`() {
        // Set compute unit price to 100,000 micro-lamports
        val microLamports = 100_000uL
        val instruction = ComputeBudgetProgram.setComputeUnitPrice(microLamports)

        // Verify program ID
        assertEquals(
            "ComputeBudget111111111111111111111111111111",
            instruction.programId.address()?.address
        )

        // Verify no accounts
        assertTrue(instruction.keys.isEmpty())

        // Verify instruction data
        // Format: [opcode: 3] + [microLamports: u64 LE]
        // 100000 = 0x186A0 in hex = [0xA0, 0x86, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00] in little-endian
        val expectedData = byteArrayOf(
            0x03.toByte(), // opcode = 3 (setComputeUnitPrice)
            0xA0.toByte(), 0x86.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte() // 100000 in little-endian u64
        )
        assertArrayEquals(expectedData, instruction.data)

        // Additional verification: decode the value back
        assertEquals(3, instruction.data[0].toInt())
        val decodedMicroLamports = ComputeBudgetProgram.getComputeUnitPrice(instruction.data)
        assertEquals(microLamports, decodedMicroLamports)
    }

    @Test
    fun `test requestHeapFrame with different values`() {
        // Test with 128 KB
        val bytes = 128u * 1024u
        val instruction = ComputeBudgetProgram.requestHeapFrame(bytes)

        assertEquals(1, instruction.data[0].toInt())
        assertTrue(instruction.keys.isEmpty())
        assertEquals(5, instruction.data.size) // 1 byte opcode + 4 bytes u32
    }

    @Test
    fun `test setComputeUnitLimit with different values`() {
        // Test with 1,000,000 units
        val units = 1_000_000u
        val instruction = ComputeBudgetProgram.setComputeUnitLimit(units)

        assertEquals(2, instruction.data[0].toInt())
        assertTrue(instruction.keys.isEmpty())
        assertEquals(5, instruction.data.size) // 1 byte opcode + 4 bytes u32
    }

    @Test
    fun `test setComputeUnitPrice with different values`() {
        // Test with 5000 micro-lamports
        val microLamports = 5000uL
        val instruction = ComputeBudgetProgram.setComputeUnitPrice(microLamports)

        assertEquals(3, instruction.data[0].toInt())
        assertTrue(instruction.keys.isEmpty())
        assertEquals(9, instruction.data.size) // 1 byte opcode + 8 bytes u64
    }
}
