package com.myetherwallet.mewwalletkit.core.util

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Extension functions for encoding values in little-endian byte order.
 *
 * Used by networks that require little-endian encoding (e.g., Solana, Bitcoin).
 */

/**
 * Converts UByte to little-endian bytes (1 byte).
 */
fun UByte.toLittleEndianBytes(): ByteArray = byteArrayOf(this.toByte())

/**
 * Converts UShort to little-endian bytes (2 bytes).
 */
fun UShort.toLittleEndianBytes(): ByteArray {
    val buffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
    buffer.putShort(this.toShort())
    return buffer.array()
}

/**
 * Converts UInt to little-endian bytes (4 bytes).
 */
fun UInt.toLittleEndianBytes(): ByteArray {
    val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
    buffer.putInt(this.toInt())
    return buffer.array()
}

/**
 * Converts ULong to little-endian bytes (8 bytes).
 */
fun ULong.toLittleEndianBytes(): ByteArray {
    val buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
    buffer.putLong(this.toLong())
    return buffer.array()
}

/**
 * Converts Byte to little-endian bytes (1 byte).
 */
fun Byte.toLittleEndianBytes(): ByteArray = byteArrayOf(this)

/**
 * Converts Short to little-endian bytes (2 bytes).
 */
fun Short.toLittleEndianBytes(): ByteArray {
    val buffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
    buffer.putShort(this)
    return buffer.array()
}

/**
 * Converts Int to little-endian bytes (4 bytes).
 */
fun Int.toLittleEndianBytes(): ByteArray {
    val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
    buffer.putInt(this)
    return buffer.array()
}

/**
 * Converts Long to little-endian bytes (8 bytes).
 */
fun Long.toLittleEndianBytes(): ByteArray {
    val buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
    buffer.putLong(this)
    return buffer.array()
}

/**
 * Encodes a String as Rust-style bytes (length-prefixed UTF-8).
 * Format: [u32 length in LE] + [UTF-8 bytes]
 */
fun String.toRustBytes(): ByteArray {
    val utf8Bytes = this.toByteArray(Charsets.UTF_8)
    val length = utf8Bytes.size.toUInt()
    return length.toLittleEndianBytes() + utf8Bytes
}
