package com.myetherwallet.mewwalletkit.solana.token

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.solana.crypto.SolanaPrivateKey
import com.myetherwallet.mewwalletkit.solana.token.TokenProgram
import com.myetherwallet.mewwalletkit.solana.token.AssociatedTokenProgram
import com.myetherwallet.mewwalletkit.solana.instruction.SolanaTransactionInstruction
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive test suite for SPL Token functionality
 * Following TDD approach - tests written first, implementation follows
 */
class SPLTokenTest {

    // Test data setup
    private val owner = SolanaPrivateKey.generate().publicKey()
    private val recipient = SolanaPrivateKey.generate().publicKey()
    private val tokenMint = SolanaPrivateKey.generate().publicKey() // Mock token mint
    private val tokenAccountOwner = owner

    @Test
    fun testCreateAssociatedTokenAccount() {
        // Should create instruction to create Associated Token Account
        val instruction = AssociatedTokenProgram.createAssociatedTokenAccount(
            payer = owner,
            owner = tokenAccountOwner,
            mint = tokenMint
        )

        assertNotNull("Instruction should not be null", instruction)
        assertEquals("Should use Associated Token Program",
                    AssociatedTokenProgram.PROGRAM_ID, instruction.programId)
        assertEquals("Should have 7 accounts (payer, ATA, owner, mint, system, token, rent)",
                    7, instruction.accounts.size)

        // Verify account setup
        assertTrue("Payer should be signer and writable",
                  instruction.accounts[0].isSigner && instruction.accounts[0].isWritable)
        assertTrue("ATA should be writable", instruction.accounts[1].isWritable)
        assertFalse("ATA should not be signer", instruction.accounts[1].isSigner)
    }

    @Test
    fun testGetAssociatedTokenAddress() {
        // Should derive deterministic Associated Token Account address
        val ataAddress1 = AssociatedTokenProgram.getAssociatedTokenAddress(
            owner = tokenAccountOwner,
            mint = tokenMint
        )

        val ataAddress2 = AssociatedTokenProgram.getAssociatedTokenAddress(
            owner = tokenAccountOwner,
            mint = tokenMint
        )

        assertNotNull("ATA address should not be null", ataAddress1)
        assertEquals("ATA derivation should be deterministic", ataAddress1, ataAddress2)
        assertNotEquals("ATA should be different from owner", tokenAccountOwner, ataAddress1)
        assertNotEquals("ATA should be different from mint", tokenMint, ataAddress1)
    }

    @Test
    fun testSPLTokenTransferInstruction() {
        // Should create proper SPL token transfer instruction
        val sourceTokenAccount = AssociatedTokenProgram.getAssociatedTokenAddress(owner, tokenMint)
        val destinationTokenAccount = AssociatedTokenProgram.getAssociatedTokenAddress(recipient, tokenMint)
        val amount = 1_000_000L // 1 token (assuming 6 decimals)

        val instruction = TokenProgram.transfer(
            source = sourceTokenAccount,
            destination = destinationTokenAccount,
            owner = owner,
            amount = amount
        )

        assertNotNull("Transfer instruction should not be null", instruction)
        assertEquals("Should use Token Program", TokenProgram.PROGRAM_ID, instruction.programId)
        assertEquals("Should have 3 accounts (source, destination, owner)",
                    3, instruction.accounts.size)

        // Verify account setup
        assertEquals("Source account should match", sourceTokenAccount, instruction.accounts[0].publicKey)
        assertEquals("Destination account should match", destinationTokenAccount, instruction.accounts[1].publicKey)
        assertEquals("Owner account should match", owner, instruction.accounts[2].publicKey)

        assertTrue("Owner should be signer", instruction.accounts[2].isSigner)
        assertTrue("Source should be writable", instruction.accounts[0].isWritable)
        assertTrue("Destination should be writable", instruction.accounts[1].isWritable)
    }

    @Test
    fun testTokenMintInstruction() {
        // Should create token mint instruction
        val mintAuthority = owner
        val tokenAccount = AssociatedTokenProgram.getAssociatedTokenAddress(recipient, tokenMint)
        val amount = 1_000_000_000L // 1000 tokens

        val instruction = TokenProgram.mintTo(
            mint = tokenMint,
            account = tokenAccount,
            authority = mintAuthority,
            amount = amount
        )

        assertNotNull("Mint instruction should not be null", instruction)
        assertEquals("Should use Token Program", TokenProgram.PROGRAM_ID, instruction.programId)
        assertEquals("Should have 3 accounts (mint, account, authority)",
                    3, instruction.accounts.size)

        assertTrue("Authority should be signer", instruction.accounts[2].isSigner)
        assertTrue("Mint should be writable", instruction.accounts[0].isWritable)
        assertTrue("Account should be writable", instruction.accounts[1].isWritable)
    }

    @Test
    fun testTokenBurnInstruction() {
        // Should create token burn instruction
        val tokenAccount = AssociatedTokenProgram.getAssociatedTokenAddress(owner, tokenMint)
        val amount = 500_000L

        val instruction = TokenProgram.burn(
            account = tokenAccount,
            mint = tokenMint,
            owner = owner,
            amount = amount
        )

        assertNotNull("Burn instruction should not be null", instruction)
        assertEquals("Should use Token Program", TokenProgram.PROGRAM_ID, instruction.programId)
        assertEquals("Should have 3 accounts (account, mint, owner)",
                    3, instruction.accounts.size)

        assertTrue("Owner should be signer", instruction.accounts[2].isSigner)
        assertTrue("Account should be writable", instruction.accounts[0].isWritable)
        assertTrue("Mint should be writable", instruction.accounts[1].isWritable)
    }

