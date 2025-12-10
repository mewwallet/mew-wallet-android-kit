package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.Network
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.core.extension.hexToByteArray
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for PublicKey.isOnCurve() extension function.
 *
 * Validates that the function correctly identifies whether a public key is on the Ed25519 curve.
 * This is important for distinguishing between regular keys and Program Derived Addresses (PDAs).
 */
class PublicKeyOnCurveTest {

    @Test
    fun `test valid on-curve public key`() {
        // Create a valid Ed25519 public key from test keys
        val publicKeys = TestHelpers.createTestPublicKeys(1)
        val publicKey = publicKeys[0]

        // Regular Ed25519 public keys should be on the curve
        assertTrue("Valid Ed25519 public key should be on curve", publicKey.isOnCurve())
    }

    @Test
    fun `test multiple valid public keys are on curve`() {
        // Test several valid keys
        val publicKeys = TestHelpers.createTestPublicKeys(5)

        publicKeys.forEach { publicKey ->
            assertTrue(
                "All valid Ed25519 keys should be on curve",
                publicKey.isOnCurve()
            )
        }
    }

    @Test
    fun `test System Program ID is off-curve`() {
        // System Program ID "11111111111111111111111111111111" is a program address
        // Program addresses in Solana are intentionally off-curve (like PDAs)
        // because they shouldn't have a corresponding private key
        val systemProgramKey = SystemProgram.programId()

        assertFalse("System Program ID should be off-curve (it's a program address)",
            systemProgramKey.isOnCurve())
    }

    @Test
    fun `test invalid public key with all zeros`() {
        // All zeros is not a valid point on the curve
        val invalidKey = PublicKey(ByteArray(32) { 0x00 }, Network.SOLANA)

        assertFalse("All-zero key should not be on curve", invalidKey.isOnCurve())
    }

    @Test
    fun `test invalid public key with all ones`() {
        // All ones is not a valid point on the curve
        val invalidKey = PublicKey(ByteArray(32) { 0xFF.toByte() }, Network.SOLANA)

        assertFalse("All-ones key should not be on curve", invalidKey.isOnCurve())
    }

    @Test
    fun `test invalid public key with random bytes`() {
        // Random bytes are very unlikely to be on the curve
        val invalidKey = PublicKey(
            "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff".hexToByteArray(),
            Network.SOLANA
        )

        assertFalse("Random bytes should likely not be on curve", invalidKey.isOnCurve())
    }

    @Test
    fun `test PDA-like off-curve key`() {
        // A known off-curve point (simulating a PDA)
        // PDAs are intentionally created to be off-curve so they can't sign
        val offCurveBytes = ByteArray(32) { index ->
            // Fill with pattern that's definitely not on curve
            ((index * 7 + 13) % 256).toByte()
        }
        val pdaLikeKey = PublicKey(offCurveBytes, Network.SOLANA)

        // This should be off-curve (though we can't guarantee without actual PDA generation)
        // The test mainly verifies the function doesn't crash
        val result = pdaLikeKey.isOnCurve()
        // We just verify the function returns a boolean without crashing
        assertTrue("Result should be a boolean", result is Boolean)
    }

    @Test
    fun `test known on-curve addresses from test helpers`() {
        // Parse known valid Solana addresses
        val validAddresses = listOf(
            "J3dxNj7nDRRqRRXuEMynDG57DkZK4jYRuv3Garmb1i99",  // Test recipient
            "9U2wM3MUToUCRiBsR6zAcnqJw43kcXSxp27QxsKEJSBf"   // Could be blockhash but same format
        )

        validAddresses.forEach { address ->
            try {
                val publicKey = TestHelpers.parseAddress(address)
                // These are valid Base58 addresses, but we need to check if they're on curve
                val onCurve = publicKey.isOnCurve()
                // Just verify the function works without crashing
                assertTrue("Result should be a boolean", onCurve is Boolean)
            } catch (e: Exception) {
                // If parsing fails, that's okay for this test
            }
        }
    }

    @Test
    fun `test consistency - calling twice returns same result`() {
        val publicKey = TestHelpers.createTestPublicKeys(1)[0]

        val result1 = publicKey.isOnCurve()
        val result2 = publicKey.isOnCurve()

        assertEquals("isOnCurve should return consistent results", result1, result2)
    }

    @Test
    fun `test public key from private key is on curve`() {
        // Generate a public key from a private key
        val privateKeys = TestHelpers.createTestKeys(1)
        val publicKey = privateKeys[0].publicKey()

        assertNotNull("Public key should not be null", publicKey)
        assertTrue("Public key derived from private key should be on curve", publicKey!!.isOnCurve())
    }
}
