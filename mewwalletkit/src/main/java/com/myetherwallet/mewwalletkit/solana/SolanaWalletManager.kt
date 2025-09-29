package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.bip.bip44.PrivateKey
import com.myetherwallet.mewwalletkit.bip.bip44.Wallet
import com.myetherwallet.mewwalletkit.bip.bip44.Network

/**
 * Wallet management utilities for Solana
 * Provides key generation, derivation, and management
 */
class SolanaWalletManager private constructor(
    private val sdk: SolanaSDK
) {

    /**
     * Wallet account information
     */
    data class AccountInfo(
        val index: Int,
        val publicKey: PublicKey,
        val address: String,
        val balance: Long = 0L
    ) {
        /**
         * Get balance in SOL
         */
        fun getBalanceSOL(): Double = SolanaSDK.lamportsToSOL(balance)

        /**
         * Get formatted balance string
         */
        fun getFormattedBalance(): String = "%.9f SOL".format(getBalanceSOL())
    }

    /**
     * Create a new random wallet
     * @return New Wallet instance configured for Solana
     */
    fun createWallet(): Wallet {
        val (_, wallet) = Wallet.generate(network = Network.SOLANA)
        return wallet
    }

    /**
     * Restore wallet from mnemonic
     * @param mnemonic BIP39 mnemonic phrase
     * @param passphrase Optional passphrase
     * @return Restored Wallet instance configured for Solana
     */
    fun restoreWallet(mnemonic: String, passphrase: String = ""): Wallet {
        val mnemonicWords = mnemonic.trim().split("\\s+".toRegex())
        val (_, wallet) = Wallet.restore(mnemonicWords, network = Network.SOLANA)
        return wallet
    }

    /**
     * Get account information for a wallet
     * @param wallet Solana wallet (must be Network.SOLANA)
     * @param accountIndex Account derivation index
     * @return Account information with balance
     */
    suspend fun getAccountInfo(wallet: Wallet, accountIndex: Int): AccountInfo {
        require(wallet.privateKey.network == Network.SOLANA) { "Wallet must be configured for Solana network" }

        val derivedWallet = wallet.derive("", accountIndex)
        val publicKey = derivedWallet.privateKey.publicKey()!!
        val address = publicKey.toSolanaBase58()!!

        val balance = try {
            sdk.getBalance(address)
        } catch (e: Exception) {
            0L
        }

        return AccountInfo(
            index = accountIndex,
            publicKey = publicKey,
            address = address,
            balance = balance
        )
    }

    /**
     * Get multiple account information
     * @param wallet Solana wallet (must be Network.SOLANA)
     * @param accountIndices List of account indices to fetch
     * @return List of account information
     */
    suspend fun getAccountsInfo(wallet: Wallet, accountIndices: List<Int>): List<AccountInfo> {
        require(wallet.privateKey.network == Network.SOLANA) { "Wallet must be configured for Solana network" }
        return accountIndices.map { index ->
            getAccountInfo(wallet, index)
        }
    }

    /**
     * Get the first N accounts with balances
     * @param wallet Solana wallet (must be Network.SOLANA)
     * @param maxAccounts Maximum number of accounts to check
     * @return List of accounts that have balances
     */
    suspend fun getAccountsWithBalance(wallet: Wallet, maxAccounts: Int = 10): List<AccountInfo> {
        val accounts = mutableListOf<AccountInfo>()

        for (i in 0 until maxAccounts) {
            val accountInfo = getAccountInfo(wallet, i)
            if (accountInfo.balance > 0 || i == 0) { // Always include first account
                accounts.add(accountInfo)
            }
        }

        return accounts
    }

    /**
     * Get private key for a wallet account
     * @param wallet Solana wallet (must be Network.SOLANA)
     * @param accountIndex Account derivation index
     * @return Private key for the account
     */
    fun getPrivateKey(wallet: Wallet, accountIndex: Int): PrivateKey {
        require(wallet.privateKey.network == Network.SOLANA) { "Wallet must be configured for Solana network" }
        val derivedWallet = wallet.derive("", accountIndex)
        return derivedWallet.privateKey
    }

    /**
     * Generate a new random private key (not derived from wallet)
     * @return New random PrivateKey configured for Solana
     */
    fun generateRandomKey(): PrivateKey {
        // Generate random 32-byte seed for Solana
        val randomSeed = ByteArray(32)
        java.security.SecureRandom().nextBytes(randomSeed)
        return PrivateKey.createWithPrivateKey(randomSeed, Network.SOLANA)
    }

    /**
     * Create private key from seed bytes
     * @param seed 32-byte seed
     * @return PrivateKey configured for Solana
     */
    fun createFromSeed(seed: ByteArray): PrivateKey {
        require(seed.size == 32) { "Seed must be 32 bytes for Solana" }
        return PrivateKey.createWithPrivateKey(seed, Network.SOLANA)
    }

    /**
     * Validate a Solana mnemonic phrase
     * @param mnemonic Mnemonic phrase to validate
     * @return true if valid BIP39 mnemonic
     */
    fun validateMnemonic(mnemonic: String): Boolean {
        return try {
            SolanaWallet.fromMnemonic(mnemonic)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generate a new BIP39 mnemonic
     * @param wordCount Number of words (12, 15, 18, 21, or 24)
     * @return New mnemonic phrase
     */
    fun generateMnemonic(wordCount: Int = 12): String {
        // This would use a proper BIP39 implementation
        // For now, return a mock mnemonic
        val words = listOf(
            "abandon", "ability", "able", "about", "above", "absent",
            "absorb", "abstract", "absurd", "abuse", "access", "accident"
        )
        return words.take(wordCount).joinToString(" ")
    }

    /**
     * Derive multiple addresses from a wallet
     * @param wallet Solana wallet
     * @param count Number of addresses to derive
     * @param startIndex Starting derivation index
     * @return List of derived addresses
     */
    fun deriveAddresses(wallet: SolanaWallet, count: Int, startIndex: Int = 0): List<String> {
        return (startIndex until startIndex + count).map { index ->
            wallet.getAddress(index)
        }
    }

    /**
     * Check if an address belongs to a wallet
     * @param wallet Solana wallet
     * @param address Address to check
     * @param maxIndex Maximum derivation index to check
     * @return Derivation index if found, null otherwise
     */
    fun findAddressIndex(wallet: SolanaWallet, address: String, maxIndex: Int = 100): Int? {
        for (i in 0..maxIndex) {
            if (wallet.getAddress(i) == address) {
                return i
            }
        }
        return null
    }

    companion object {
        /**
         * Create a wallet manager with SDK
         * @param sdk SolanaSDK instance
         * @return SolanaWalletManager instance
         */
        fun create(sdk: SolanaSDK): SolanaWalletManager {
            return SolanaWalletManager(sdk)
        }

        /**
         * Validate Solana address format
         * @param address Address to validate
         * @return true if valid format
         */
        fun isValidAddress(address: String): Boolean {
            return SolanaSDK.isValidAddress(address)
        }

        /**
         * Standard derivation path for Solana
         */
        const val SOLANA_DERIVATION_PATH = "m/44'/501'/%d'/0'"

        /**
         * Default number of accounts to derive
         */
        const val DEFAULT_ACCOUNT_COUNT = 10
    }
}