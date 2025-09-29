package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.solana.crypto.SolanaPrivateKey
import com.myetherwallet.mewwalletkit.solana.rpc.SolanaNetwork
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*

/**
 * Utility functions for Solana operations
 * Provides common formatting, validation, and conversion functions
 */
object SolanaUtils {

    /**
     * Format lamports as SOL with specified decimal places
     * @param lamports Amount in lamports
     * @param decimals Number of decimal places (default: 9)
     * @return Formatted SOL amount
     */
    fun formatSOL(lamports: Long, decimals: Int = 9): String {
        val sol = BigDecimal(lamports).divide(BigDecimal(1_000_000_000), decimals, RoundingMode.DOWN)
        return "${sol.toPlainString()} SOL"
    }

    /**
     * Format lamports as SOL with currency formatting
     * @param lamports Amount in lamports
     * @param locale Locale for formatting (default: US)
     * @return Formatted SOL amount with locale-specific formatting
     */
    fun formatSOLCurrency(lamports: Long, locale: Locale = Locale.US): String {
        val sol = lamports / 1_000_000_000.0
        val formatter = NumberFormat.getNumberInstance(locale)
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 9
        return "${formatter.format(sol)} SOL"
    }

    /**
     * Format token amount with specified decimals
     * @param amount Raw token amount
     * @param decimals Token decimals
     * @param symbol Token symbol
     * @return Formatted token amount
     */
    fun formatToken(amount: Long, decimals: Int, symbol: String): String {
        val divisor = BigDecimal(10).pow(decimals)
        val tokenAmount = BigDecimal(amount).divide(divisor, decimals, RoundingMode.DOWN)
        return "${tokenAmount.toPlainString()} $symbol"
    }

    /**
     * Parse SOL amount to lamports
     * @param solAmount SOL amount as string
     * @return Amount in lamports
     * @throws IllegalArgumentException if invalid format
     */
    fun parseSOLToLamports(solAmount: String): Long {
        return try {
            val sol = BigDecimal(solAmount.replace("[^0-9.]".toRegex(), ""))
            sol.multiply(BigDecimal(1_000_000_000)).toLong()
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid SOL amount: $solAmount")
        }
    }

    /**
     * Parse token amount to raw units
     * @param tokenAmount Token amount as string
     * @param decimals Token decimals
     * @return Raw token amount
     * @throws IllegalArgumentException if invalid format
     */
    fun parseTokenAmount(tokenAmount: String, decimals: Int): Long {
        return try {
            val amount = BigDecimal(tokenAmount.replace("[^0-9.]".toRegex(), ""))
            val divisor = BigDecimal(10).pow(decimals)
            amount.multiply(divisor).toLong()
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid token amount: $tokenAmount")
        }
    }

    /**
     * Shorten address for display purposes
     * @param address Full Solana address
     * @param prefixLength Length of prefix to show (default: 4)
     * @param suffixLength Length of suffix to show (default: 4)
     * @return Shortened address like "abcd...wxyz"
     */
    fun shortenAddress(address: String, prefixLength: Int = 4, suffixLength: Int = 4): String {
        if (address.length <= prefixLength + suffixLength) return address
        return "${address.take(prefixLength)}...${address.takeLast(suffixLength)}"
    }

    /**
     * Generate a QR code data string for Solana address
     * @param address Solana address
     * @param amount Optional amount in SOL
     * @param label Optional label
     * @return QR code data string
     */
    fun generateQRData(address: String, amount: Double? = null, label: String? = null): String {
        var qrData = "solana:$address"
        val params = mutableListOf<String>()

        amount?.let {
            params.add("amount=$it")
        }

        label?.let {
            params.add("label=${it.replace(" ", "%20")}")
        }

        if (params.isNotEmpty()) {
            qrData += "?" + params.joinToString("&")
        }

        return qrData
    }

    /**
     * Parse Solana QR code data
     * @param qrData QR code data string
     * @return Parsed components (address, amount, label)
     */
    data class SolanaQRData(
        val address: String,
        val amount: Double? = null,
        val label: String? = null
    )

