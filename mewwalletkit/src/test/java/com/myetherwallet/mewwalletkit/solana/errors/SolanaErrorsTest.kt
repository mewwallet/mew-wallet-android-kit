package com.myetherwallet.mewwalletkit.solana.errors

import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive tests for Solana error handling
 */
class SolanaErrorsTest {

    @Test
    fun testNetworkExceptions() {
        // Test ConnectionException
        val connectionError = SolanaNetworkException.ConnectionException("Connection failed")
        assertTrue(connectionError is SolanaException)
        assertEquals("Connection failed", connectionError.message)

        // Test TimeoutException
        val timeoutError = SolanaNetworkException.TimeoutException()
        assertEquals("Network request timed out", timeoutError.message)

        // Test RpcException
        val rpcError = SolanaNetworkException.RpcException(-32000, "Node unhealthy", mapOf("slot" to 123))
        assertEquals(-32000, rpcError.errorCode)
        assertEquals("RPC Error -32000: Node unhealthy", rpcError.message)
        assertNotNull(rpcError.data)

        // Test InvalidResponseException
        val invalidResponse = SolanaNetworkException.InvalidResponseException()
        assertEquals("Invalid response format from RPC", invalidResponse.message)

        // Test NetworkUnavailableException
        val networkUnavailable = SolanaNetworkException.NetworkUnavailableException("mainnet")
        assertEquals("mainnet", networkUnavailable.network)
        assertEquals("Network mainnet is unavailable", networkUnavailable.message)
    }

    @Test
    fun testTransactionExceptions() {
        // Test InvalidTransactionException
        val invalidTx = SolanaTransactionException.InvalidTransactionException("Missing signature")
        assertEquals("Missing signature", invalidTx.message)

        // Test InsufficientFundsException
        val insufficientFunds = SolanaTransactionException.InsufficientFundsException(
            requiredAmount = 1000000L,
            availableAmount = 500000L
        )
        assertEquals(1000000L, insufficientFunds.requiredAmount)
        assertEquals(500000L, insufficientFunds.availableAmount)
        assertTrue(insufficientFunds.message?.contains("500000") ?: false)

        // Test TransactionFailedException
        val logs = listOf("Program invoke", "Program failed")
        val txFailed = SolanaTransactionException.TransactionFailedException(
            signature = "abc123",
            errorLogs = logs,
            message = "Transaction execution failed"
        )
        assertEquals("abc123", txFailed.signature)
        assertEquals(logs, txFailed.errorLogs)
        assertTrue(txFailed.getDetailedError().contains("Program invoke"))

        // Test BlockhashExpiredException
        val blockhashExpired = SolanaTransactionException.BlockhashExpiredException("abc123")
        assertEquals("abc123", blockhashExpired.blockhash)
        assertTrue(blockhashExpired.message?.contains("abc123") ?: false)

        // Test TransactionTooLargeException
        val tooLarge = SolanaTransactionException.TransactionTooLargeException(1500, 1232)
        assertEquals(1500, tooLarge.actualSize)
        assertEquals(1232, tooLarge.maxSize)

        // Test DuplicateTransactionException
        val duplicate = SolanaTransactionException.DuplicateTransactionException("xyz789")
        assertEquals("xyz789", duplicate.signature)
    }

    @Test
    fun testAccountExceptions() {
        // Test InvalidAddressException
        val invalidAddress = SolanaAccountException.InvalidAddressException("invalid_address")
        assertEquals("invalid_address", invalidAddress.address)

        // Test AccountNotFoundException
        val notFound = SolanaAccountException.AccountNotFoundException("11111111111111111111111111111111")
        assertEquals("11111111111111111111111111111111", notFound.address)

        // Test AccountNotInitializedException
        val notInitialized = SolanaAccountException.AccountNotInitializedException("token_account")
        assertEquals("token_account", notInitialized.address)

        // Test InsufficientAccountBalanceException
        val insufficientBalance = SolanaAccountException.InsufficientAccountBalanceException(
            address = "account123",
            balance = 1000L,
            required = 5000L
        )
        assertEquals("account123", insufficientBalance.address)
        assertEquals(1000L, insufficientBalance.balance)
        assertEquals(5000L, insufficientBalance.required)

        // Test AccountAlreadyExistsException
        val alreadyExists = SolanaAccountException.AccountAlreadyExistsException("existing_account")
        assertEquals("existing_account", alreadyExists.address)

        // Test InvalidKeyException
        val invalidKey = SolanaAccountException.InvalidKeyException("Invalid seed length")
        assertEquals("Invalid seed length", invalidKey.message)
    }

