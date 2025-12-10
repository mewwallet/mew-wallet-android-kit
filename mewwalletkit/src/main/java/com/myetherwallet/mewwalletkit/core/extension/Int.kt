package com.myetherwallet.mewwalletkit.core.extension

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Created by BArtWell on 10.06.2019.
 */

fun Int.toByteArray(byteOrder: ByteOrder = ByteOrder.nativeOrder()): ByteArray =
    ByteBuffer
        .allocate(Int.SIZE_BYTES)
        .order(byteOrder)  // Set byte order BEFORE putting the value
        .putInt(this)
        .array()