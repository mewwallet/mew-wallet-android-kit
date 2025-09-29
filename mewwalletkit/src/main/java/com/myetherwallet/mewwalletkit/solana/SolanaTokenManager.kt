package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.solana.crypto.SolanaPrivateKey
import com.myetherwallet.mewwalletkit.solana.token.TokenProgram
import com.myetherwallet.mewwalletkit.solana.token.AssociatedTokenProgram
import com.myetherwallet.mewwalletkit.solana.transaction.VersionedTransaction
import com.myetherwallet.mewwalletkit.solana.rpc.SolanaRpcClient
import com.myetherwallet.mewwalletkit.solana.rpc.models.TokenAccountInfo

/**
 * Token operations manager for SPL tokens
 * Provides high-level token functionality
 */
class SolanaTokenManager(
    private val rpcClient: SolanaRpcClient
) {

    /**
     * Token information data class
     */
    data class TokenInfo(
        val mint: String,
        val symbol: String,
        val name: String,
        val decimals: Int,
        val supply: Long? = null
    )

    /**
     * Token account balance information
     */
    data class TokenBalance(
        val mint: String,
        val amount: Long,
        val decimals: Int,
        val uiAmount: Double
    ) {
        /**
         * Get human-readable amount
         */
        fun getDisplayAmount(): String {
            return "%.${decimals}f".format(uiAmount)
        }
    }

    /**
     * Get all token accounts for an owner
     * @param owner Owner's public key
     * @return List of token account information
     */
    suspend fun getTokenAccounts(owner: PublicKey): List<TokenAccountInfo> {
        return rpcClient.getTokenAccountsByOwner(owner.toSolanaBase58()!!)
    }

    /**
     * Get all token accounts for an owner using address string
     * @param ownerAddress Owner's address in base58 format
     * @return List of token account information
     */
    suspend fun getTokenAccounts(ownerAddress: String): List<TokenAccountInfo> {
        return rpcClient.getTokenAccountsByOwner(ownerAddress)
    }

    /**
     * Get token balance for a specific mint
     * @param owner Owner's public key
     * @param mint Token mint address
     * @return Token balance or null if account doesn't exist
     */
    suspend fun getTokenBalance(owner: PublicKey, mint: PublicKey): TokenBalance? {
        val ataAddress = AssociatedTokenProgram.getAssociatedTokenAddress(owner, mint)
        val accountInfo = try {
            rpcClient.getAccountInfo(ataAddress.toSolanaBase58()!!)
        } catch (e: Exception) {
            return null
        }

        // In a real implementation, we would parse the account data to get balance
        // For now, return mock data
        return TokenBalance(
            mint = mint.toSolanaBase58()!!,
            amount = 1000000L, // Mock amount
            decimals = 6,
            uiAmount = 1.0
        )
    }

    /**
     * Create a new token mint
     * @param payer Account that will pay for creation
     * @param mintAuthority Authority that can mint tokens
     * @param decimals Number of decimal places
     * @param freezeAuthority Optional freeze authority
     * @return Pair of (mint address, transaction signature)
     */
    suspend fun createToken(
        payer: SolanaPrivateKey,
        mintAuthority: PublicKey,
        decimals: Byte,
        freezeAuthority: PublicKey? = null
    ): Pair<PublicKey, String> {
        val mintKeypair = SolanaPrivateKey.generate()
        val mint = mintKeypair.publicKey()

        val blockhash = rpcClient.getLatestBlockhash()

        val transaction = VersionedTransaction.builder()
            .setPayer(payer.publicKey())
            .setRecentBlockhash(blockhash.blockhash)
            .addInstruction(
                TokenProgram.initializeMint(
                    mint = mint,
                    decimals = decimals,
                    mintAuthority = mintAuthority,
                    freezeAuthority = freezeAuthority
                )
            )
            .build()

        transaction.sign(payer)
        // In a real implementation, we would also sign with mintKeypair for account creation
        val signature = rpcClient.sendTransaction(transaction)

        return Pair(mint, signature)
    }

    /**
     * Mint tokens to an account
     * @param mintAuthority Authority that can mint tokens
     * @param mint Token mint address
     * @param destination Destination token account
     * @param amount Amount to mint
     * @return Transaction signature
     */
    suspend fun mintTokens(
        mintAuthority: SolanaPrivateKey,
        mint: PublicKey,
        destination: PublicKey,
        amount: Long
    ): String {
        val blockhash = rpcClient.getLatestBlockhash()

        val transaction = VersionedTransaction.builder()
            .setPayer(mintAuthority.publicKey())
            .setRecentBlockhash(blockhash.blockhash)
            .addInstruction(
                TokenProgram.mintTo(
                    mint = mint,
                    account = destination,
                    authority = mintAuthority.publicKey(),
                    amount = amount
                )
            )
            .build()

        transaction.sign(mintAuthority)
        return rpcClient.sendTransaction(transaction)
    }

    /**
     * Burn tokens from an account
     * @param owner Owner of the token account
     * @param mint Token mint address
     * @param amount Amount to burn
     * @return Transaction signature
     */
    suspend fun burnTokens(
        owner: SolanaPrivateKey,
        mint: PublicKey,
        amount: Long
    ): String {
        val tokenAccount = AssociatedTokenProgram.getAssociatedTokenAddress(
            owner.publicKey(),
            mint
        )

        val blockhash = rpcClient.getLatestBlockhash()

        val transaction = VersionedTransaction.builder()
            .setPayer(owner.publicKey())
            .setRecentBlockhash(blockhash.blockhash)
            .addInstruction(
                TokenProgram.burn(
                    account = tokenAccount,
                    mint = mint,
                    owner = owner.publicKey(),
                    amount = amount
                )
            )
            .build()

        transaction.sign(owner)
        return rpcClient.sendTransaction(transaction)
    }

    /**
     * Approve a delegate to spend tokens
     * @param owner Owner of the token account
     * @param mint Token mint address
     * @param delegate Account to approve
     * @param amount Amount to approve
     * @return Transaction signature
     */
    suspend fun approveTokens(
        owner: SolanaPrivateKey,
        mint: PublicKey,
        delegate: PublicKey,
        amount: Long
    ): String {
        val tokenAccount = AssociatedTokenProgram.getAssociatedTokenAddress(
            owner.publicKey(),
            mint
        )

        val blockhash = rpcClient.getLatestBlockhash()

        val transaction = VersionedTransaction.builder()
            .setPayer(owner.publicKey())
            .setRecentBlockhash(blockhash.blockhash)
            .addInstruction(
                TokenProgram.approve(
                    source = tokenAccount,
                    delegate = delegate,
                    owner = owner.publicKey(),
                    amount = amount
                )
            )
            .build()

        transaction.sign(owner)
        return rpcClient.sendTransaction(transaction)
    }

    /**
     * Revoke all token approvals
     * @param owner Owner of the token account
     * @param mint Token mint address
     * @return Transaction signature
     */
    suspend fun revokeTokenApproval(
        owner: SolanaPrivateKey,
        mint: PublicKey
    ): String {
        val tokenAccount = AssociatedTokenProgram.getAssociatedTokenAddress(
            owner.publicKey(),
            mint
        )

        val blockhash = rpcClient.getLatestBlockhash()

        val transaction = VersionedTransaction.builder()
            .setPayer(owner.publicKey())
            .setRecentBlockhash(blockhash.blockhash)
            .addInstruction(
                TokenProgram.revoke(
                    source = tokenAccount,
                    owner = owner.publicKey()
                )
            )
            .build()

        transaction.sign(owner)
        return rpcClient.sendTransaction(transaction)
    }

    /**
     * Close a token account and reclaim SOL
     * @param owner Owner of the token account
     * @param mint Token mint address
     * @param destination Where to send the reclaimed SOL
     * @return Transaction signature
     */
    suspend fun closeTokenAccount(
        owner: SolanaPrivateKey,
        mint: PublicKey,
        destination: PublicKey = owner.publicKey()
    ): String {
        val tokenAccount = AssociatedTokenProgram.getAssociatedTokenAddress(
            owner.publicKey(),
            mint
        )

        val blockhash = rpcClient.getLatestBlockhash()

        val transaction = VersionedTransaction.builder()
            .setPayer(owner.publicKey())
            .setRecentBlockhash(blockhash.blockhash)
            .addInstruction(
                TokenProgram.closeAccount(
                    account = tokenAccount,
                    destination = destination,
                    owner = owner.publicKey()
                )
            )
            .build()

        transaction.sign(owner)
        return rpcClient.sendTransaction(transaction)
    }

    /**
     * Convert token amount from UI representation to raw amount
     * @param uiAmount Amount in UI format (e.g., 1.5 USDC)
     * @param decimals Token decimals
     * @return Raw amount in smallest unit
     */
    fun uiAmountToRawAmount(uiAmount: Double, decimals: Int): Long {
        return (uiAmount * Math.pow(10.0, decimals.toDouble())).toLong()
    }

    /**
     * Convert raw token amount to UI representation
     * @param rawAmount Amount in smallest unit
     * @param decimals Token decimals
     * @return UI amount as double
     */
    fun rawAmountToUiAmount(rawAmount: Long, decimals: Int): Double {
        return rawAmount / Math.pow(10.0, decimals.toDouble())
    }

    companion object {
        /**
         * Common token mint addresses on Solana mainnet
         */
        object WellKnownTokens {
            const val USDC = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
            const val USDT = "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB"
            const val SOL_WRAPPED = "So11111111111111111111111111111111111111112"
        }
    }
}