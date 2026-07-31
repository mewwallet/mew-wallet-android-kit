package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.Network
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for Associated Token Account (ATA) and Program Derived Address (PDA) functionality.
 * Ported from iOS: Solana.AssociatedTokenProgram+Tests.swift
 */
class AssociatedTokenProgramTest {

    @Test
    fun `test associatedTokenAddress`() {
        // Test data from iOS
        val owner = PublicKey.createWithBase58("B8UwBUUnKwCyKuGMbFKWaG7exYdDk2ozZrPg72NyVbfj", Network.SOLANA)
        val mint1 = PublicKey.createWithBase58("7o36UsWR1JQLpZ9PE2gn9L4SQ69CNNiWAXd4Jt7rqz9Z", Network.SOLANA)

        // Test 1: Basic ATA derivation with on-curve owner
        val associatedPublicKey = owner.associatedTokenAddress(tokenMint = mint1)

        val expectedAssociatedPublicKey = PublicKey.createWithBase58(
            "DShWnroshVbeUp28oopA3Pu7oFPDBtC1DBmPECXXAQ9n",
            Network.SOLANA
        )

        assertArrayEquals(
            "Associated token address should match expected value",
            expectedAssociatedPublicKey.data(),
            associatedPublicKey.data()
        )

        // Test 2: Should throw error when trying to derive ATA with off-curve owner (without allowOwnerOffCurve)
        val mint2 = PublicKey.createWithBase58("7o36UsWR1JQLpZ9PE2gn9L4SQ69CNNiWAXd4Jt7rqz9Z", Network.SOLANA)

        try {
            associatedPublicKey.associatedTokenAddress(tokenMint = mint2)
            fail("Should throw AssociatedTokenError.OwnerOffCurve when owner is off-curve")
        } catch (e: AssociatedTokenError.OwnerOffCurve) {
            // Expected
        }

        // Test 3: Should succeed when allowOwnerOffCurve is true
        val associatedPublicKeyOffCurve = associatedPublicKey.associatedTokenAddress(
            tokenMint = mint2,
            allowOwnerOffCurve = true
        )

        val expectedAssociatedPublicKeyOffCurve = PublicKey.createWithBase58(
            "F3DmXZFqkfEWFA7MN2vDPs813GeEWPaT6nLk4PSGuWJd",
            Network.SOLANA
        )

        assertArrayEquals(
            "Associated token address with off-curve owner should match expected value",
            expectedAssociatedPublicKeyOffCurve.data(),
            associatedPublicKeyOffCurve.data()
        )
    }

    @Test
    fun `test isOnCurve distinguishes on-curve from off-curve keys`() {
        // Regular wallet address (on-curve)
        val onCurveKey = PublicKey.createWithBase58(
            "B8UwBUUnKwCyKuGMbFKWaG7exYdDk2ozZrPg72NyVbfj",
            Network.SOLANA
        )
        assertTrue("Regular wallet address should be on-curve", onCurveKey.isOnCurve())

        // PDA (off-curve) - derived from the first test
        val offCurveKey = PublicKey.createWithBase58(
            "DShWnroshVbeUp28oopA3Pu7oFPDBtC1DBmPECXXAQ9n",
            Network.SOLANA
        )
        assertFalse("PDA should be off-curve", offCurveKey.isOnCurve())
    }

    @Test
    fun `test createProgramAddress validates seed length`() {
        val programId = PublicKey.createWithBase58(
            "11111111111111111111111111111111",
            Network.SOLANA
        )

        // Seed that exceeds 32 bytes
        val longSeed = ByteArray(33) { 0xFF.toByte() }

        try {
            createProgramAddress(listOf(longSeed), programId)
            fail("Should throw MaxSeedLengthExceeded for seeds > 32 bytes")
        } catch (e: AssociatedTokenError.MaxSeedLengthExceeded) {
            // Expected
        }
    }

    @Test
    fun `test findProgramAddress returns valid bump and PDA`() {
        val owner = PublicKey.createWithBase58("B8UwBUUnKwCyKuGMbFKWaG7exYdDk2ozZrPg72NyVbfj", Network.SOLANA)
        val mint = PublicKey.createWithBase58("7o36UsWR1JQLpZ9PE2gn9L4SQ69CNNiWAXd4Jt7rqz9Z", Network.SOLANA)
        val tokenProgramId = TokenProgram.programId()
        val associatedTokenProgramId = AssociatedTokenProgram.programId()

        val seeds = listOf(
            owner.data(),
            tokenProgramId.data(),
            mint.data()
        )

        val (pda, bump) = findProgramAddress(seeds, associatedTokenProgramId)

        assertNotNull("PDA should not be null", pda)
        assertTrue("Bump should be between 0 and 255", bump <= 255u)
        assertFalse("PDA should be off-curve", pda.isOnCurve())

        // Verify we can recreate the same PDA with the found bump
        val recreatedPda = createProgramAddress(
            seeds + listOf(byteArrayOf(bump.toByte())),
            associatedTokenProgramId
        )

        assertArrayEquals("Recreated PDA should match original", pda.data(), recreatedPda.data())
    }

    @Test
    fun `test TokenProgram and AssociatedTokenProgram have correct addresses`() {
        // Verify program IDs match expected Solana addresses
        val expectedTokenProgramId = PublicKey.createWithBase58(
            "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
            Network.SOLANA
        )

        val expectedAssociatedTokenProgramId = PublicKey.createWithBase58(
            "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL",
            Network.SOLANA
        )

        assertArrayEquals(
            "TokenProgram ID should match",
            expectedTokenProgramId.data(),
            TokenProgram.programId().data()
        )

        assertArrayEquals(
            "AssociatedTokenProgram ID should match",
            expectedAssociatedTokenProgramId.data(),
            AssociatedTokenProgram.programId().data()
        )
    }
}
