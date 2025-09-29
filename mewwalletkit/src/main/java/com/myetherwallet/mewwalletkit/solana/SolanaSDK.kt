package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.bip.bip44.PrivateKey
import com.myetherwallet.mewwalletkit.bip.bip44.Network
import com.myetherwallet.mewwalletkit.solana.rpc.SolanaNetwork
import com.myetherwallet.mewwalletkit.solana.rpc.SolanaRpcClient
import com.myetherwallet.mewwalletkit.solana.rpc.models.Commitment
import com.myetherwallet.mewwalletkit.solana.token.TokenProgram
import com.myetherwallet.mewwalletkit.solana.token.AssociatedTokenProgram
import com.myetherwallet.mewwalletkit.solana.transaction.VersionedTransaction
import com.myetherwallet.mewwalletkit.solana.program.SystemProgram

/**
 * High-level Solana SDK for easy blockchain interactions
 * Provides simplified APIs for common operations
 */
class SolanaSDK private constructor(
    val rpcClient: SolanaRpcClient,
    val network: SolanaNetwork
) {

    /**
     * Get SOL balance for an address
     * @param address Solana address in base58 format
     * @return Balance in lamports
     */
    suspend fun getBalance(address: String): Long {
        return rpcClient.getBalance(address)
    }

    /**
     * Get SOL balance for a public key
     * @param publicKey Public key object
     * @return Balance in lamports
     */
    suspend fun getBalance(publicKey: PublicKey): Long {
        return getBalance(publicKey.toSolanaBase58()!!)
    }

    /**
     * Transfer SOL from one account to another
     * @param from Sender's private key (must be Network.SOLANA)
     * @param to Recipient's public key
     * @param lamports Amount to transfer in lamports
     * @return Transaction signature
     */
    suspend fun transferSOL(from: PrivateKey, to: PublicKey, lamports: Long): String {
        require(from.network == Network.SOLANA) { "Private key must be for Solana network" }
        val blockhash = rpcClient.getLatestBlockhash()

        val transaction = VersionedTransaction.builder()
            .setPayer(from.publicKey())
            .setRecentBlockhash(blockhash.blockhash)
            .addInstruction(SystemProgram.transfer(from.publicKey(), to, lamports))
            .build()

        transaction.sign(from)
        return rpcClient.sendTransaction(transaction)
    }

    /**
     * Transfer SOL using string addresses
     * @param from Sender's private key (must be Network.SOLANA)
     * @param toAddress Recipient's address in base58 format
     * @param lamports Amount to transfer in lamports
     * @return Transaction signature
     */
    suspend fun transferSOL(from: PrivateKey, toAddress: String, lamports: Long): String {
        require(from.network == Network.SOLANA) { "Private key must be for Solana network" }
        val to = PublicKey.fromSolanaBase58(toAddress)
        return transferSOL(from, to, lamports)
    }

    /**
     * Get Associated Token Account address for a token
     * @param owner Owner's public key
     * @param mint Token mint address
     * @return ATA address
     */
    fun getAssociatedTokenAddress(owner: PublicKey, mint: PublicKey): PublicKey {
        return AssociatedTokenProgram.getAssociatedTokenAddress(owner, mint)
    }

    /**
     * Get Associated Token Account address using string addresses
     * @param ownerAddress Owner's address in base58 format
     * @param mintAddress Token mint address in base58 format
     * @return ATA address in base58 format
     */
    fun getAssociatedTokenAddress(ownerAddress: String, mintAddress: String): String {
        val owner = PublicKey.fromSolanaBase58(ownerAddress)
        val mint = PublicKey.fromSolanaBase58(mintAddress)
        return getAssociatedTokenAddress(owner, mint).toSolanaBase58()!!
    }

    /**
     * Create Associated Token Account
     * @param payer Account that will pay for creation
     * @param owner Owner of the new token account
     * @param mint Token mint
     * @return Transaction signature
     */
    suspend fun createAssociatedTokenAccount(
        payer: PrivateKey,
        owner: PublicKey,
        mint: PublicKey
    ): String {
        require(payer.network == Network.SOLANA) { "Payer key must be for Solana network" }
        val blockhash = rpcClient.getLatestBlockhash()

        val transaction = VersionedTransaction.builder()
            .setPayer(payer.publicKey())
            .setRecentBlockhash(blockhash.blockhash)
            .addInstruction(
                AssociatedTokenProgram.createAssociatedTokenAccountIdempotent(
                    payer = payer.publicKey(),
                    owner = owner,
                    mint = mint
                )
            )
            .build()

        transaction.sign(payer)
        return rpcClient.sendTransaction(transaction)
    }

    /**
     * Transfer SPL tokens
     * @param from Sender's private key
     * @param to Recipient's public key
     * @param mint Token mint address
     * @param amount Amount to transfer (in token's smallest unit)
     * @return Transaction signature
     */
    suspend fun transferToken(
        from: PrivateKey,
        to: PublicKey,
        mint: PublicKey,
        amount: Long
    ): String {
        require(from.network == Network.SOLANA) { "From key must be for Solana network" }
        val fromAta = getAssociatedTokenAddress(from.publicKey(), mint)
        val toAta = getAssociatedTokenAddress(to, mint)

        val blockhash = rpcClient.getLatestBlockhash()

        val instructions = mutableListOf(
            // Create recipient ATA if needed
            AssociatedTokenProgram.createAssociatedTokenAccountIdempotent(
                payer = from.publicKey(),
                owner = to,
                mint = mint
            ),
            // Transfer tokens
            TokenProgram.transfer(
                source = fromAta,
                destination = toAta,
                owner = from.publicKey(),
                amount = amount
            )
        )

        val transaction = VersionedTransaction.builder()
            .setPayer(from.publicKey())
            .setRecentBlockhash(blockhash.blockhash)
            .addInstructions(instructions)
            .build()

        transaction.sign(from)
        return rpcClient.sendTransaction(transaction)
    }

    /**
     * Transfer SPL tokens using string addresses
     * @param from Sender's private key
     * @param toAddress Recipient's address in base58 format
     * @param mintAddress Token mint address in base58 format
     * @param amount Amount to transfer
     * @return Transaction signature
     */
    suspend fun transferToken(
        from: PrivateKey,
        toAddress: String,
        mintAddress: String,
        amount: Long
    ): String {
        require(from.network == Network.SOLANA) { "From key must be for Solana network" }
        val to = PublicKey.fromSolanaBase58(toAddress)
        val mint = PublicKey.fromSolanaBase58(mintAddress)
        return transferToken(from, to, mint, amount)
    }

    /**
     * Simulate a transaction before sending
     * @param transaction Transaction to simulate
     * @return Simulation result with logs and errors
     */
    suspend fun simulateTransaction(transaction: VersionedTransaction) =
        rpcClient.simulateTransaction(transaction)

    /**
     * Get transaction status
     * @param signature Transaction signature
     * @return Transaction status or null if not found
     */
    suspend fun getTransactionStatus(signature: String) =
        rpcClient.getTransactionStatus(signature)

    /**
     * Get current network information
     */
    suspend fun getNetworkInfo() = rpcClient.getClusterInfo()

    /**
     * Get minimum balance required for rent exemption
     * @param dataLength Size of account data in bytes
     * @return Minimum balance in lamports
     */
    suspend fun getMinimumBalanceForRentExemption(dataLength: Long) =
        rpcClient.getMinimumBalanceForRentExemption(dataLength)

    companion object {
        /**
         * Create a new Solana SDK instance
         * @param network Solana network to connect to
         * @return SolanaSDK instance
         */
        fun create(network: SolanaNetwork = SolanaNetwork.DEVNET): SolanaSDK {
            val rpcClient = SolanaRpcClient.builder()
                .setNetwork(network)
                .setTimeoutMs(30000L)
                .setRetryAttempts(3)
                .build()

            return SolanaSDK(rpcClient, network)
        }

        /**
         * Create a custom SDK instance with specific RPC client
         * @param rpcClient Custom RPC client
         * @param network Network being used
         * @return SolanaSDK instance
         */
        fun create(rpcClient: SolanaRpcClient, network: SolanaNetwork): SolanaSDK {
            return SolanaSDK(rpcClient, network)
        }

        /**
         * Validate if an address is a valid Solana address
         * @param address Address string to validate
         * @return true if valid Solana address format
         */
        fun isValidAddress(address: String): Boolean {
            return try {
                PublicKey.fromSolanaBase58(address)
                true
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Convert lamports to SOL
         * @param lamports Amount in lamports
         * @return Amount in SOL
         */
        fun lamportsToSOL(lamports: Long): Double {
            return lamports / 1_000_000_000.0
        }

        /**
         * Convert SOL to lamports
         * @param sol Amount in SOL
         * @return Amount in lamports
         */
        fun solToLamports(sol: Double): Long {
            return (sol * 1_000_000_000).toLong()
        }
    }
}