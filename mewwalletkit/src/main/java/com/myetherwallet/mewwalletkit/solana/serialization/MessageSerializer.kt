package com.myetherwallet.mewwalletkit.solana.serialization

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.core.extension.decodeBase58
import com.myetherwallet.mewwalletkit.core.util.ShortVecCodec
import com.myetherwallet.mewwalletkit.core.util.toLittleEndianBytes
import com.myetherwallet.mewwalletkit.solana.CompiledInstruction
import com.myetherwallet.mewwalletkit.solana.Message
import com.myetherwallet.mewwalletkit.solana.MessageHeader
import com.myetherwallet.mewwalletkit.solana.SignaturePubkeyPair

/**
 * Serializer for Solana transaction messages and components.
 *
 * Wire format for Message:
 * - MessageHeader (3 bytes)
 * - Compact array of account keys (PublicKey[])
 * - Recent blockhash (32 bytes)
 * - Compact array of compiled instructions
 *
 * Wire format for Transaction:
 * - Compact array of signatures (64 bytes each, or 64 zeros if unsigned)
 * - Message (as above)
 */
object MessageSerializer {

    /**
     * Serializes a MessageHeader.
     *
     * @param header The header to serialize
     * @return 3 bytes: [numRequiredSignatures, numReadonlySignedAccounts, numReadonlyUnsignedAccounts]
     */
    fun serializeHeader(header: MessageHeader): ByteArray {
        return byteArrayOf(
            header.numRequiredSignatures.toByte(),
            header.numReadonlySignedAccounts.toByte(),
            header.numReadonlyUnsignedAccounts.toByte()
        )
    }

    /**
     * Serializes a PublicKey.
     *
     * @param publicKey The public key to serialize
     * @return 32 bytes of the public key
     */
    fun serializePublicKey(publicKey: PublicKey): ByteArray {
        return publicKey.data() // Already 32 bytes for Solana Ed25519 keys
    }

    /**
     * Serializes a CompiledInstruction.
     *
     * Wire format:
     * - Program ID index (1 byte)
     * - Compact array of account indices
     * - Compact array of instruction data bytes
     *
     * @param instruction The instruction to serialize
     * @return Serialized instruction bytes
     */
    fun serializeInstruction(instruction: CompiledInstruction): ByteArray {
        var bytes = byteArrayOf()

        // Program ID index (1 byte)
        bytes += instruction.programIdIndex.toByte()

        // Compact array of account indices
        bytes += ShortVecCodec.encodeList(instruction.accounts) { index ->
            byteArrayOf(index.toByte())
        }

        // Compact array of instruction data
        bytes += ShortVecCodec.encodeBytes(instruction.data)

        return bytes
    }

    /**
     * Serializes a Message.
     *
     * Wire format:
     * - MessageHeader (3 bytes)
     * - Compact array of account keys
     * - Recent blockhash (32 bytes)
     * - Compact array of compiled instructions
     *
     * @param message The message to serialize
     * @return Serialized message bytes (ready for signing)
     */
    fun serializeMessage(message: Message): ByteArray {
        var bytes = byteArrayOf()

        // 1. Message header (3 bytes)
        bytes += serializeHeader(message.header)

        // 2. Compact array of account keys
        bytes += ShortVecCodec.encodeList(message.accountKeys) { publicKey ->
            serializePublicKey(publicKey)
        }

        // 3. Recent blockhash (32 bytes, Base58-decoded)
        val blockhashBytes = message.recentBlockhash.decodeBase58("123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz")
            ?: throw IllegalArgumentException("Failed to decode blockhash from Base58")
        if (blockhashBytes.size != 32) {
            throw IllegalArgumentException("Blockhash must decode to 32 bytes, got ${blockhashBytes.size}")
        }
        bytes += blockhashBytes

        // 4. Compact array of compiled instructions
        bytes += ShortVecCodec.encodeList(message.instructions) { instruction ->
            serializeInstruction(instruction)
        }

        return bytes
    }

    /**
     * Serializes a transaction's signatures array.
     *
     * Each signature is 64 bytes. If a signature is null (unsigned),
     * it's serialized as 64 zero bytes.
     *
     * @param signatures The signatures to serialize
     * @return Compact array of 64-byte signatures
     */
    fun serializeSignatures(signatures: List<SignaturePubkeyPair>): ByteArray {
        return ShortVecCodec.encodeList(signatures) { pair ->
            pair.signature ?: ByteArray(64) // 64 zeros if unsigned
        }
    }

    /**
     * Serializes a complete transaction.
     *
     * Wire format:
     * - Compact array of signatures (64 bytes each)
     * - Message (see serializeMessage)
     *
     * @param signatures The transaction signatures
     * @param message The compiled message
     * @return Serialized transaction bytes (ready for broadcast)
     */
    fun serializeTransaction(signatures: List<SignaturePubkeyPair>, message: Message): ByteArray {
        var bytes = byteArrayOf()

        // 1. Signatures
        bytes += serializeSignatures(signatures)

        // 2. Message
        bytes += serializeMessage(message)

        return bytes
    }
}
