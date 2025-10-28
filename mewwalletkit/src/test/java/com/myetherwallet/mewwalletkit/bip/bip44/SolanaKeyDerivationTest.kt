package com.myetherwallet.mewwalletkit.bip.bip44

import com.myetherwallet.mewwalletkit.bip.bip39.BIP39
import com.myetherwallet.mewwalletkit.core.extension.toHexString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Tests Solana key derivation to match iOS implementation
 *
 * Solana Derivation Paths:
 * - Network.SOLANA base path: m/44'/501'/0'/0
 * - Network.SOLANA_ANONYMIZED_ID path: m/1080'/60'/0'/0
 * - This test derives: m/44'/501'/0'/0' (4 hardened levels)
 */
class SolanaKeyDerivationTest {

    @Test
    fun `test Solana HD key derivation matches iOS`() {
        val mnemonicString = "hat correct find conduct original nasty narrow slush wool smile spread pride spirit profit mention smart squeeze roast inhale claim frog eye leave step"
        val mnemonicWords = mnemonicString.split(" ")

        // Generate seed from mnemonic using BIP39
        val bip39 = BIP39(mnemonicWords)
        val seed = bip39.seed()
        assertNotNull("Seed should not be null", seed)

        // Create master key with Solana network
        val masterKey = PrivateKey.createWithSeed(seed!!, Network.SOLANA)
        assertNotNull(masterKey)

        // Derive path m/44'/501'/0'/0' (Solana standard path)
        // For hardened derivation, must add 0x80000000 to the index
        val hardenedOffset = 0x80000000.toInt()
        val nodes: Array<DerivationNode> = arrayOf(
            DerivationNode.HARDENED(hardenedOffset or 44),   // 44'
            DerivationNode.HARDENED(hardenedOffset or 501),  // 501' (Solana coin type)
            DerivationNode.HARDENED(hardenedOffset or 0),    // 0' (account)
            DerivationNode.HARDENED(hardenedOffset or 0)     // 0' (change)
        )

        val derivedKey = masterKey.derived(nodes)
        assertNotNull("Derived key should not be null", derivedKey)

        // Test 1: privateKey.data() should return 32-byte private key
        val privateKeyData = derivedKey!!.data()
        assertEquals("Private key should be 32 bytes", 32, privateKeyData.size)
        assertEquals(
            "Private key hex should match iOS",
            "c0b9355922b6df97e88b04058dee478908328dd959adf61991d7ca64e4d27a8c",
            privateKeyData.toHexString()
        )

        // Test 2: privateKey.ed25519() should return 64-byte key (32 private + 32 public)
        val ed25519Key = derivedKey.ed25519()
        assertNotNull("Ed25519 key should not be null for Solana", ed25519Key)
        assertEquals("Ed25519 key should be 64 bytes", 64, ed25519Key!!.size)
        assertEquals(
            "Ed25519 key hex should match iOS",
            "c0b9355922b6df97e88b04058dee478908328dd959adf61991d7ca64e4d27a8c5f9e678f7f5c32ead7d451255dcd2ae713d0a68dfb8ec8d952b545badc241843",
            ed25519Key.toHexString()
        )

        // Test 3: publicKey.data() should return 32-byte public key
        val publicKey = derivedKey.publicKey()
        assertNotNull("Public key should not be null", publicKey)

        val publicKeyData = publicKey!!.data()
        assertEquals("Public key should be 32 bytes", 32, publicKeyData.size)
        assertEquals(
            "Public key hex should match iOS",
            "5f9e678f7f5c32ead7d451255dcd2ae713d0a68dfb8ec8d952b545badc241843",
            publicKeyData.toHexString()
        )
    }

    @Test
    fun `test ed25519 returns null for non-Solana networks`() {
        val mnemonicString = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val mnemonicWords = mnemonicString.split(" ")
        val bip39 = BIP39(mnemonicWords)
        val seed = bip39.seed()
        assertNotNull("Seed should not be null", seed)

        // Test with Ethereum network
        val ethKey = PrivateKey.createWithSeed(seed!!, Network.ETHEREUM)
        val ed25519Key = ethKey.ed25519()

        assertEquals("Ed25519 should return null for Ethereum", null, ed25519Key)
    }
}
