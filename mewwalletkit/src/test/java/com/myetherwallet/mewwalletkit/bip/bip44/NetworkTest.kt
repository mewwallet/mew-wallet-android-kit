package com.myetherwallet.mewwalletkit.bip.bip44

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for Network derivation path functions
 */
class NetworkTest {

    @Test
    fun `test pathWithIndex for Ethereum network with multiple accounts`() {
        // Ethereum path: m/44'/60'/0'/0
        // Expected pattern: m/44'/60'/0'/0/{index}

        assertEquals(
            "Ethereum account 0 path should be correct",
            "m/44'/60'/0'/0/0",
            Network.ETHEREUM.pathWithIndex(0)
        )

        assertEquals(
            "Ethereum account 1 path should be correct",
            "m/44'/60'/0'/0/1",
            Network.ETHEREUM.pathWithIndex(1)
        )

        assertEquals(
            "Ethereum account 2 path should be correct",
            "m/44'/60'/0'/0/2",
            Network.ETHEREUM.pathWithIndex(2)
        )

        assertEquals(
            "Ethereum account 5 path should be correct",
            "m/44'/60'/0'/0/5",
            Network.ETHEREUM.pathWithIndex(5)
        )

        assertEquals(
            "Ethereum account 10 path should be correct",
            "m/44'/60'/0'/0/10",
            Network.ETHEREUM.pathWithIndex(10)
        )
    }

    @Test
    fun `test pathWithIndex for Solana network with multiple accounts`() {
        // Solana path: m/44'/501'/0'/0
        // Expected pattern: m/44'/501'/{index}'/0' (account index is hardened)

        assertEquals(
            "Solana account 0 path should be correct",
            "m/44'/501'/0'/0'",
            Network.SOLANA.pathWithIndex(0)
        )

        assertEquals(
            "Solana account 1 path should be correct",
            "m/44'/501'/1'/0'",
            Network.SOLANA.pathWithIndex(1)
        )

        assertEquals(
            "Solana account 2 path should be correct",
            "m/44'/501'/2'/0'",
            Network.SOLANA.pathWithIndex(2)
        )

        assertEquals(
            "Solana account 5 path should be correct",
            "m/44'/501'/5'/0'",
            Network.SOLANA.pathWithIndex(5)
        )

        assertEquals(
            "Solana account 10 path should be correct",
            "m/44'/501'/10'/0'",
            Network.SOLANA.pathWithIndex(10)
        )
    }

    @Test
    fun `test pathWithIndex for other networks`() {
        // Test a few other networks to ensure they follow the default pattern

        assertEquals(
            "Bitcoin account 0 path should be correct",
            "m/44'/0'/0'/0/0",
            Network.BITCOIN.pathWithIndex(0)
        )

        assertEquals(
            "Bitcoin account 1 path should be correct",
            "m/44'/0'/0'/0/1",
            Network.BITCOIN.pathWithIndex(1)
        )

        assertEquals(
            "Ropsten account 0 path should be correct",
            "m/44'/1'/0'/0/0",
            Network.ROPSTEN.pathWithIndex(0)
        )

        assertEquals(
            "Ropsten account 3 path should be correct",
            "m/44'/1'/0'/0/3",
            Network.ROPSTEN.pathWithIndex(3)
        )
    }

    @Test
    fun `test pathWithIndex for SOLANA_ANONYMIZED_ID network`() {
        // SOLANA_ANONYMIZED_ID uses Ethereum rules (default case)
        // Path: m/1080'/60'/0'/0
        // Expected pattern: m/1080'/60'/0'/0/{index}

        assertEquals(
            "SOLANA_ANONYMIZED_ID account 0 path should be correct",
            "m/1080'/60'/0'/0/0",
            Network.SOLANA_ANONYMIZED_ID.pathWithIndex(0)
        )

        assertEquals(
            "SOLANA_ANONYMIZED_ID account 1 path should be correct",
            "m/1080'/60'/0'/0/1",
            Network.SOLANA_ANONYMIZED_ID.pathWithIndex(1)
        )

        assertEquals(
            "SOLANA_ANONYMIZED_ID account 5 path should be correct",
            "m/1080'/60'/0'/0/5",
            Network.SOLANA_ANONYMIZED_ID.pathWithIndex(5)
        )
    }
}
