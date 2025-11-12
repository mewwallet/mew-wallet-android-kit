package com.myetherwallet.mewwalletkit.solana.serialization

import com.myetherwallet.mewwalletkit.bip.bip44.Network
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.core.extension.decodeBase58
import com.myetherwallet.mewwalletkit.core.extension.encodeBase58
import com.myetherwallet.mewwalletkit.core.extension.encodeBase58String
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

    // ==================== DESERIALIZATION ====================

    /**
     * Deserializes a MessageHeader from bytes.
     *
     * @param bytes The bytes to deserialize from
     * @param offset The current read offset
     * @return Pair of (MessageHeader, bytes consumed)
     */
    fun deserializeHeader(bytes: ByteArray, offset: Int): Pair<MessageHeader, Int> {
        if (offset + 3 > bytes.size) {
            throw IllegalArgumentException("Insufficient bytes for MessageHeader")
        }

        val header = MessageHeader(
            numRequiredSignatures = (bytes[offset].toInt() and 0xFF).toUByte(),
            numReadonlySignedAccounts = (bytes[offset + 1].toInt() and 0xFF).toUByte(),
            numReadonlyUnsignedAccounts = (bytes[offset + 2].toInt() and 0xFF).toUByte()
        )

        return Pair(header, 3)
    }

    /**
     * Deserializes a PublicKey from bytes.
     *
     * @param bytes The bytes to deserialize from
     * @param offset The current read offset
     * @return Pair of (PublicKey, bytes consumed)
     */
    fun deserializePublicKey(bytes: ByteArray, offset: Int): Pair<PublicKey, Int> {
        if (offset + 32 > bytes.size) {
            throw IllegalArgumentException("Insufficient bytes for PublicKey")
        }

        val keyBytes = bytes.sliceArray(offset until offset + 32)
        val alphabet = Network.SOLANA.alphabet()
            ?: throw IllegalStateException("Solana network should have Base58 alphabet")
        val base58 = keyBytes.encodeBase58String(alphabet)
            ?: throw IllegalArgumentException("Failed to encode PublicKey to Base58")
        val publicKey = PublicKey.createWithBase58(base58, Network.SOLANA)
            ?: throw IllegalArgumentException("Failed to create PublicKey from Base58")

        return Pair(publicKey, 32)
    }

    /**
     * Deserializes a CompiledInstruction from bytes.
     *
     * @param bytes The bytes to deserialize from
     * @param offset The current read offset
     * @return Pair of (CompiledInstruction, bytes consumed)
     */
    fun deserializeInstruction(bytes: ByteArray, offset: Int): Pair<CompiledInstruction, Int> {
        var currentOffset = offset

        // 1. Program ID index (1 byte)
        if (currentOffset >= bytes.size) {
            throw IllegalArgumentException("Insufficient bytes for instruction programIdIndex")
        }
        val programIdIndex = (bytes[currentOffset].toInt() and 0xFF).toUByte()
        currentOffset++

        // 2. Compact array of account indices
        val offsetArray = intArrayOf(currentOffset)
        val accounts = ShortVecCodec.decodeList(bytes, offsetArray) { b, off ->
            if (off >= b.size) {
                throw IllegalArgumentException("Insufficient bytes for account index")
            }
            Pair((b[off].toInt() and 0xFF).toUByte(), 1)
        }
        currentOffset = offsetArray[0]

        // 3. Compact array of instruction data
        val (dataLength, dataLengthBytes) = ShortVecCodec.decodeLength(bytes, currentOffset)
        currentOffset += dataLengthBytes

        if (currentOffset + dataLength > bytes.size) {
            throw IllegalArgumentException("Insufficient bytes for instruction data")
        }
        val data = bytes.sliceArray(currentOffset until currentOffset + dataLength)
        currentOffset += dataLength

        val instruction = CompiledInstruction(
            programIdIndex = programIdIndex,
            accounts = accounts,
            data = data
        )

        return Pair(instruction, currentOffset - offset)
    }

    /**
     * Deserializes a Message (Legacy) from bytes.
     *
     * @param bytes The bytes to deserialize from
     * @return Deserialized Message
     */
    fun deserializeMessage(bytes: ByteArray): Message {
        var currentOffset = 0

        // 1. Message header (3 bytes)
        val (header, headerBytes) = deserializeHeader(bytes, currentOffset)
        currentOffset += headerBytes

        // Check if this is a versioned message (version prefix)
        if ((header.numRequiredSignatures.toInt() and 0x80) != 0) {
            throw IllegalArgumentException("Versioned messages must be deserialized using deserializeVersionedMessage")
        }

        // 2. Compact array of account keys
        val offsetArray = intArrayOf(currentOffset)
        val accountKeys = ShortVecCodec.decodeList(bytes, offsetArray) { b, off ->
            deserializePublicKey(b, off)
        }
        currentOffset = offsetArray[0]

        // 3. Recent blockhash (32 bytes)
        if (currentOffset + 32 > bytes.size) {
            throw IllegalArgumentException("Insufficient bytes for blockhash")
        }
        val blockhashBytes = bytes.sliceArray(currentOffset until currentOffset + 32)
        val alphabet = Network.SOLANA.alphabet()
            ?: throw IllegalStateException("Solana network should have Base58 alphabet")
        val recentBlockhash = blockhashBytes.encodeBase58String(alphabet)
            ?: throw IllegalArgumentException("Failed to encode blockhash to Base58")
        currentOffset += 32

        // 4. Compact array of compiled instructions
        offsetArray[0] = currentOffset
        val instructions = ShortVecCodec.decodeList(bytes, offsetArray) { b, off ->
            deserializeInstruction(b, off)
        }

        return Message(
            header = header,
            accountKeys = accountKeys,
            recentBlockhash = recentBlockhash,
            instructions = instructions
        )
    }

    /**
     * Deserializes a MessageAddressTableLookup from bytes.
     *
     * @param bytes The bytes to deserialize from
     * @param offset The current read offset
     * @return Pair of (MessageAddressTableLookup, bytes consumed)
     */
    fun deserializeAddressTableLookup(bytes: ByteArray, offset: Int): Pair<MessageAddressTableLookup, Int> {
        var currentOffset = offset

        // 1. Account key (32 bytes)
        val (accountKey, keyBytes) = deserializePublicKey(bytes, currentOffset)
        currentOffset += keyBytes

        // 2. Compact array of writable indices
        val offsetArray = intArrayOf(currentOffset)
        val writableIndexes = ShortVecCodec.decodeList(bytes, offsetArray) { b, off ->
            if (off >= b.size) {
                throw IllegalArgumentException("Insufficient bytes for writable index")
            }
            Pair((b[off].toInt() and 0xFF).toUByte(), 1)
        }
        currentOffset = offsetArray[0]

        // 3. Compact array of readonly indices
        offsetArray[0] = currentOffset
        val readonlyIndexes = ShortVecCodec.decodeList(bytes, offsetArray) { b, off ->
            if (off >= b.size) {
                throw IllegalArgumentException("Insufficient bytes for readonly index")
            }
            Pair((b[off].toInt() and 0xFF).toUByte(), 1)
        }
        currentOffset = offsetArray[0]

        val lookup = MessageAddressTableLookup(
            accountKey = accountKey,
            writableIndexes = writableIndexes,
            readonlyIndexes = readonlyIndexes
        )

        return Pair(lookup, currentOffset - offset)
    }

    /**
     * Deserializes a MessageV0 from bytes.
     *
     * @param bytes The bytes to deserialize from
     * @return Deserialized MessageV0
     */
    fun deserializeMessageV0(bytes: ByteArray): MessageV0 {
        var currentOffset = 0

        // 1. Version prefix (0x80)
        if (currentOffset >= bytes.size) {
            throw IllegalArgumentException("Insufficient bytes for version prefix")
        }
        val versionByte = bytes[currentOffset]
        currentOffset++

        val version = TransactionVersion.fromByte(versionByte)
        if (version != TransactionVersion.V0) {
            throw IllegalArgumentException("Expected V0 version (0x80), got: ${versionByte.toInt() and 0xFF}")
        }

        // 2. Message header (3 bytes)
        val (header, headerBytes) = deserializeHeader(bytes, currentOffset)
        currentOffset += headerBytes

        // 3. Compact array of static account keys
        val offsetArray = intArrayOf(currentOffset)
        val staticAccountKeys = ShortVecCodec.decodeList(bytes, offsetArray) { b, off ->
            deserializePublicKey(b, off)
        }
        currentOffset = offsetArray[0]

        // 4. Recent blockhash (32 bytes)
        if (currentOffset + 32 > bytes.size) {
            throw IllegalArgumentException("Insufficient bytes for blockhash")
        }
        val blockhashBytes = bytes.sliceArray(currentOffset until currentOffset + 32)
        val alphabet = Network.SOLANA.alphabet()
            ?: throw IllegalStateException("Solana network should have Base58 alphabet")
        val recentBlockhash = blockhashBytes.encodeBase58String(alphabet)
            ?: throw IllegalArgumentException("Failed to encode blockhash to Base58")
        currentOffset += 32

        // 5. Compact array of compiled instructions
        offsetArray[0] = currentOffset
        val instructions = ShortVecCodec.decodeList(bytes, offsetArray) { b, off ->
            deserializeInstruction(b, off)
        }
        currentOffset = offsetArray[0]

        // 6. Compact array of address table lookups
        offsetArray[0] = currentOffset
        val addressTableLookups = ShortVecCodec.decodeList(bytes, offsetArray) { b, off ->
            deserializeAddressTableLookup(b, off)
        }

        return MessageV0(
            header = header,
            staticAccountKeys = staticAccountKeys,
            recentBlockhash = recentBlockhash,
            compiledInstructions = instructions,
            addressTableLookups = addressTableLookups
        )
    }

    /**
     * Deserializes signatures from bytes.
     *
     * @param bytes The bytes to deserialize from
     * @param offset The current read offset (will be updated)
     * @return List of signature byte arrays (64 bytes each, may be all zeros)
     */
    fun deserializeSignatures(bytes: ByteArray, offset: IntArray): List<ByteArray> {
        return ShortVecCodec.decodeList(bytes, offset) { b, off ->
            if (off + 64 > b.size) {
                throw IllegalArgumentException("Insufficient bytes for signature")
            }
            val signature = b.sliceArray(off until off + 64)
            Pair(signature, 64)
        }
    }

    /**
     * Deserializes a complete transaction from bytes.
     *
     * Returns a triple of (signatures, message, version).
     * Version is Legacy for non-versioned transactions, V0 for versioned ones.
     *
     * @param bytes The transaction bytes
     * @return Triple of (List<ByteArray>, VersionedMessage)
     */
    fun deserializeTransaction(bytes: ByteArray): Pair<List<ByteArray>, VersionedMessage> {
        val offsetArray = intArrayOf(0)

        // 1. Deserialize signatures
        val signatures = deserializeSignatures(bytes, offsetArray)

        // 2. Check message version
        if (offsetArray[0] >= bytes.size) {
            throw IllegalArgumentException("Insufficient bytes for message")
        }

        val messageBytes = bytes.sliceArray(offsetArray[0] until bytes.size)
        val version = TransactionVersion.fromByte(messageBytes[0])

        // 3. Deserialize message
        val message = when (version) {
            TransactionVersion.LEGACY -> {
                VersionedMessage.Legacy(deserializeMessage(messageBytes))
            }
            TransactionVersion.V0 -> {
                VersionedMessage.V0(deserializeMessageV0(messageBytes))
            }
        }

        return Pair(signatures, message)
    }
}