    @Test
    fun testTokenAccountInitializationInstruction() {
        // Should create token account initialization instruction
        val newTokenAccount = SolanaPrivateKey.generate().publicKey()

        val instruction = TokenProgram.initializeAccount(
            account = newTokenAccount,
            mint = tokenMint,
            owner = owner
        )

        assertNotNull("Initialize account instruction should not be null", instruction)
        assertEquals("Should use Token Program", TokenProgram.PROGRAM_ID, instruction.programId)
        assertEquals("Should have 4 accounts (account, mint, owner, rent)",
                    4, instruction.accounts.size)

        assertTrue("New account should be writable", instruction.accounts[0].isWritable)
        assertFalse("Mint should not be writable for init", instruction.accounts[1].isWritable)
        assertFalse("Owner should not be signer for init", instruction.accounts[2].isSigner)
    }

    @Test
    fun testCloseTokenAccountInstruction() {
        // Should create instruction to close token account
        val tokenAccount = AssociatedTokenProgram.getAssociatedTokenAddress(owner, tokenMint)
        val destination = recipient // Where remaining SOL goes

        val instruction = TokenProgram.closeAccount(
            account = tokenAccount,
            destination = destination,
            owner = owner
        )

        assertNotNull("Close account instruction should not be null", instruction)
        assertEquals("Should use Token Program", TokenProgram.PROGRAM_ID, instruction.programId)
        assertEquals("Should have 3 accounts (account, destination, owner)",
                    3, instruction.accounts.size)

        assertTrue("Owner should be signer", instruction.accounts[2].isSigner)
        assertTrue("Account should be writable", instruction.accounts[0].isWritable)
        assertTrue("Destination should be writable", instruction.accounts[1].isWritable)
    }

    @Test
    fun testTokenApprovalInstruction() {
        // Should create approval instruction for token spending
        val tokenAccount = AssociatedTokenProgram.getAssociatedTokenAddress(owner, tokenMint)
        val delegate = recipient
        val amount = 1_000_000L

        val instruction = TokenProgram.approve(
            source = tokenAccount,
            delegate = delegate,
            owner = owner,
            amount = amount
        )

        assertNotNull("Approve instruction should not be null", instruction)
        assertEquals("Should use Token Program", TokenProgram.PROGRAM_ID, instruction.programId)
        assertEquals("Should have 3 accounts (source, delegate, owner)",
                    3, instruction.accounts.size)

        assertTrue("Owner should be signer", instruction.accounts[2].isSigner)
        assertTrue("Source should be writable", instruction.accounts[0].isWritable)
        assertFalse("Delegate should not be writable", instruction.accounts[1].isWritable)
        assertFalse("Delegate should not be signer", instruction.accounts[1].isSigner)
    }

    @Test
    fun testTokenRevokeInstruction() {
        // Should create revoke instruction to remove approval
        val tokenAccount = AssociatedTokenProgram.getAssociatedTokenAddress(owner, tokenMint)

        val instruction = TokenProgram.revoke(
            source = tokenAccount,
            owner = owner
        )

        assertNotNull("Revoke instruction should not be null", instruction)
        assertEquals("Should use Token Program", TokenProgram.PROGRAM_ID, instruction.programId)
        assertEquals("Should have 2 accounts (source, owner)",
                    2, instruction.accounts.size)

        assertTrue("Owner should be signer", instruction.accounts[1].isSigner)
        assertTrue("Source should be writable", instruction.accounts[0].isWritable)
    }

    @Test
    fun testMultipleTokenAccountsForSameOwner() {
        // Should handle multiple token accounts for the same owner
        val mint1 = SolanaPrivateKey.generate().publicKey()
        val mint2 = SolanaPrivateKey.generate().publicKey()

        val ata1 = AssociatedTokenProgram.getAssociatedTokenAddress(owner, mint1)
        val ata2 = AssociatedTokenProgram.getAssociatedTokenAddress(owner, mint2)

        assertNotEquals("Different mints should produce different ATAs", ata1, ata2)

        // Same owner, same mint should always produce same ATA
        val ata1Again = AssociatedTokenProgram.getAssociatedTokenAddress(owner, mint1)
        assertEquals("Same owner and mint should produce same ATA", ata1, ata1Again)
    }

    @Test
    fun testTokenProgramConstants() {
        // Should have correct program IDs and constants
        assertNotNull("Token Program ID should not be null", TokenProgram.PROGRAM_ID)
        assertNotNull("Associated Token Program ID should not be null",
                     AssociatedTokenProgram.PROGRAM_ID)

        // Program IDs should be different
        assertNotEquals("Token and ATA programs should have different IDs",
                       TokenProgram.PROGRAM_ID, AssociatedTokenProgram.PROGRAM_ID)

        // Should have proper sizes for token program constants
        assertTrue("Token account size should be reasonable",
                  TokenProgram.ACCOUNT_SIZE > 0 && TokenProgram.ACCOUNT_SIZE < 1000)
        assertTrue("Mint account size should be reasonable",
                  TokenProgram.MINT_SIZE > 0 && TokenProgram.MINT_SIZE < 1000)
    }
}