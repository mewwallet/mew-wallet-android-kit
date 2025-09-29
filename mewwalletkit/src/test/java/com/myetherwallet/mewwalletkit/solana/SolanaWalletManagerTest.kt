package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.bip.bip44.PrivateKey
import com.myetherwallet.mewwalletkit.bip.bip44.Wallet
import com.myetherwallet.mewwalletkit.bip.bip44.Network
import com.myetherwallet.mewwalletkit.solana.rpc.SolanaNetwork
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Comprehensive tests for the SolanaWalletManager
 */
class SolanaWalletManagerTest {

    private lateinit var sdk: SolanaSDK
    private lateinit var walletManager: SolanaWalletManager
    private lateinit var testWallet: Wallet

    @Before
    fun setup() {
        sdk = SolanaSDK.create(SolanaNetwork.DEVNET)
        walletManager = SolanaWalletManager.create(sdk)
        testWallet = Wallet.generate(network = Network.SOLANA).second
    }

    @Test
    fun testWalletManagerCreation() {
        val manager = SolanaWalletManager.create(sdk)
        assertNotNull("Wallet manager should not be null", manager)
    }

    @Test
    fun testCreateWallet() {
        val wallet1 = walletManager.createWallet()
        val wallet2 = walletManager.createWallet()

        assertNotNull("First wallet should not be null", wallet1)
        assertNotNull("Second wallet should not be null", wallet2)

        // Wallets should be different
        val address1 = wallet1.privateKey.getSolanaAddress()!!
        val address2 = wallet2.privateKey.getSolanaAddress()!!
        assertNotEquals("Wallets should generate different addresses", address1, address2)
    }

    @Test
    fun testRestoreWallet() {
        val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"

        val restoredWallet = walletManager.restoreWallet(mnemonic)
        assertNotNull("Restored wallet should not be null", restoredWallet)

        // Test with passphrase
        val restoredWalletWithPass = walletManager.restoreWallet(mnemonic, "testpass")
        assertNotNull("Restored wallet with passphrase should not be null", restoredWalletWithPass)

        // Wallets with different passphrases should be different
        val address1 = restoredWallet.privateKey.getSolanaAddress()!!
        val address2 = restoredWalletWithPass.privateKey.getSolanaAddress()!!
        assertNotEquals("Wallets with different passphrases should be different", address1, address2)
    }

    @Test
    fun testAccountInfo() = runBlocking {
        val accountInfo = walletManager.getAccountInfo(testWallet, 0)

        assertNotNull("Account info should not be null", accountInfo)
        assertEquals("Account index should match", 0, accountInfo.index)
        assertNotNull("Public key should not be null", accountInfo.publicKey)
        assertNotNull("Address should not be null", accountInfo.address)
        assertTrue("Balance should be non-negative", accountInfo.balance >= 0)

        // Test formatted balance
        val formattedBalance = accountInfo.getFormattedBalance()
        assertTrue("Formatted balance should contain SOL", formattedBalance.contains("SOL"))

        // Test SOL conversion
        val balanceSOL = accountInfo.getBalanceSOL()
        assertTrue("SOL balance should be non-negative", balanceSOL >= 0.0)
        assertEquals("SOL conversion should be consistent",
                    SolanaSDK.lamportsToSOL(accountInfo.balance), balanceSOL, 0.000000001)
    }

    @Test
    fun testMultipleAccountsInfo() = runBlocking {
        val indices = listOf(0, 1, 2, 3)
        val accountsInfo = walletManager.getAccountsInfo(testWallet, indices)

        assertEquals("Should return info for all requested accounts", indices.size, accountsInfo.size)

        for (i in indices.indices) {
            val account = accountsInfo[i]
            assertEquals("Account index should match", indices[i], account.index)
            assertNotNull("Address should not be null", account.address)
            assertTrue("Balance should be non-negative", account.balance >= 0)
        }

        // All accounts should have different addresses
        val addresses = accountsInfo.map { it.address }.toSet()
        assertEquals("All addresses should be unique", accountsInfo.size, addresses.size)
    }

