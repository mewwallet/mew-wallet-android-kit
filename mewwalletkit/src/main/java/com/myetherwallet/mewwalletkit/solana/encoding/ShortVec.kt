package com.myetherwallet.mewwalletkit.solana.encoding

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Solana ShortVec encoding/decoding utilities
 *
 * ShortVec is a compact way to encode array lengths in Solana transactions.
 * It uses variable-length encoding where:
 * - Values 0-127: encoded as single byte
 * - Values 128-16383: encoded as 2 bytes with continuation bit
 * - And so on...
 */
object ShortVec {

    /**
     * Encode a length value as ShortVec
     * @param length The length to encode
     * @return The encoded bytes
     */
    fun encodeLength(length: Int): ByteArray {
        val result = ByteArrayOutputStream()
        var remaining = length

        while (remaining > 0) {
            var byte = remaining and 0x7F // Take lower 7 bits
            remaining = remaining ushr 7   // Shift right by 7 bits

            if (remaining > 0) {
                byte = byte or 0x80 // Set continuation bit
            }

            result.write(byte)
        }

        // Special case: length 0 is encoded as single 0 byte
        if (length == 0) {
            result.write(0)
        }

        return result.toByteArray()
    }

    /**
     * Decode a ShortVec length from bytes
     * @param data The byte array to decode from
     * @param offset The starting offset (will be updated)
     * @return The decoded length
     */
    fun decodeLength(data: ByteArray, offset: IntArray): Int {
        var result = 0
        var shift = 0
        var index = offset[0]

        while (index < data.size) {
            val byte = data[index].toInt() and 0xFF
            result = result or ((byte and 0x7F) shl shift)

            index++

            if ((byte and 0x80) == 0) {
                // No continuation bit, we're done
                break
            }

            shift += 7

            if (shift >= 32) {
                throw IllegalArgumentException("ShortVec length too large")
            }
        }

        offset[0] = index
        return result
    }

    /**
     * Decode a ShortVec length from ByteBuffer
     * @param buffer The ByteBuffer to decode from
     * @return The decoded length
     */
    fun decodeLength(buffer: ByteBuffer): Int {
        var result = 0
        var shift = 0

        while (buffer.hasRemaining()) {
            val byte = buffer.get().toInt() and 0xFF
            result = result or ((byte and 0x7F) shl shift)

            if ((byte and 0x80) == 0) {
                // No continuation bit, we're done
                break
            }

            shift += 7

            if (shift >= 32) {
                throw IllegalArgumentException("ShortVec length too large")
            }
        }

        return result
    }

    /**
     * Encode an array with ShortVec length prefix
     * @param items The items to encode
     * @param itemEncoder Function to encode each item
     * @return The encoded bytes (length + items)
     */
    fun <T> encodeArray(items: List<T>, itemEncoder: (T) -> ByteArray): ByteArray {
        val result = ByteArrayOutputStream()

        // Write length prefix
        result.write(encodeLength(items.size))

        // Write each item
        for (item in items) {
            result.write(itemEncoder(item))
        }

        return result.toByteArray()
    }

    /**
     * Decode an array with ShortVec length prefix
     * @param data The data to decode from
     * @param offset The starting offset (will be updated)
     * @param itemDecoder Function to decode each item
     * @return The decoded items
     */
    fun <T> decodeArray(
        data: ByteArray,
        offset: IntArray,
        itemDecoder: (ByteArray, IntArray) -> T
    ): List<T> {
        val length = decodeLength(data, offset)
        val result = mutableListOf<T>()

        repeat(length) {
            result.add(itemDecoder(data, offset))
        }

        return result
    }

    /**
     * Calculate the encoded size of a length value
     * @param length The length value
     * @return Number of bytes needed to encode this length
     */
    fun encodedSize(length: Int): Int {
        if (length == 0) return 1

        var size = 0
        var remaining = length

        while (remaining > 0) {
            size++
            remaining = remaining ushr 7
        }

        return size
    }
}