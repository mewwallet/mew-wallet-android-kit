package com.myetherwallet.mewwalletkit.solana.serialization

import com.myetherwallet.mewwalletkit.bip.bip44.Network
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.core.extension.decodeBase58
import com.myetherwallet.mewwalletkit.core.util.ShortVecCodec
import com.myetherwallet.mewwalletkit.core.util.toLittleEndianBytes
import com.myetherwallet.mewwalletkit.solana.CompiledInstruction
import com.myetherwallet.mewwalletkit.solana.Message
import com.myetherwallet.mewwalletkit.solana.MessageAddressTableLookup
import com.myetherwallet.mewwalletkit.solana.MessageHeader
import com.myetherwallet.mewwalletkit.solana.MessageV0
import com.myetherwallet.mewwalletkit.solana.SignaturePubkeyPair
import com.myetherwallet.mewwalletkit.solana.TransactionVersion
import com.myetherwallet.mewwalletkit.solana.VersionedMessage

/**
 * Serializer for Solana transaction messages and components.
 *
 * Supports both Legacy and V0 (versioned) message formats.
 *
 * Wire format for Legacy Message:
 * - MessageHeader (3 bytes)
 * - Compact array of account keys (PublicKey[])
 * - Recent blockhash (32 bytes)
 * - Compact array of compiled instructions
 *
 * Wire format for V0 Message:
 * - Version prefix: 0x80
 * - MessageHeader (3 bytes)
 * - Compact array of static account keys (non-ALT)
 * - Recent blockhash (32 bytes)
 * - Compact array of compiled instructions
 * - Compact array of address table lookups
 *
 * Wire format for Transaction:
 * - Compact array of signatures (64 bytes each, or 64 zeros if unsigned)
 * - Message (Legacy or V0 with version prefix)
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
        val alphabet = Network.SOLANA.alphabet()
            ?: throw IllegalStateException("Solana network should have Base58 alphabet")
        val blockhashBytes = message.recentBlockhash.decodeBase58(alphabet)
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
     * Serializes a MessageAddressTableLookup.
     *
     * Wire format:
     * - accountKey (32 bytes)
     * - shortvec(writableIndexes.size) + raw u8[]
     * - shortvec(readonlyIndexes.size) + raw u8[]
     *
     * @param lookup The address table lookup to serialize
     * @return Serialized lookup bytes
     */
    fun serializeAddressTableLookup(lookup: MessageAddressTableLookup): ByteArray {
        var bytes = byteArrayOf()

        // 1. Account key (32 bytes)
        bytes += serializePublicKey(lookup.accountKey)

        // 2. Compact array of writable indices
        bytes += ShortVecCodec.encodeList(lookup.writableIndexes) { index ->
            byteArrayOf(index.toByte())
        }

        // 3. Compact array of readonly indices
        bytes += ShortVecCodec.encodeList(lookup.readonlyIndexes) { index ->
            byteArrayOf(index.toByte())
        }

        return bytes
    }

    /**
     * Serializes a MessageV0.
     *
     * Wire format:
     * - Version prefix: 0x80
     * - Message header (3 bytes)
     * - Compact array: Static account keys (non-ALT)
     * - Recent blockhash (32 bytes)
     * - Compact array: Compiled instructions
     * - Compact array: Address lookup tables
     *
     * @param message The v0 message to serialize
     * @return Serialized message bytes (ready for signing)
     */
    fun serializeMessageV0(message: MessageV0): ByteArray {
        var bytes = byteArrayOf()

        // 1. Version prefix (0x80)
        bytes += message.version.prefix!!

        // 2. Message header (3 bytes)
        bytes += serializeHeader(message.header)

        // 3. Compact array of static account keys
        bytes += ShortVecCodec.encodeList(message.staticAccountKeys) { publicKey ->
            serializePublicKey(publicKey)
        }

        // 4. Recent blockhash (32 bytes, Base58-decoded)
        val alphabet = Network.SOLANA.alphabet()
            ?: throw IllegalStateException("Solana network should have Base58 alphabet")
        val blockhashBytes = message.recentBlockhash.decodeBase58(alphabet)
            ?: throw IllegalArgumentException("Failed to decode blockhash from Base58")
        if (blockhashBytes.size != 32) {
            throw IllegalArgumentException("Blockhash must decode to 32 bytes, got ${blockhashBytes.size}")
        }
        bytes += blockhashBytes

        // 5. Compact array of compiled instructions
        bytes += ShortVecCodec.encodeList(message.compiledInstructions) { instruction ->
            serializeInstruction(instruction)
        }

        // 6. Compact array of address table lookups
        bytes += ShortVecCodec.encodeList(message.addressTableLookups) { lookup ->
            serializeAddressTableLookup(lookup)
        }

        return bytes
    }

    /**
     * Serializes a VersionedMessage (Legacy or V0).
     *
     * @param message The versioned message to serialize
     * @return Serialized message bytes (ready for signing)
     */
    fun serializeVersionedMessage(message: VersionedMessage): ByteArray {
        return when (message) {
            is VersionedMessage.Legacy -> serializeMessage(message.message)
            is VersionedMessage.V0 -> serializeMessageV0(message.message)
        }
    }

    /**
     * Serializes a complete transaction (Legacy format).
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

    /**
     * Serializes a complete versioned transaction (Legacy or V0).
     *
     * Wire format:
     * - Compact array of signatures (64 bytes each)
     * - Versioned message (Legacy or V0 with version prefix)
     *
     * @param signatures The transaction signatures
     * @param message The versioned message
     * @return Serialized transaction bytes (ready for broadcast)
     */
    fun serializeVersionedTransaction(
        signatures: List<SignaturePubkeyPair>,
        message: VersionedMessage
    ): ByteArray {
        var bytes = byteArrayOf()

        // 1. Signatures
        bytes += serializeSignatures(signatures)

        // 2. Versioned message
        bytes += serializeVersionedMessage(message)

        return bytes
    }
}