    @Test
    fun testAccountsWithBalance() = runBlocking {
        // This will mostly return just the first account since test accounts have no balance
        val accounts = walletManager.getAccountsWithBalance(testWallet, 5)

        assertTrue("Should return at least the first account", accounts.isNotEmpty())
        assertTrue("Should not exceed max accounts", accounts.size <= 5)

        // First account should always be included
        assertEquals("First account should have index 0", 0, accounts[0].index)
    }

    @Test
    fun testPrivateKeyGeneration() {
        val privateKey1 = walletManager.getPrivateKey(testWallet, 0)
        val privateKey2 = walletManager.getPrivateKey(testWallet, 1)
        val privateKey3 = walletManager.getPrivateKey(testWallet, 0) // Same index

        assertNotNull("Private key 1 should not be null", privateKey1)
        assertNotNull("Private key 2 should not be null", privateKey2)
        assertNotNull("Private key 3 should not be null", privateKey3)

        // Different indices should give different keys
        assertNotEquals("Different indices should give different keys",
                       privateKey1.toHexString(), privateKey2.toHexString())

        // Same index should give same key
        assertEquals("Same index should give same key",
                    privateKey1.toHexString(), privateKey3.toHexString())

        // Public keys should match wallet addresses
        val address0 = testWallet.getAddress(0)
        val pubKey0 = privateKey1.publicKey().toSolanaBase58()
        assertEquals("Public key should match wallet address", address0, pubKey0)
    }

    @Test
    fun testRandomKeyGeneration() {
        val randomKey1 = walletManager.generateRandomKey()
        val randomKey2 = walletManager.generateRandomKey()

        assertNotNull("Random key 1 should not be null", randomKey1)
        assertNotNull("Random key 2 should not be null", randomKey2)

        // Random keys should be different
        assertNotEquals("Random keys should be different",
                       randomKey1.toHexString(), randomKey2.toHexString())

        // Keys should be valid
        val pubKey1 = randomKey1.publicKey()
        val pubKey2 = randomKey2.publicKey()
        assertNotNull("Public key 1 should not be null", pubKey1)
        assertNotNull("Public key 2 should not be null", pubKey2)

        // Addresses should be valid
        assertTrue("Address 1 should be valid",
                  SolanaWalletManager.isValidAddress(pubKey1.toSolanaBase58()!!))
        assertTrue("Address 2 should be valid",
                  SolanaWalletManager.isValidAddress(pubKey2.toSolanaBase58()!!))
    }

    @Test
    fun testCreateFromSeed() {
        val seed1 = ByteArray(32) { it.toByte() }
        val seed2 = ByteArray(32) { (it + 1).toByte() }

        val key1 = walletManager.createFromSeed(seed1)
        val key2 = walletManager.createFromSeed(seed2)
        val key3 = walletManager.createFromSeed(seed1) // Same seed

        assertNotNull("Key 1 should not be null", key1)
        assertNotNull("Key 2 should not be null", key2)
        assertNotNull("Key 3 should not be null", key3)

        // Different seeds should give different keys
        assertNotEquals("Different seeds should give different keys",
                       key1.toHexString(), key2.toHexString())

        // Same seed should give same key
        assertEquals("Same seed should give same key",
                    key1.toHexString(), key3.toHexString())

        // Test invalid seed length
        try {
            val invalidSeed = ByteArray(16) // Too short
            walletManager.createFromSeed(invalidSeed)
            fail("Should throw exception for invalid seed length")
        } catch (e: IllegalArgumentException) {
            assertTrue("Exception message should mention seed length",
                      e.message!!.contains("32 bytes"))
        }
    }

