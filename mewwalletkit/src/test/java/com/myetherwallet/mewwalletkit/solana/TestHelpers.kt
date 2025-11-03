package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip39.BIP39
import com.myetherwallet.mewwalletkit.bip.bip44.Network
import com.myetherwallet.mewwalletkit.bip.bip44.PrivateKey
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.core.extension.decodeBase58
import com.myetherwallet.mewwalletkit.core.extension.encodeBase58String

/**
 * Shared test utilities for Solana tests.
 *
 * Provides common helper functions to reduce code duplication across test files.
 */
object TestHelpers {

    /**
     * Standard test mnemonic used across all tests.
     * WARNING: Never use this mnemonic in production!
     */
    const val TEST_MNEMONIC = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"

    /**
     * Known test recipient address for integration tests.
     */
    const val TEST_RECIPIENT_ADDRESS = "J3dxNj7nDRRqRRXuEMynDG57DkZK4jYRuv3Garmb1i99"

    /**
     * Creates test private keys from the standard test mnemonic.
     *
     * Each key is derived by modifying the seed's last byte to create
     * unique but deterministic keys for testing.
     *
     * @param count Number of keys to generate
     * @return List of PrivateKey instances for Solana network
     */
    fun createTestKeys(count: Int = 2): List<PrivateKey> {
        val bip39 = BIP39(TEST_MNEMONIC.split(" "))
        val seed = bip39.seed()!!

        return (0 until count).map { index ->
            val indexedSeed = seed.copyOf(32)
            indexedSeed[31] = (indexedSeed[31].toInt() + index).toByte()
            PrivateKey.createWithSeed(indexedSeed, Network.SOLANA)
        }
    }

    /**
     * Creates test public keys from the standard test mnemonic.
     *
     * @param count Number of keys to generate
     * @return List of PublicKey instances
     */
    fun createTestPublicKeys(count: Int = 2): List<PublicKey> {
        return createTestKeys(count).map { it.publicKey()!! }
    }

    /**
     * Creates a deterministic test blockhash.
     *
     * The blockhash is a Base58-encoded 32-byte value where each byte
     * equals its index. This provides a consistent, valid blockhash for testing.
     *
     * @return Base58-encoded blockhash string
     */
    fun createTestBlockhash(): String {
        val hashBytes = ByteArray(32) { index -> index.toByte() }
        return hashBytes.encodeBase58String("123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz")
            ?: throw IllegalStateException("Failed to encode blockhash")
    }

    /**
     * Parses a Base58-encoded Solana address into a PublicKey.
     *
     * @param address Base58-encoded Solana address string
     * @return PublicKey instance
     * @throws IllegalArgumentException if address is invalid
     */
    fun parseAddress(address: String): PublicKey {
        val alphabet = Network.SOLANA.alphabet()
            ?: throw IllegalStateException("Failed to get Solana alphabet")
        val publicKeyBytes = address.decodeBase58(alphabet)
            ?: throw IllegalArgumentException("Failed to decode address: $address")
        require(publicKeyBytes.size == 32) { "Invalid public key size: ${publicKeyBytes.size}, expected 32" }
        return PublicKey(publicKeyBytes, Network.SOLANA)
    }

    /**
     * Creates a simple test transaction with one instruction.
     *
     * @param publicKey The public key to use as fee payer and signer
     * @param blockhash The blockhash to use (defaults to createTestBlockhash())
     * @param programId The program ID for the instruction (defaults to System Program)
     * @param data The instruction data (defaults to empty)
     * @return Configured Transaction ready for signing
     */
    fun createSimpleTransaction(
        publicKey: PublicKey,
        blockhash: String = createTestBlockhash(),
        programId: PublicKey = SystemProgram.programId(),
        data: ByteArray = byteArrayOf()
    ): Transaction {
        val transaction = Transaction(
            feePayer = publicKey,
            recentBlockhash = blockhash
        )

        transaction.add(
            TransactionInstruction(
                keys = listOf(AccountMeta(publicKey, isSigner = true, isWritable = true)),
                programId = programId,
                data = data
            )
        )

        return transaction
    }

    /**
     * Creates a transfer transaction for testing.
     *
     * @param fromKey The sender's private key
     * @param toPublicKey The recipient's public key
     * @param lamports Amount to transfer in lamports
     * @param blockhash The blockhash to use (defaults to createTestBlockhash())
     * @return Configured Transaction with transfer instruction
     */
    fun createTransferTransaction(
        fromKey: PrivateKey,
        toPublicKey: PublicKey,
        lamports: ULong = 1000000uL,
        blockhash: String = createTestBlockhash()
    ): Transaction {
        val fromPublicKey = fromKey.publicKey()!!

        val transaction = Transaction(
            feePayer = fromPublicKey,
            recentBlockhash = blockhash
        )

        transaction.add(
            SystemProgram.transfer(
                fromPubkey = fromPublicKey,
                toPubkey = toPublicKey,
                lamports = lamports
            )
        )

        return transaction
    }
}