    @Test
    fun testTokenExceptions() {
        // Test TokenAccountNotFoundException
        val tokenNotFound = SolanaTokenException.TokenAccountNotFoundException(
            mint = "mint123",
            owner = "owner456"
        )
        assertEquals("mint123", tokenNotFound.mint)
        assertEquals("owner456", tokenNotFound.owner)

        // Test InsufficientTokenBalanceException
        val insufficientTokens = SolanaTokenException.InsufficientTokenBalanceException(
            mint = "usdc_mint",
            balance = 1000000L,
            required = 5000000L
        )
        assertEquals("usdc_mint", insufficientTokens.mint)
        assertEquals(1000000L, insufficientTokens.balance)
        assertEquals(5000000L, insufficientTokens.required)

        // Test InvalidTokenMintException
        val invalidMint = SolanaTokenException.InvalidTokenMintException("invalid_mint")
        assertEquals("invalid_mint", invalidMint.mint)

        // Test TokenAccountCreationFailedException
        val creationFailed = SolanaTokenException.TokenAccountCreationFailedException(
            mint = "mint123",
            owner = "owner456"
        )
        assertEquals("mint123", creationFailed.mint)
        assertEquals("owner456", creationFailed.owner)

        // Test TokenTransferFailedException
        val transferFailed = SolanaTokenException.TokenTransferFailedException(
            from = "from123",
            to = "to456",
            amount = 1000000L
        )
        assertEquals("from123", transferFailed.from)
        assertEquals("to456", transferFailed.to)
        assertEquals(1000000L, transferFailed.amount)

        // Test TokenApprovalFailedException
        val approvalFailed = SolanaTokenException.TokenApprovalFailedException(
            owner = "owner123",
            delegate = "delegate456",
            amount = 500000L
        )
        assertEquals("owner123", approvalFailed.owner)
        assertEquals("delegate456", approvalFailed.delegate)
        assertEquals(500000L, approvalFailed.amount)

        // Test InvalidTokenDecimalsException
        val invalidDecimals = SolanaTokenException.InvalidTokenDecimalsException(18)
        assertEquals(18, invalidDecimals.decimals)
    }

    @Test
    fun testWalletExceptions() {
        // Test InvalidMnemonicException
        val invalidMnemonic = SolanaWalletException.InvalidMnemonicException("invalid words")
        assertEquals("invalid words", invalidMnemonic.mnemonic)

        // Test KeyDerivationException
        val derivationFailed = SolanaWalletException.KeyDerivationException("m/44'/501'/0'/0'")
        assertEquals("m/44'/501'/0'/0'", derivationFailed.path)

        // Test InvalidSeedException
        val invalidSeed = SolanaWalletException.InvalidSeedException("Seed too short")
        assertEquals("Seed too short", invalidSeed.message)

        // Test WalletInitializationException
        val initFailed = SolanaWalletException.WalletInitializationException()
        assertEquals("Failed to initialize wallet", initFailed.message)
    }

    @Test
    fun testPDAExceptions() {
        // Test InvalidSeedsException
        val invalidSeeds = SolanaPDAException.InvalidSeedsException("Empty seeds list")
        assertEquals("Empty seeds list", invalidSeeds.message)

        // Test TooManySeedsException
        val tooManySeeds = SolanaPDAException.TooManySeedsException(20, 16)
        assertEquals(20, tooManySeeds.seedCount)
        assertEquals(16, tooManySeeds.maxSeeds)

        // Test SeedTooLongException
        val seedTooLong = SolanaPDAException.SeedTooLongException(64, 32)
        assertEquals(64, seedTooLong.seedLength)
        assertEquals(32, seedTooLong.maxLength)

        // Test PDADerivationFailedException
        val derivationFailed = SolanaPDAException.PDADerivationFailedException()
        assertEquals("Unable to find valid program derived address", derivationFailed.message)

        // Test InvalidBumpException
        val invalidBump = SolanaPDAException.InvalidBumpException(300)
        assertEquals(300, invalidBump.bump)
    }

