package com.myetherwallet.mewwalletkit.solana.encoding

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Helper class for writing binary data in Solana format
 */
class BinaryWriter {
    private val buffer = ByteArrayOutputStream()

    /**
     * Write a single byte
     */
    fun writeByte(value: Byte) {
        buffer.write(value.toInt())
    }

    /**
     * Write a byte array
     */
    fun writeBytes(bytes: ByteArray) {
        buffer.write(bytes)
    }

    /**
     * Write a public key (32 bytes)
     */
    fun writePublicKey(publicKey: PublicKey) {
        buffer.write(publicKey.data())
    }

    /**
     * Write a 32-bit unsigned integer (little endian)
     */
    fun writeUInt32(value: Long) {
        val bytes = ByteBuffer.allocate(4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(value.toInt())
            .array()
        buffer.write(bytes)
    }

    /**
     * Write a 64-bit unsigned integer (little endian)
     */
    fun writeUInt64(value: Long) {
        val bytes = ByteBuffer.allocate(8)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putLong(value)
            .array()
        buffer.write(bytes)
    }

    /**
     * Write a length-prefixed array using ShortVec encoding
     */
    fun <T> writeArray(items: List<T>, itemWriter: (T, BinaryWriter) -> Unit) {
        // Write length using ShortVec encoding
        val lengthBytes = ShortVec.encodeLength(items.size)
        buffer.write(lengthBytes)

        // Write each item
        for (item in items) {
            itemWriter(item, this)
        }
    }

    /**
     * Write a ShortVec-encoded array of byte arrays
     */
    fun writeShortVec(items: List<ByteArray>) {
        // Write length using ShortVec encoding
        val lengthBytes = ShortVec.encodeLength(items.size)
        buffer.write(lengthBytes)

        // Write each byte array
        for (item in items) {
            buffer.write(item)
        }
    }

    /**
     * Write a string with length prefix
     */
    fun writeString(str: String) {
        val bytes = str.toByteArray(Charsets.UTF_8)
        val lengthBytes = ShortVec.encodeLength(bytes.size)
        buffer.write(lengthBytes)
        buffer.write(bytes)
    }

    /**
     * Get the current size of the buffer
     */
    fun size(): Int = buffer.size()

    /**
     * Get the final byte array
     */
    fun toByteArray(): ByteArray = buffer.toByteArray()

    /**
     * Clear the buffer
     */
    fun clear() {
        buffer.reset()
    }

    companion object {
        /**
         * Convenience method to write data and return the bytes
         */
        fun write(block: (BinaryWriter) -> Unit): ByteArray {
            val writer = BinaryWriter()
            block(writer)
            return writer.toByteArray()
        }
    }
}