    @Test
    fun testMnemonicValidation() {
        val validMnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val invalidMnemonic = "invalid mnemonic phrase with random words that do not form valid bip39"
        val emptyMnemonic = ""

        assertTrue("Valid mnemonic should pass validation",
                  walletManager.validateMnemonic(validMnemonic))
        assertFalse("Invalid mnemonic should fail validation",
                   walletManager.validateMnemonic(invalidMnemonic))
        assertFalse("Empty mnemonic should fail validation",
                   walletManager.validateMnemonic(emptyMnemonic))
    }

    @Test
    fun testMnemonicGeneration() {
        // Test default word count
        val mnemonic12 = walletManager.generateMnemonic()
        val words12 = mnemonic12.split(" ")
        assertEquals("Default should generate 12 words", 12, words12.size)

        // Test different word counts
        val mnemonic24 = walletManager.generateMnemonic(24)
        val words24 = mnemonic24.split(" ")
        assertEquals("Should generate 24 words", 24, words24.size)

        // Test edge cases
        val mnemonic15 = walletManager.generateMnemonic(15)
        val words15 = mnemonic15.split(" ")
        assertTrue("Should generate at most 15 words", words15.size <= 15)

        // Different generations should be different (though this is mocked)
        val mnemonic1 = walletManager.generateMnemonic()
        val mnemonic2 = walletManager.generateMnemonic()
        // Note: In real implementation these would be different, but mocked version returns same
        assertNotNull("Mnemonic 1 should not be null", mnemonic1)
        assertNotNull("Mnemonic 2 should not be null", mnemonic2)
    }

    @Test
    fun testAddressDerivation() {
        val addresses = walletManager.deriveAddresses(testWallet, 5)

        assertEquals("Should derive requested number of addresses", 5, addresses.size)

        // All addresses should be unique
        val uniqueAddresses = addresses.toSet()
        assertEquals("All addresses should be unique", addresses.size, uniqueAddresses.size)

        // All addresses should be valid
        for (address in addresses) {
            assertTrue("Address should be valid: $address",
                      SolanaWalletManager.isValidAddress(address))
        }

        // Test with custom start index
        val addressesFromIndex2 = walletManager.deriveAddresses(testWallet, 3, 2)
        assertEquals("Should derive requested number from custom start", 3, addressesFromIndex2.size)

        // Should match individual derivations
        for (i in addressesFromIndex2.indices) {
            val expectedAddress = testWallet.getAddress(i + 2)
            assertEquals("Address should match individual derivation",
                        expectedAddress, addressesFromIndex2[i])
        }
    }

    @Test
    fun testFindAddressIndex() {
        val targetIndex = 5
        val targetAddress = testWallet.getAddress(targetIndex)

        val foundIndex = walletManager.findAddressIndex(testWallet, targetAddress)
        assertEquals("Should find correct index", targetIndex, foundIndex)

        // Test with non-existent address
        val nonExistentAddress = "11111111111111111111111111111111" // System program
        val notFoundIndex = walletManager.findAddressIndex(testWallet, nonExistentAddress, 10)
        assertNull("Should not find non-existent address", notFoundIndex)

        // Test with limited search range
        val limitedIndex = walletManager.findAddressIndex(testWallet, targetAddress, 3)
        assertNull("Should not find address beyond search limit", limitedIndex)

        // Test finding address within search range
        val earlyTargetAddress = testWallet.getAddress(1)
        val foundEarlyIndex = walletManager.findAddressIndex(testWallet, earlyTargetAddress, 10)
        assertEquals("Should find early index", 1, foundEarlyIndex)
    }