    fun parseQRData(qrData: String): SolanaQRData? {
        if (!qrData.startsWith("solana:")) return null

        val parts = qrData.removePrefix("solana:").split("?")
        val address = parts[0]

        if (!SolanaSDK.isValidAddress(address)) return null

        var amount: Double? = null
        var label: String? = null

        if (parts.size > 1) {
            val params = parts[1].split("&")
            for (param in params) {
                val keyValue = param.split("=")
                if (keyValue.size == 2) {
                    when (keyValue[0]) {
                        "amount" -> amount = keyValue[1].toDoubleOrNull()
                        "label" -> label = keyValue[1].replace("%20", " ")
                    }
                }
            }
        }

        return SolanaQRData(address, amount, label)
    }

    /**
     * Get network display name
     * @param network Solana network
     * @return Human-readable network name
     */
    fun getNetworkDisplayName(network: SolanaNetwork): String {
        return when (network) {
            SolanaNetwork.MAINNET_BETA -> "Mainnet"
            SolanaNetwork.DEVNET -> "Devnet"
            SolanaNetwork.TESTNET -> "Testnet"
            SolanaNetwork.LOCALHOST -> "Localhost"
        }
    }

    /**
     * Get network color for UI (as hex string)
     * @param network Solana network
     * @return Hex color string
     */
    fun getNetworkColor(network: SolanaNetwork): String {
        return when (network) {
            SolanaNetwork.MAINNET_BETA -> "#00D4AA" // Green
            SolanaNetwork.DEVNET -> "#FF6B35" // Orange
            SolanaNetwork.TESTNET -> "#4285F4" // Blue
            SolanaNetwork.LOCALHOST -> "#9AA0A6" // Gray
        }
    }

    /**
     * Validate transaction signature format
     * @param signature Transaction signature string
     * @return true if valid signature format
     */
    fun isValidSignature(signature: String): Boolean {
        return signature.length in 87..89 && signature.all { char ->
            char.isLetterOrDigit() || char in listOf('/', '+', '=')
        }
    }

    /**
     * Estimate transaction size in bytes
     * @param numSignatures Number of signatures
     * @param numAccounts Number of account keys
     * @param numInstructions Number of instructions
     * @param dataSize Total instruction data size
     * @return Estimated transaction size in bytes
     */
    fun estimateTransactionSize(
        numSignatures: Int,
        numAccounts: Int,
        numInstructions: Int,
        dataSize: Int
    ): Int {
        return 64 * numSignatures + // Signatures
                3 + // Message header
                32 * numAccounts + // Account keys
                32 + // Recent blockhash
                numInstructions + // Instructions compact array length
                dataSize // Instruction data
    }

    /**
     * Calculate approximate transaction fee
     * @param signatures Number of signatures required
     * @param lamportsPerSignature Cost per signature (default: 5000)
     * @return Estimated fee in lamports
     */
    fun calculateTransactionFee(signatures: Int, lamportsPerSignature: Long = 5000L): Long {
        return signatures * lamportsPerSignature
    }

    /**
     * Generate a memo instruction data
     * @param memo Memo text
     * @return Memo instruction data
     */
    fun createMemoData(memo: String): ByteArray {
        return memo.toByteArray(Charsets.UTF_8)
    }

    /**
     * Convert public key to various formats
     */
    object KeyFormats {
        fun toBase58(publicKey: PublicKey): String = publicKey.toSolanaBase58()!!
        fun toHex(publicKey: PublicKey): String = publicKey.data().joinToString("") { "%02x".format(it) }
        fun toByteArray(publicKey: PublicKey): ByteArray = publicKey.data()
    }

    /**
     * Common Solana program IDs
     */
    object ProgramIds {
        const val SYSTEM_PROGRAM = "11111111111111111111111111111111"
        const val TOKEN_PROGRAM = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
        const val ASSOCIATED_TOKEN_PROGRAM = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"
        const val MEMO_PROGRAM = "MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr"
        const val RENT_PROGRAM = "SysvarRent111111111111111111111111111111111"
    }

    /**
     * Well-known Solana addresses
     */
    object WellKnownAddresses {
        const val VOTE_PROGRAM = "Vote111111111111111111111111111111111111111"
        const val STAKE_PROGRAM = "Stake11111111111111111111111111111111111111"
        const val CONFIG_PROGRAM = "Config1111111111111111111111111111111111111"
    }

    /**
     * Common token decimals
     */
    object CommonTokenDecimals {
        const val SOL = 9
        const val USDC = 6
        const val USDT = 6
        const val BTC = 8
        const val ETH = 18
    }
}