package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.solana.bip44.SolanaWallet
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.solana.crypto.SolanaPrivateKey
import com.myetherwallet.mewwalletkit.solana.program.SystemProgram
import com.myetherwallet.mewwalletkit.solana.transaction.SolanaTransaction

/**
 * Main Solana utility class providing easy access to Solana functionality
 */
object Solana {

    /**
     * Create a new Solana wallet from mnemonic
     * @param mnemonic BIP39 mnemonic phrase
     * @param passphrase Optional passphrase
     * @return Solana wallet instance
     */
    fun createWallet(mnemonic: String, passphrase: String = ""): SolanaWallet {
        return SolanaWallet.fromMnemonic(mnemonic, passphrase)
    }

    /**
     * Generate a new random Solana wallet
     * @return New random Solana wallet
     */
    fun generateWallet(): SolanaWallet {
        return SolanaWallet.generate()
    }

    /**
     * Create a public key from Base58 string
     * @param base58 Base58 encoded public key
     * @return Solana public key
     */
    fun publicKey(base58: String): PublicKey {
        return PublicKey.fromSolanaBase58(base58)
    }

    /**
     * Create a private key from hex string
     * @param hex Hex encoded private key seed
     * @return Solana private key
     */
    fun privateKey(hex: String): SolanaPrivateKey {
        return SolanaPrivateKey.fromHex(hex)
    }

    /**
     * Validate a Solana address
     * @param address Base58 encoded address
     * @return true if valid Solana address
     */
    fun isValidAddress(address: String): Boolean {
        return PublicKey.isValidSolanaAddress(address)
    }

    /**
     * Create a simple SOL transfer transaction
     * @param from Source address
     * @param to Destination address
     * @param lamports Amount in lamports (1 SOL = 1,000,000,000 lamports)
     * @param recentBlockhash Recent blockhash from the network
     * @return Unsigned transaction
     */
    fun createTransfer(
        from: PublicKey,
        to: PublicKey,
        lamports: Long,
        recentBlockhash: String
    ): SolanaTransaction {
        return SolanaTransaction.createTransfer(from, to, lamports, recentBlockhash)
    }

    /**
     * Create a simple SOL transfer transaction from addresses
     * @param fromAddress Source address (Base58)
     * @param toAddress Destination address (Base58)
     * @param lamports Amount in lamports
     * @param recentBlockhash Recent blockhash from the network
     * @return Unsigned transaction
     */
    fun createTransfer(
        fromAddress: String,
        toAddress: String,
        lamports: Long,
        recentBlockhash: String
    ): SolanaTransaction {
        val from = PublicKey.fromSolanaBase58(fromAddress)
        val to = PublicKey.fromSolanaBase58(toAddress)
        return createTransfer(from, to, lamports, recentBlockhash)
    }

    /**
     * Convert SOL to lamports
     * @param sol Amount in SOL
     * @return Amount in lamports
     */
    fun solToLamports(sol: Double): Long {
        return (sol * 1_000_000_000).toLong()
    }

    /**
     * Convert lamports to SOL
     * @param lamports Amount in lamports
     * @return Amount in SOL
     */
    fun lamportsToSol(lamports: Long): Double {
        return lamports.toDouble() / 1_000_000_000
    }

    /**
     * System Program utilities
     */
    object System {
        /**
         * Create a transfer instruction
         */
        fun transfer(from: PublicKey, to: PublicKey, lamports: Long) =
            SystemProgram.transfer(from, to, lamports)

        /**
         * Create an account creation instruction
         */
        fun createAccount(
            from: PublicKey,
            newAccount: PublicKey,
            lamports: Long,
            space: Long,
            owner: PublicKey
        ) = SystemProgram.createAccount(from, newAccount, lamports, space, owner)

        /**
         * Get minimum balance for rent exemption
         */
        fun getMinimumBalanceForRentExemption(dataLength: Long) =
            SystemProgram.getMinimumBalanceForRentExemption(dataLength)
    }

    /**
     * Constants and well-known addresses
     */
    object Constants {
        val SYSTEM_PROGRAM = PublicKey.fromSolanaBase58("11111111111111111111111111111111")
        val STAKE_PROGRAM = PublicKey.fromSolanaBase58("Stake11111111111111111111111111111111111111")

        const val LAMPORTS_PER_SOL = 1_000_000_000L
    }
}