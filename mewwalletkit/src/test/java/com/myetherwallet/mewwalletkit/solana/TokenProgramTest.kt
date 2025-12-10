package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.core.extension.toHexString
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TokenProgramTest {

    @Test
    fun `test TokenProgram program ID is correct`() {
        assertEquals(
            "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
            TokenProgram.PROGRAM_ID
        )
    }

    @Test
    fun `test TokenProgram programId returns valid PublicKey`() {
        val programId = TokenProgram.programId()
        assertNotNull(programId)
        assertEquals(
            "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
            programId.address()?.address
        )
    }

    @Test
    fun `test transfer instruction creates correct accounts`() {
        val keys = TestHelpers.createTestPublicKeys(3)
        val source = keys[0]
        val destination = keys[1]
        val owner = keys[2]
        val amount = 1000000uL

        val instruction = TokenProgram.transfer(
            source = source,
            destination = destination,
            owner = owner,
            amount = amount
        )

        assertEquals(3, instruction.keys.size)

        assertEquals(source, instruction.keys[0].pubkey)
        assertFalse(instruction.keys[0].isSigner)
        assertTrue(instruction.keys[0].isWritable)

        assertEquals(destination, instruction.keys[1].pubkey)
        assertFalse(instruction.keys[1].isSigner)
        assertTrue(instruction.keys[1].isWritable)

        assertEquals(owner, instruction.keys[2].pubkey)
        assertTrue(instruction.keys[2].isSigner)
        assertFalse(instruction.keys[2].isWritable)
    }

    @Test
    fun `test transfer instruction data encoding`() {
        val keys = TestHelpers.createTestPublicKeys(3)
        val amount = 1000000uL

        val instruction = TokenProgram.transfer(
            source = keys[0],
            destination = keys[1],
            owner = keys[2],
            amount = amount
        )

        assertEquals(9, instruction.data.size)

        val buffer = ByteBuffer.wrap(instruction.data).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(3.toByte(), buffer.get())
        assertEquals(amount.toLong(), buffer.getLong())
    }

    @Test
    fun `test transfer instruction has correct program ID`() {
        val keys = TestHelpers.createTestPublicKeys(3)
        val instruction = TokenProgram.transfer(keys[0], keys[1], keys[2], 1000000uL)

        assertEquals(
            "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
            instruction.programId.address()?.address
        )
    }

    @Test
    fun `test AssociatedTokenProgram program ID is correct`() {
        assertEquals(
            "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL",
            AssociatedTokenProgram.PROGRAM_ID
        )
    }

    @Test
    fun `test AssociatedTokenProgram programId returns valid PublicKey`() {
        val programId = AssociatedTokenProgram.programId()
        assertNotNull(programId)
        assertEquals(
            "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL",
            programId.address()?.address
        )
    }

    @Test
    fun `test createAssociatedTokenAccount instruction creates correct accounts`() {
        val keys = TestHelpers.createTestPublicKeys(3)
        val payer = keys[0]
        val owner = keys[1]
        val mint = keys[2]

        val instruction = AssociatedTokenProgram.createAssociatedTokenAccount(
            payer = payer,
            owner = owner,
            mint = mint
        )

        assertEquals(6, instruction.keys.size)

        assertEquals(payer, instruction.keys[0].pubkey)
        assertTrue(instruction.keys[0].isSigner)
        assertTrue(instruction.keys[0].isWritable)

        assertFalse(instruction.keys[1].isSigner)
        assertTrue(instruction.keys[1].isWritable)

        assertEquals(owner, instruction.keys[2].pubkey)
        assertFalse(instruction.keys[2].isSigner)
        assertFalse(instruction.keys[2].isWritable)

        assertEquals(mint, instruction.keys[3].pubkey)
        assertFalse(instruction.keys[3].isSigner)
        assertFalse(instruction.keys[3].isWritable)
    }

    @Test
    fun `test createAssociatedTokenAccount instruction data is empty`() {
        val keys = TestHelpers.createTestPublicKeys(3)
        val instruction = AssociatedTokenProgram.createAssociatedTokenAccount(
            payer = keys[0],
            owner = keys[1],
            mint = keys[2]
        )

        assertEquals(0, instruction.data.size)
    }

    @Test
    fun `test createAssociatedTokenAccount has correct program ID`() {
        val keys = TestHelpers.createTestPublicKeys(3)
        val instruction = AssociatedTokenProgram.createAssociatedTokenAccount(
            payer = keys[0],
            owner = keys[1],
            mint = keys[2]
        )

        assertEquals(
            "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL",
            instruction.programId.address()?.address
        )
    }
}
