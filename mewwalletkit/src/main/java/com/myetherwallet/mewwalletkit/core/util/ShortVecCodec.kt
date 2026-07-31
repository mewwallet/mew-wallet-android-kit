package com.myetherwallet.mewwalletkit.core.util

/**
 * Codec for compact array length encoding using variable-length integers.
 *
 * This encoding (used by Solana and potentially other networks) uses variable-length
 * integers to represent array lengths efficiently:
 * - Values 0-127: 1 byte (0xxx xxxx)
 * - Values 128-16383: 2 bytes (1xxx xxxx, 0xxx xxxx)
 * - Values 16384-2097151: 3 bytes (1xxx xxxx, 1xxx xxxx, 0xxx xxxx)
 * - And so on...
 *
 * The high bit of each byte indicates whether more bytes follow:
 * - 0 = last byte
 * - 1 = more bytes follow
 *
 * Example:
 * - 0 = [0x00]
 * - 127 = [0x7f]
 * - 128 = [0x80, 0x01]
 * - 255 = [0xff, 0x01]
 * - 256 = [0x80, 0x02]
 * - 16383 = [0xff, 0x7f]
 * - 16384 = [0x80, 0x80, 0x01]
 */
object ShortVecCodec {

    /**
     * Encodes a length value as compact bytes.
     *
     * @param length The length to encode (must be non-negative)
     * @return Compact-encoded length bytes
     */
    fun encodeLength(length: Int): ByteArray {
        require(length >= 0) { "Length must be non-negative" }

        val bytes = mutableListOf<Byte>()
        var remaining = length

        while (true) {
            // Take the lowest 7 bits
            var byte = (remaining and 0x7F).toByte()

            // Shift to get next 7 bits
            remaining = remaining ushr 7

            // If there are more bytes to encode, set the high bit
            if (remaining != 0) {
                byte = (byte.toInt() or 0x80).toByte()
            }

            bytes.add(byte)

            // If no more bytes, we're done
            if (remaining == 0) {
                break
            }
        }

        return bytes.toByteArray()
    }

    /**
     * Decodes a compact-encoded length from bytes.
     *
     * @param bytes The bytes to decode from
     * @param offset The offset to start reading from
     * @return Pair of (decoded length, number of bytes consumed)
     */
    fun decodeLength(bytes: ByteArray, offset: Int = 0): Pair<Int, Int> {
        var length = 0
        var shift = 0
        var bytesRead = 0

        for (i in offset until bytes.size) {
            val byte = bytes[i].toInt() and 0xFF
            bytesRead++

            // Add the lower 7 bits to the result
            length = length or ((byte and 0x7F) shl shift)

            // If high bit is not set, we're done
            if ((byte and 0x80) == 0) {
                return Pair(length, bytesRead)
            }

            shift += 7

            // Prevent overflow (max ~4 billion for 32-bit int)
            if (shift >= 32) {
                throw IllegalArgumentException("Compact length encoding is too long")
            }
        }

        throw IllegalArgumentException("Incomplete compact length encoding")
    }

    /**
     * Encodes a byte array with its compact length prefix.
     *
     * @param data The data to encode
     * @return Compact length + data
     */
    fun encodeBytes(data: ByteArray): ByteArray {
        return encodeLength(data.size) + data
    }

    /**
     * Encodes a list with its compact length prefix.
     * Each item is encoded using the provided encoder function.
     *
     * @param items The items to encode
     * @param itemEncoder Function to encode each item
     * @return Compact length + encoded items
     */
    fun <T> encodeList(items: List<T>, itemEncoder: (T) -> ByteArray): ByteArray {
        val lengthBytes = encodeLength(items.size)
        val itemsBytes = items.flatMap { itemEncoder(it).toList() }.toByteArray()
        return lengthBytes + itemsBytes
    }

    /**
     * Decodes a compact-encoded list from bytes.
     *
     * @param bytes The bytes to decode from
     * @param offset The offset to start reading from (will be updated)
     * @param itemDecoder Function to decode each item, returns (item, bytes consumed)
     * @return Decoded list of items
     */
    fun <T> decodeList(bytes: ByteArray, offset: IntArray, itemDecoder: (ByteArray, Int) -> Pair<T, Int>): List<T> {
        val (length, lengthBytes) = decodeLength(bytes, offset[0])
        offset[0] += lengthBytes

        val items = mutableListOf<T>()
        repeat(length) {
            val (item, itemBytes) = itemDecoder(bytes, offset[0])
            items.add(item)
            offset[0] += itemBytes
        }
        return items
    }
}