    @Test
    fun testProgramExceptions() {
        val programId = "11111111111111111111111111111111"

        // Test InvalidInstructionException
        val invalidInstruction = SolanaProgramException.InvalidInstructionException(
            programId = programId,
            instructionType = 99
        )
        assertEquals(programId, invalidInstruction.programId)
        assertEquals(99, invalidInstruction.instructionType)

        // Test InsufficientAccountsException
        val insufficientAccounts = SolanaProgramException.InsufficientAccountsException(
            programId = programId,
            provided = 2,
            required = 4
        )
        assertEquals(programId, insufficientAccounts.programId)
        assertEquals(2, insufficientAccounts.provided)
        assertEquals(4, insufficientAccounts.required)

        // Test InvalidAccountPermissionsException
        val invalidPermissions = SolanaProgramException.InvalidAccountPermissionsException(
            programId = programId,
            accountKey = "account123"
        )
        assertEquals(programId, invalidPermissions.programId)
        assertEquals("account123", invalidPermissions.accountKey)

        // Test ProgramExecutionException
        val logs = listOf("Program invoke", "Custom program error: 0x1")
        val executionFailed = SolanaProgramException.ProgramExecutionException(
            programId = programId,
            errorCode = 1,
            logs = logs
        )
        assertEquals(programId, executionFailed.programId)
        assertEquals(1, executionFailed.errorCode)
        assertEquals(logs, executionFailed.logs)
        assertTrue(executionFailed.getDetailedError().contains("Error code: 1"))
        assertTrue(executionFailed.getDetailedError().contains("Program invoke"))
    }

    @Test
    fun testErrorUtils() {
        // Test parseRpcError
        val connectionError = SolanaErrorUtils.parseRpcError(-32000, "Node is unhealthy")
        assertTrue(connectionError is SolanaNetworkException.ConnectionException)

        val invalidRequestError = SolanaErrorUtils.parseRpcError(-32001, "Invalid params")
        assertTrue(invalidRequestError is SolanaNetworkException.InvalidResponseException)

        val methodNotFound = SolanaErrorUtils.parseRpcError(-32602, "Method not found")
        assertTrue(methodNotFound is SolanaNetworkException.RpcException)
        assertEquals(-32602, (methodNotFound as SolanaNetworkException.RpcException).errorCode)

        val internalError = SolanaErrorUtils.parseRpcError(-32603, "Internal error")
        assertTrue(internalError is SolanaNetworkException.RpcException)

        val unknownError = SolanaErrorUtils.parseRpcError(-99999, "Unknown error")
        assertTrue(unknownError is SolanaNetworkException.RpcException)
        assertEquals(-99999, (unknownError as SolanaNetworkException.RpcException).errorCode)

        // Test parseTransactionError
        val insufficientFundsError = SolanaErrorUtils.parseTransactionError("insufficient funds")
        assertTrue(insufficientFundsError is SolanaTransactionException.InsufficientFundsException)

        val blockhashError = SolanaErrorUtils.parseTransactionError("invalid blockhash")
        assertTrue(blockhashError is SolanaTransactionException.BlockhashExpiredException)

        val duplicateError = SolanaErrorUtils.parseTransactionError("duplicate transaction")
        assertTrue(duplicateError is SolanaTransactionException.DuplicateTransactionException)

        val tooLargeError = SolanaErrorUtils.parseTransactionError("transaction too large")
        assertTrue(tooLargeError is SolanaTransactionException.TransactionTooLargeException)

        val genericError = SolanaErrorUtils.parseTransactionError("unknown error")
        assertTrue(genericError is SolanaTransactionException.TransactionFailedException)

        // Test isRetryableError
        assertTrue(SolanaErrorUtils.isRetryableError(
            SolanaNetworkException.ConnectionException()
        ))
        assertTrue(SolanaErrorUtils.isRetryableError(
            SolanaNetworkException.TimeoutException()
        ))
        assertTrue(SolanaErrorUtils.isRetryableError(
            SolanaNetworkException.RpcException(-32000, "Node unhealthy")
        ))
        assertTrue(SolanaErrorUtils.isRetryableError(
            SolanaTransactionException.BlockhashExpiredException("abc")
        ))

        assertFalse(SolanaErrorUtils.isRetryableError(
            SolanaAccountException.InvalidAddressException("invalid")
        ))
        assertFalse(SolanaErrorUtils.isRetryableError(
            SolanaTokenException.InvalidTokenMintException("invalid")
        ))

        // Test getUserFriendlyMessage
        assertEquals(
            "Unable to connect to Solana network. Please check your internet connection.",
            SolanaErrorUtils.getUserFriendlyMessage(SolanaNetworkException.ConnectionException())
        )

        assertEquals(
            "Request timed out. Please try again.",
            SolanaErrorUtils.getUserFriendlyMessage(SolanaNetworkException.TimeoutException())
        )

        assertEquals(
            "Insufficient balance to complete this transaction.",
            SolanaErrorUtils.getUserFriendlyMessage(
                SolanaTransactionException.InsufficientFundsException(1000, 500)
            )
        )

        assertEquals(
            "Invalid Solana address format.",
            SolanaErrorUtils.getUserFriendlyMessage(
                SolanaAccountException.InvalidAddressException("invalid")
            )
        )

        assertEquals(
            "Token account not found. It may need to be created first.",
            SolanaErrorUtils.getUserFriendlyMessage(
                SolanaTokenException.TokenAccountNotFoundException("mint", "owner")
            )
        )

        assertEquals(
            "Invalid recovery phrase. Please check your mnemonic words.",
            SolanaErrorUtils.getUserFriendlyMessage(
                SolanaWalletException.InvalidMnemonicException("invalid")
            )
        )

        assertEquals(
            "Unable to generate program derived address.",
            SolanaErrorUtils.getUserFriendlyMessage(
                SolanaPDAException.PDADerivationFailedException()
            )
        )

        // Test generic error message
        val genericException = RuntimeException("Generic error")
        assertEquals(
            "Generic error",
            SolanaErrorUtils.getUserFriendlyMessage(genericException)
        )

        val nullMessageException = RuntimeException(null as String?)
        assertEquals(
            "An unexpected error occurred",
            SolanaErrorUtils.getUserFriendlyMessage(nullMessageException)
        )
    }

