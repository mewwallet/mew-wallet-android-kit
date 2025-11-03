package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.PrivateKey
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.core.extension.signSolanaMessage
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer

/**
 * A Solana transaction in the process of being built.
 *
 * This class uses a builder pattern with mutable state. Instructions and signatures
 * can be added incrementally before compilation and signing.
 *
 * Lifecycle:
 * 1. Create transaction with fee payer and blockhash
 * 2. Add instructions
 * 3. Compile to Message (automatic during signing)
 * 4. Sign with required signers
 * 5. Serialize for broadcast
 *
 * @property feePayer The account that will pay transaction fees (defaults to first signer if not set)
 * @property recentBlockhash Recent blockhash for transaction expiry (required before signing)
 */
class Transaction(
    var feePayer: PublicKey? = null,
    var recentBlockhash: String? = null
) {
    private val signatures: MutableList<SignaturePubkeyPair> = mutableListOf()
    private val instructions: MutableList<TransactionInstruction> = mutableListOf()
    private var cachedMessage: Message? = null

    /**
     * Adds an instruction to this transaction.
     * Invalidates the cached compiled message.
     */
    fun add(instruction: TransactionInstruction) {
        instructions.add(instruction)
        cachedMessage = null
    }

    /**
     * Adds multiple instructions to this transaction.
     * Invalidates the cached compiled message.
     */
    fun add(vararg instructions: TransactionInstruction) {
        instructions.forEach { add(it) }
    }

    /**
     * Returns an immutable copy of the current instructions.
     */
    fun getInstructions(): List<TransactionInstruction> = instructions.toList()

    /**
     * Returns an immutable copy of the current signatures.
     */
    fun getSignatures(): List<SignaturePubkeyPair> = signatures.toList()

    /**
     * Compiles the transaction into an immutable Message.
     *
     * This process:
     * 1. Validates recentBlockhash and feePayer are set
     * 2. Collects all accounts from instructions (including program IDs)
     * 3. Deduplicates and merges account flags
     * 4. Sorts accounts in Solana's required order
     * 5. Generates MessageHeader with account counts
     * 6. Compiles instructions (replaces pubkeys with indices)
     *
     * The result is cached until the transaction is modified.
     *
     * @throws IllegalStateException if recentBlockhash is null
     * @throws IllegalStateException if feePayer cannot be determined
     * @throws IllegalArgumentException if feePayer is not a signer
     * @return The compiled message ready for signing
     */
    fun compileMessage(): Message {
        // Return cached message if available
        cachedMessage?.let { return it }

        // Validate required fields
        val blockhash = recentBlockhash
            ?: throw IllegalStateException("recentBlockhash must be set before compiling message")

        // Determine fee payer (use explicit feePayer or first signature)
        val payer = feePayer ?: signatures.firstOrNull()?.publicKey
            ?: throw IllegalStateException("feePayer must be set or transaction must have at least one signature")

        // Collect all accounts from instructions
        val allAccounts = CompiledKeys.collectAccounts(instructions)

        // Deduplicate and merge flags
        val deduplicated = CompiledKeys.deduplicateAndMerge(allAccounts)

        // Sort accounts and generate header
        val (sortedKeys, header) = CompiledKeys.sortAndCreateHeader(deduplicated, payer)

        // Compile instructions
        val compiledInstructions = CompiledKeys.compileInstructions(instructions, sortedKeys)

        // Create message
        val message = Message(
            header = header,
            accountKeys = sortedKeys,
            recentBlockhash = blockhash,
            instructions = compiledInstructions
        )

        // Populate signatures array if empty (null signatures for unsigned)
        if (signatures.isEmpty()) {
            // Create null signature slots for all required signers
            val numSigners = header.numRequiredSignatures.toInt()
            for (i in 0 until numSigners) {
                signatures.add(SignaturePubkeyPair(
                    signature = null, // unsigned
                    publicKey = sortedKeys[i] // First N keys are signers
                ))
            }
        }

        // Cache the compiled message
        cachedMessage = message

        return message
    }

    /**
     * Signs the transaction with the provided signers.
     *
     * This method:
     * 1. Clears any existing signatures
     * 2. Compiles the message (populates signature slots for ALL required signers)
     * 3. Deduplicates provided signers by public key
     * 4. Signs the message with each provided signer's Ed25519 private key
     *
     * Note: This method resets ALL signatures. Use partialSign() to add signatures
     * without clearing existing ones.
     *
     * @param signers List of private keys to sign with (must include fee payer)
     * @throws IllegalStateException if message compilation fails
     * @throws IllegalArgumentException if any required signer is missing or keys are invalid
     */
    fun sign(signers: List<PrivateKey>) {
        require(signers.isNotEmpty()) { "At least one signer is required" }

        // 1. Clear existing signatures and let compileMessage() populate slots
        signatures.clear()

        // 2. Compile message (automatically populates signature slots for all required signers)
        val message = compileMessage()

        // 3. Deduplicate signers by public key
        val uniqueSigners = signers.distinctBy { it.publicKey() }

        // 4. Sign with each signer
        partialSignInternal(message, uniqueSigners)
    }

    /**
     * Partially signs the transaction with the provided signers.
     *
     * Does not replace existing signatures, only adds/updates signatures for
     * the provided signers. Useful for multi-signature workflows where signatures
     * are collected incrementally.
     *
     * @param signers List of private keys to sign with
     * @throws IllegalStateException if message compilation fails
     * @throws IllegalArgumentException if keys are invalid
     */
    fun partialSign(signers: List<PrivateKey>) {
        require(signers.isNotEmpty()) { "At least one signer is required" }

        // Deduplicate signers by public key
        val uniqueSigners = signers.distinctBy { it.publicKey() }

        // Compile message to ensure signature array is populated
        val message = compileMessage()

        // Sign with provided signers (preserves existing signatures)
        partialSignInternal(message, uniqueSigners)
    }

    /**
     * Adds an external signature to the transaction.
     *
     * Useful when a signature is created externally (e.g., by a hardware wallet
     * or remote signer) and needs to be added to the transaction.
     *
     * @param pubkey The public key that created the signature
     * @param signature The 64-byte Ed25519 signature
     * @throws IllegalStateException if message compilation fails
     * @throws IllegalArgumentException if signature is invalid or pubkey not found
     */
    fun addSignature(pubkey: PublicKey, signature: ByteArray) {
        // Compile message to ensure signature slots are populated
        compileMessage()

        // Add the signature
        addSignatureInternal(pubkey, signature)
    }

    /**
     * Serializes the transaction for broadcast to the network.
     *
     * Wire format:
     * - Compact array of signatures (64 bytes each, zeros if unsigned)
     * - Message (header + accounts + blockhash + instructions)
     *
     * @param requireAllSignatures Whether to fail if any signature is missing
     * @param verifySignatures Whether to verify Ed25519 signatures (Phase 5)
     * @return Serialized transaction bytes ready for broadcast
     * @throws IllegalStateException if requireAllSignatures is true and any signature is missing
     */
    fun serialize(requireAllSignatures: Boolean = true, verifySignatures: Boolean = true): ByteArray {
        // Compile message if not already done
        val message = compileMessage()

        // Check for missing signatures if required
        if (requireAllSignatures) {
            val missingSignatures = signatures.filter { it.signature == null }
            if (missingSignatures.isNotEmpty()) {
                throw IllegalStateException(
                    "Missing signatures for ${missingSignatures.size} accounts. " +
                    "Call sign() or partialSign() before serializing."
                )
            }
        }

        // Verify signatures if requested
        if (verifySignatures) {
            if (!verifySignatures(requireAllSignatures)) {
                throw IllegalStateException("Signature verification failed. One or more signatures are invalid.")
            }
        }

        return com.myetherwallet.mewwalletkit.solana.serialization.MessageSerializer.serializeTransaction(
            signatures, message
        )
    }

    /**
     * Serializes just the message portion (for signing).
     *
     * This is the data that gets signed by Ed25519. It does not include signatures.
     *
     * Wire format:
     * - MessageHeader (3 bytes)
     * - Compact array of account keys
     * - Recent blockhash (32 bytes)
     * - Compact array of compiled instructions
     *
     * @return Serialized message bytes ready for signing
     */
    fun serializeMessage(): ByteArray {
        val message = compileMessage()
        return com.myetherwallet.mewwalletkit.solana.serialization.MessageSerializer.serializeMessage(message)
    }

    /**
     * Verifies all signatures in the transaction.
     *
     * Uses Ed25519 signature verification via BouncyCastle to validate that each
     * signature was created by the corresponding public key signing the message bytes.
     *
     * @param requireAllSignatures Whether to fail if any signature is missing
     * @return true if all present signatures are valid, false otherwise
     */
    fun verifySignatures(requireAllSignatures: Boolean = true): Boolean {
        // Compile message and get bytes to verify against
        val message = compileMessage()
        val messageBytes = com.myetherwallet.mewwalletkit.solana.serialization.MessageSerializer.serializeMessage(message)

        // Check for missing signatures
        val missingSigs = signatures.filter { it.signature == null }
        if (requireAllSignatures && missingSigs.isNotEmpty()) {
            return false
        }

        // Verify each present signature
        signatures.forEach { sigPair ->
            val signature = sigPair.signature ?: return@forEach  // Skip null signatures

            // Validate signature is 64 bytes
            if (signature.size != 64) {
                return false
            }

            // Create Ed25519 public key parameters
            val publicKeyBytes = sigPair.publicKey.data()
            if (publicKeyBytes.size != 32) {
                return false
            }

            val publicKeyParams = Ed25519PublicKeyParameters(publicKeyBytes, 0)

            // Create verifier and verify signature
            val verifier = Ed25519Signer()
            verifier.init(false, publicKeyParams)  // false = verify mode
            verifier.update(messageBytes, 0, messageBytes.size)

            if (!verifier.verifySignature(signature)) {
                return false
            }
        }

        return true
    }

    /**
     * Internal helper for partial signing.
     *
     * Serializes the message and signs it with each provided signer's Ed25519 private key.
     *
     * @param message The compiled message to sign
     * @param signers The private keys to sign with
     */
    private fun partialSignInternal(message: Message, signers: List<PrivateKey>) {
        // Serialize message for signing
        val messageBytes = com.myetherwallet.mewwalletkit.solana.serialization.MessageSerializer.serializeMessage(message)

        // Sign with each signer
        signers.forEach { signer ->
            // Get the 32-byte Ed25519 private key seed
            val privateKey = signer.data()

            // Sign the message using BouncyCastle Ed25519
            val signature = messageBytes.signSolanaMessage(privateKey)

            // Add signature to the transaction
            val publicKey = signer.publicKey() ?: throw IllegalArgumentException("Invalid signer public key")
            addSignatureInternal(publicKey, signature)
        }
    }

    /**
     * Internal helper for adding a signature.
     *
     * Finds the signature slot for the given public key and updates it with the signature.
     *
     * @param pubkey The public key that created the signature
     * @param signature The 64-byte Ed25519 signature
     * @throws IllegalArgumentException if signature is invalid or public key not found
     */
    private fun addSignatureInternal(pubkey: PublicKey, signature: ByteArray) {
        require(signature.size == 64) { "Ed25519 signature must be exactly 64 bytes, got ${signature.size}" }

        // Find the signature slot for this public key
        val index = signatures.indexOfFirst { it.publicKey == pubkey }
        require(index >= 0) {
            "Public key ${pubkey.address()?.address ?: "unknown"} not found in required signers. " +
            "Ensure the transaction has been compiled and the public key is part of the transaction."
        }

        // Update the signature
        signatures[index].signature = signature
    }
}
