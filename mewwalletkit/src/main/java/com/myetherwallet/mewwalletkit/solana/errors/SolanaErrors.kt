package com.myetherwallet.mewwalletkit.solana.errors

/**
 * Base class for all Solana-related exceptions
 */
sealed class SolanaException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Network and RPC related errors
 */
sealed class SolanaNetworkException(
    message: String,
    cause: Throwable? = null
) : SolanaException(message, cause) {

    class ConnectionException(
        message: String = "Failed to connect to Solana network",
        cause: Throwable? = null
    ) : SolanaNetworkException(message, cause)

    class TimeoutException(
        message: String = "Network request timed out",
        cause: Throwable? = null
    ) : SolanaNetworkException(message, cause)

    class RpcException(
        val errorCode: Int,
        message: String,
        val data: Any? = null
    ) : SolanaNetworkException("RPC Error $errorCode: $message")

    class InvalidResponseException(
        message: String = "Invalid response format from RPC",
        cause: Throwable? = null
    ) : SolanaNetworkException(message, cause)

    class NetworkUnavailableException(
        val network: String,
        message: String = "Network $network is unavailable"
    ) : SolanaNetworkException(message)
}

/**
 * Transaction related errors
 */
sealed class SolanaTransactionException(
    message: String,
    cause: Throwable? = null
) : SolanaException(message, cause) {

    class InvalidTransactionException(
        message: String,
        cause: Throwable? = null
    ) : SolanaTransactionException(message, cause)

    class InsufficientFundsException(
        val requiredAmount: Long,
        val availableAmount: Long,
        message: String = "Insufficient funds: required $requiredAmount lamports, available $availableAmount"
    ) : SolanaTransactionException(message)

    class TransactionFailedException(
        val signature: String?,
        val errorLogs: List<String> = emptyList(),
        message: String = "Transaction failed"
    ) : SolanaTransactionException(message) {

        fun getDetailedError(): String {
            return if (errorLogs.isNotEmpty()) {
                "${message ?: "Transaction failed"}: ${errorLogs.joinToString(", ")}"
            } else {
                message ?: "Transaction failed"
            }
        }
    }

    class BlockhashExpiredException(
        val blockhash: String,
        message: String = "Blockhash has expired: $blockhash"
    ) : SolanaTransactionException(message)

    class TransactionTooLargeException(
        val actualSize: Int,
        val maxSize: Int = 1232,
        message: String = "Transaction too large: $actualSize bytes (max: $maxSize)"
    ) : SolanaTransactionException(message)

    class DuplicateTransactionException(
        val signature: String,
        message: String = "Duplicate transaction: $signature"
    ) : SolanaTransactionException(message)
}

/**
 * Account and key related errors
 */
sealed class SolanaAccountException(
    message: String,
    cause: Throwable? = null
) : SolanaException(message, cause) {

    class InvalidAddressException(
        val address: String,
        message: String = "Invalid Solana address: $address"
    ) : SolanaAccountException(message)

    class AccountNotFoundException(
        val address: String,
        message: String = "Account not found: $address"
    ) : SolanaAccountException(message)

    class AccountNotInitializedException(
        val address: String,
        message: String = "Account not initialized: $address"
    ) : SolanaAccountException(message)

    class InsufficientAccountBalanceException(
        val address: String,
        val balance: Long,
        val required: Long,
        message: String = "Insufficient balance in account $address: $balance lamports (required: $required)"
    ) : SolanaAccountException(message)

    class AccountAlreadyExistsException(
        val address: String,
        message: String = "Account already exists: $address"
    ) : SolanaAccountException(message)

    class InvalidKeyException(
        message: String = "Invalid private or public key",
        cause: Throwable? = null
    ) : SolanaAccountException(message, cause)
}

/**
 * Token related errors
 */