    @Test
    fun testExceptionInheritance() {
        // Test that all custom exceptions inherit from SolanaException
        val networkException = SolanaNetworkException.ConnectionException()
        assertTrue(networkException is SolanaException)

        val transactionException = SolanaTransactionException.InvalidTransactionException("test")
        assertTrue(transactionException is SolanaException)

        val accountException = SolanaAccountException.InvalidAddressException("test")
        assertTrue(accountException is SolanaException)

        val tokenException = SolanaTokenException.InvalidTokenMintException("test")
        assertTrue(tokenException is SolanaException)

        val walletException = SolanaWalletException.InvalidMnemonicException("test")
        assertTrue(walletException is SolanaException)

        val pdaException = SolanaPDAException.InvalidSeedsException("test")
        assertTrue(pdaException is SolanaException)

        val programException = SolanaProgramException.InvalidInstructionException("program", 1)
        assertTrue(programException is SolanaException)
    }

    @Test
    fun testExceptionCauses() {
        val rootCause = IllegalArgumentException("Root cause")

        val networkException = SolanaNetworkException.ConnectionException(
            "Connection failed",
            rootCause
        )
        assertEquals(rootCause, networkException.cause)

        val accountException = SolanaAccountException.InvalidKeyException(
            "Invalid key",
            rootCause
        )
        assertEquals(rootCause, accountException.cause)
    }

    @Test
    fun testDetailedErrorMessages() {
        // Test TransactionFailedException detailed error
        val logs = listOf("Program 11111 invoke [1]", "Transfer: insufficient funds", "Program 11111 failed")
        val txError = SolanaTransactionException.TransactionFailedException(
            signature = "abc123",
            errorLogs = logs,
            message = "Transaction failed"
        )

        val detailedMessage = txError.getDetailedError()
        assertTrue(detailedMessage.contains("Transaction failed"))
        assertTrue(detailedMessage.contains("insufficient funds"))
        assertTrue(detailedMessage.contains("Program 11111 invoke"))

        // Test TransactionFailedException without logs
        val txErrorNoLogs = SolanaTransactionException.TransactionFailedException(
            signature = "def456",
            errorLogs = emptyList(),
            message = "Simple failure"
        )
        assertEquals("Simple failure", txErrorNoLogs.getDetailedError())

        // Test ProgramExecutionException detailed error
        val programLogs = listOf("Program ABC invoke", "Custom error: 42")
        val programError = SolanaProgramException.ProgramExecutionException(
            programId = "ABC123",
            errorCode = 42,
            logs = programLogs,
            message = "Program failed"
        )

        val programDetailedMessage = programError.getDetailedError()
        assertTrue(programDetailedMessage.contains("Program failed"))
        assertTrue(programDetailedMessage.contains("Error code: 42"))
        assertTrue(programDetailedMessage.contains("Custom error: 42"))

        // Test ProgramExecutionException without error code or logs
        val programErrorSimple = SolanaProgramException.ProgramExecutionException(
            programId = "ABC123",
            errorCode = null,
            logs = emptyList(),
            message = "Simple program failure"
        )
        assertEquals("Simple program failure", programErrorSimple.getDetailedError())
    }
}