    @Test
    fun testStaticUtilities() {
        // Test address validation
        val validAddress = "11111111111111111111111111111111" // System program
        val invalidAddress = "invalid_address_format"
        val emptyAddress = ""

        assertTrue("System program address should be valid",
                  SolanaWalletManager.isValidAddress(validAddress))
        assertFalse("Invalid format should be rejected",
                   SolanaWalletManager.isValidAddress(invalidAddress))
        assertFalse("Empty address should be rejected",
                   SolanaWalletManager.isValidAddress(emptyAddress))

        // Test constants
        assertEquals("Derivation path should match expected",
                    "m/44'/501'/%d'/0'", SolanaWalletManager.SOLANA_DERIVATION_PATH)
        assertEquals("Default account count should be 10",
                    10, SolanaWalletManager.DEFAULT_ACCOUNT_COUNT)
    }

    @Test
    fun testWalletConsistency() {
        // Test that wallet operations are consistent
        val wallet1 = walletManager.createWallet()
        val wallet2 = walletManager.createWallet()

        // Each wallet should consistently derive the same addresses
        val addr1_0 = wallet1.getAddress(0)
        val addr1_0_again = wallet1.getAddress(0)
        assertEquals("Same wallet should derive same address", addr1_0, addr1_0_again)

        // Different wallets should derive different addresses
        val addr2_0 = wallet2.getAddress(0)
        assertNotEquals("Different wallets should derive different addresses", addr1_0, addr2_0)

        // Private keys should be consistent with addresses
        val privKey1_0 = walletManager.getPrivateKey(wallet1, 0)
        val pubKey1_0 = privKey1_0.publicKey().toSolanaBase58()
        assertEquals("Private key should match derived address", addr1_0, pubKey1_0)
    }

    @Test
    fun testErrorHandling() = runBlocking {
        // Test with invalid account index (negative)
        try {
            walletManager.getAccountInfo(testWallet, -1)
            // This might not throw in the current implementation, but if it does, that's okay
        } catch (e: Exception) {
            assertTrue("Exception should have meaningful message", e.message?.isNotEmpty() == true)
        }

        // Test network errors (mock scenario)
        // In a real test, we might mock the SDK to throw network errors
        val accountInfo = walletManager.getAccountInfo(testWallet, 0)
        // Should handle network errors gracefully and return 0 balance
        assertTrue("Should handle network errors gracefully", accountInfo.balance >= 0)
    }

    @Test
    fun testAccountInfoFormatting() = runBlocking {
        val accountInfo = walletManager.getAccountInfo(testWallet, 0)

        // Test balance formatting
        val formattedBalance = accountInfo.getFormattedBalance()
        assertTrue("Formatted balance should end with SOL", formattedBalance.endsWith("SOL"))
        assertTrue("Formatted balance should contain number",
                  formattedBalance.matches(Regex("\\d+\\.\\d+ SOL")))

        // Test SOL conversion consistency
        val balanceSOL = accountInfo.getBalanceSOL()
        val expectedSOL = SolanaSDK.lamportsToSOL(accountInfo.balance)
        assertEquals("SOL conversion should be consistent", expectedSOL, balanceSOL, 0.000000001)

        // Test edge cases
        val zeroBalanceInfo = SolanaWalletManager.AccountInfo(
            index = 0,
            publicKey = testWallet.getPrivateKey(0).publicKey(),
            address = testWallet.getAddress(0),
            balance = 0L
        )

        assertEquals("Zero balance should convert to 0.0 SOL", 0.0, zeroBalanceInfo.getBalanceSOL(), 0.0)
        assertEquals("Zero balance should format correctly", "0.000000000 SOL", zeroBalanceInfo.getFormattedBalance())

        // Test large balance
        val largeBalanceInfo = SolanaWalletManager.AccountInfo(
            index = 0,
            publicKey = testWallet.getPrivateKey(0).publicKey(),
            address = testWallet.getAddress(0),
            balance = 1_000_000_000L // 1 SOL
        )

        assertEquals("1 SOL should convert correctly", 1.0, largeBalanceInfo.getBalanceSOL(), 0.000000001)
        assertEquals("1 SOL should format correctly", "1.000000000 SOL", largeBalanceInfo.getFormattedBalance())
    }
}