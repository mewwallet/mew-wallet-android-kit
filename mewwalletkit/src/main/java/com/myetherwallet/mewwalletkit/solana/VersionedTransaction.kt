package com.myetherwallet.mewwalletkit.solana

import android.os.Parcelable
import com.myetherwallet.mewwalletkit.bip.bip44.PrivateKey
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.core.extension.signSolanaMessage
import com.myetherwallet.mewwalletkit.core.util.ShortVecCodec
import com.myetherwallet.mewwalletkit.solana.serialization.MessageSerializer
import kotlinx.parcelize.Parcelize
import java.io.ByteArrayOutputStream

@Parcelize
class VersionedTransaction(
    val message: VersionedMessage,
    val signatures: MutableList<ByteArray> = MutableList(message.header.numRequiredSignatures.toInt()) { ByteArray(64) }
): Parcelable {

    sealed class VersionedTransactionError : Exception() {
        object InvalidSignature : VersionedTransactionError() {
            override val message: String = "Signature must be exactly 64 bytes"
        }

        object InvalidSignaturesCount : VersionedTransactionError() {
            override val message: String = "Signatures count does not match numRequiredSignatures"
        }

        data class SignerNotRequired(val publicKey: PublicKey) : VersionedTransactionError() {
            override val message: String = "Signer ${publicKey.address()?.address} is not required for this transaction"
        }
    }

    init {
        require(signatures.size == message.header.numRequiredSignatures.toInt()) {
            throw VersionedTransactionError.InvalidSignaturesCount
        }
        require(signatures.all { it.size == 64 }) {
            throw VersionedTransactionError.InvalidSignature
        }
    }

    val version: TransactionVersion
        get() = message.version

    val recentBlockhash: String
        get() = message.recentBlockhash

    val compiledInstructions: List<CompiledInstruction>
        get() = message.compiledInstructions

    fun sign(signer: PrivateKey) {
        sign(listOf(signer))
    }

    fun sign(signers: List<PrivateKey>) {
        val messageBytes = MessageSerializer.serializeVersionedMessage(message)

        signers.forEach { signer ->
            val privateKey = signer.ed25519() ?: throw IllegalArgumentException("Invalid signer private key")
            val signature = messageBytes.signSolanaMessage(privateKey)
            val publicKey = signer.publicKey() ?: throw IllegalArgumentException("Invalid signer public key")
            addSignature(publicKey, signature)
        }
    }

    fun addSignature(publicKey: PublicKey, signature: ByteArray) {
        if (signature.size != 64) {
            throw VersionedTransactionError.InvalidSignature
        }

        val staticAccountKeys = message.staticAccountKeys
        val numRequiredSignatures = message.header.numRequiredSignatures.toInt()
        val signerPubkeys = staticAccountKeys.subList(0, numRequiredSignatures)

        val signerIndex = signerPubkeys.indexOfFirst { it == publicKey }
        if (signerIndex == -1) {
            throw VersionedTransactionError.SignerNotRequired(publicKey)
        }

        signatures[signerIndex] = signature
    }

    fun serialize(): ByteArray {
        val output = ByteArrayOutputStream()

        val lengthBytes = ShortVecCodec.encodeLength(signatures.size)
        output.write(lengthBytes)

        signatures.forEach { signature ->
            output.write(signature)
        }

        val messageBytes = MessageSerializer.serializeVersionedMessage(message)
        output.write(messageBytes)

        return output.toByteArray()
    }

    companion object {
        /**
         * Creates a VersionedTransaction from a Transaction.
         *
         * Extracts the versioned message from the transaction (either cached V0 message
         * or compiles a Legacy message) and creates a VersionedTransaction with empty signatures.
         *
         * This is useful for converting from the builder-pattern Transaction class to
         * VersionedTransaction for unified signing of both Legacy and V0 transactions.
         *
         * @param transaction The transaction to convert
         * @return A new VersionedTransaction ready for signing
         */
        fun fromTransaction(transaction: Transaction): VersionedTransaction {
            val versionedMessage = transaction.getVersionedMessage()
            return VersionedTransaction(versionedMessage)
        }

        /**
         * Deserializes a VersionedTransaction from wire format bytes.
         *
         * Wire format:
         * - Compact array length of signatures
         * - Signatures (64 bytes each)
         * - Versioned message
         *
         * @param bytes The transaction bytes to deserialize
         * @return Deserialized VersionedTransaction with signatures and message
         */
        fun deserialize(bytes: ByteArray): VersionedTransaction {
            var offset = 0

            val (signaturesCount, bytesRead) = ShortVecCodec.decodeLength(bytes, offset)
            offset += bytesRead

            val signatures = MutableList(signaturesCount) {
                val signature = bytes.copyOfRange(offset, offset + 64)
                offset += 64
                signature
            }

            val messageBytes = bytes.copyOfRange(offset, bytes.size)
            val message = MessageSerializer.deserializeVersionedMessage(messageBytes)

            return VersionedTransaction(message, signatures)
        }
    }
}