sealed class SolanaTokenException(
    message: String,
    cause: Throwable? = null
) : SolanaException(message, cause) {

    class TokenAccountNotFoundException(
        val mint: String,
        val owner: String,
        message: String = "Token account not found for mint $mint and owner $owner"
    ) : SolanaTokenException(message)

    class InsufficientTokenBalanceException(
        val mint: String,
        val balance: Long,
        val required: Long,
        message: String = "Insufficient token balance: $balance (required: $required) for mint $mint"
    ) : SolanaTokenException(message)

    class InvalidTokenMintException(
        val mint: String,
        message: String = "Invalid token mint: $mint"
    ) : SolanaTokenException(message)

    class TokenAccountCreationFailedException(
        val mint: String,
        val owner: String,
        message: String = "Failed to create token account for mint $mint and owner $owner",
        cause: Throwable? = null
    ) : SolanaTokenException(message, cause)

    class TokenTransferFailedException(
        val from: String,
        val to: String,
        val amount: Long,
        message: String = "Failed to transfer $amount tokens from $from to $to",
        cause: Throwable? = null
    ) : SolanaTokenException(message, cause)

    class TokenApprovalFailedException(
        val owner: String,
        val delegate: String,
        val amount: Long,
        message: String = "Failed to approve $amount tokens for delegate $delegate by owner $owner",
        cause: Throwable? = null
    ) : SolanaTokenException(message, cause)

    class InvalidTokenDecimalsException(
        val decimals: Int,
        message: String = "Invalid token decimals: $decimals (must be 0-9)"
    ) : SolanaTokenException(message)
}

/**
 * Wallet and mnemonic related errors
 */
sealed class SolanaWalletException(
    message: String,
    cause: Throwable? = null
) : SolanaException(message, cause) {

    class InvalidMnemonicException(
        val mnemonic: String,
        message: String = "Invalid BIP39 mnemonic phrase",
        cause: Throwable? = null
    ) : SolanaWalletException(message, cause)

    class KeyDerivationException(
        val path: String,
        message: String = "Failed to derive key at path: $path",
        cause: Throwable? = null
    ) : SolanaWalletException(message, cause)

    class InvalidSeedException(
        message: String = "Invalid seed data",
        cause: Throwable? = null
    ) : SolanaWalletException(message, cause)

    class WalletInitializationException(
        message: String = "Failed to initialize wallet",
        cause: Throwable? = null
    ) : SolanaWalletException(message, cause)
}

/**
 * Program Derived Address errors
 */
sealed class SolanaPDAException(
    message: String,
    cause: Throwable? = null
) : SolanaException(message, cause) {

    class InvalidSeedsException(
        message: String = "Invalid seeds for PDA derivation",
        cause: Throwable? = null
    ) : SolanaPDAException(message, cause)

    class TooManySeedsException(
        val seedCount: Int,
        val maxSeeds: Int = 16,
        message: String = "Too many seeds: $seedCount (maximum: $maxSeeds)"
    ) : SolanaPDAException(message)

    class SeedTooLongException(
        val seedLength: Int,
        val maxLength: Int = 32,
        message: String = "Seed too long: $seedLength bytes (maximum: $maxLength)"
    ) : SolanaPDAException(message)

    class PDADerivationFailedException(
        message: String = "Unable to find valid program derived address",
        cause: Throwable? = null
    ) : SolanaPDAException(message, cause)

    class InvalidBumpException(
        val bump: Int,
        message: String = "Invalid bump seed: $bump (must be 0-255)"
    ) : SolanaPDAException(message)
}

/**
 * Program-specific errors
 */
sealed class SolanaProgramException(
    val programId: String,
    message: String,
    cause: Throwable? = null
) : SolanaException(message, cause) {

    class InvalidInstructionException(
        programId: String,
        val instructionType: Int,
        message: String = "Invalid instruction $instructionType for program $programId"
    ) : SolanaProgramException(programId, message)

    class InsufficientAccountsException(
        programId: String,
        val provided: Int,
        val required: Int,
        message: String = "Insufficient accounts for program $programId: provided $provided, required $required"
    ) : SolanaProgramException(programId, message)

    class InvalidAccountPermissionsException(
        programId: String,
        val accountKey: String,
        message: String = "Invalid account permissions for $accountKey in program $programId"
    ) : SolanaProgramException(programId, message)

    class ProgramExecutionException(
        programId: String,
        val errorCode: Int?,
        val logs: List<String> = emptyList(),
        message: String = "Program execution failed"
    ) : SolanaProgramException(programId, message) {

        fun getDetailedError(): String {
            val errorInfo = mutableListOf<String>()
            errorCode?.let { errorInfo.add("Error code: $it") }
            if (logs.isNotEmpty()) {
                errorInfo.add("Logs: ${logs.joinToString(", ")}")
            }

            return if (errorInfo.isNotEmpty()) {
                "${message ?: "Program execution failed"} (${errorInfo.joinToString(", ")})"
            } else {
                message ?: "Program execution failed"
            }
        }
    }
}

/**
 * Utility functions for error handling
 */
object SolanaErrorUtils {

    /**
     * Convert RPC error response to appropriate exception
     */
    fun parseRpcError(errorCode: Int, message: String, data: Any? = null): SolanaNetworkException {
        return when (errorCode) {
            -32000 -> SolanaNetworkException.ConnectionException("Node is unhealthy: $message")
            -32001 -> SolanaNetworkException.InvalidResponseException("Invalid request: $message")
            -32602 -> SolanaNetworkException.RpcException(errorCode, "Method not found: $message", data)
            -32603 -> SolanaNetworkException.RpcException(errorCode, "Internal error: $message", data)
            else -> SolanaNetworkException.RpcException(errorCode, message, data)
        }
    }

    /**
     * Parse transaction error from simulation result
     */
    fun parseTransactionError(error: String, logs: List<String> = emptyList()): SolanaTransactionException {
        return when {
            error.contains("insufficient", ignoreCase = true) -> {
                if (error.contains("funds", ignoreCase = true) || error.contains("balance", ignoreCase = true)) {
                    SolanaTransactionException.InsufficientFundsException(0, 0, error)
                } else {
                    SolanaTransactionException.TransactionFailedException(null, logs, error)
                }
            }
            error.contains("blockhash", ignoreCase = true) -> {
                SolanaTransactionException.BlockhashExpiredException("", error)
            }
            error.contains("duplicate", ignoreCase = true) -> {
                SolanaTransactionException.DuplicateTransactionException("", error)
            }
            error.contains("too large", ignoreCase = true) -> {
                SolanaTransactionException.TransactionTooLargeException(0, 1232, error)
            }
            else -> SolanaTransactionException.TransactionFailedException(null, logs, error)
        }
    }

    /**
     * Check if exception is retryable
     */
    fun isRetryableError(exception: Throwable): Boolean {
        return when (exception) {
            is SolanaNetworkException.ConnectionException,
            is SolanaNetworkException.TimeoutException,
            is SolanaNetworkException.NetworkUnavailableException -> true
            is SolanaNetworkException.RpcException -> exception.errorCode in listOf(-32000, -32603)
            is SolanaTransactionException.BlockhashExpiredException -> true
            else -> false
        }
    }

    /**
     * Get user-friendly error message
     */
    fun getUserFriendlyMessage(exception: Throwable): String {
        return when (exception) {
            is SolanaNetworkException.ConnectionException ->
                "Unable to connect to Solana network. Please check your internet connection."
            is SolanaNetworkException.TimeoutException ->
                "Request timed out. Please try again."
            is SolanaTransactionException.InsufficientFundsException ->
                "Insufficient balance to complete this transaction."
            is SolanaAccountException.InvalidAddressException ->
                "Invalid Solana address format."
            is SolanaTokenException.TokenAccountNotFoundException ->
                "Token account not found. It may need to be created first."
            is SolanaWalletException.InvalidMnemonicException ->
                "Invalid recovery phrase. Please check your mnemonic words."
            is SolanaPDAException.PDADerivationFailedException ->
                "Unable to generate program derived address."
            else -> exception.message ?: "An unexpected error occurred"
        }
    }
}