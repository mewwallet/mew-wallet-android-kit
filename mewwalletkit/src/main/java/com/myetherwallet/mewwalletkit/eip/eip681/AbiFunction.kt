package com.myetherwallet.mewwalletkit.eip.eip681

import com.myetherwallet.mewwalletkit.core.extension.hexToByteArray
import com.myetherwallet.mewwalletkit.core.extension.keccak256
import com.myetherwallet.mewwalletkit.eip.eip67.AbiDecoder
import pm.gnosis.model.SolidityBase


/**
 * Created by BArtWell on 23.09.2021.
 */

data class AbiFunction(
    val functionName: String,
    val inputs: List<Pair<String, String>>,
    val outputs: Array<Any>,
    val isConstant: Boolean,
    val isPayable: Boolean
) {

    val erc20transfer = Method.ERC20_TRANSFER

    private val signature: String
        get() = "${functionName}(${inputs.joinToString(",") { it.second }})"

    private val methodEncoding: ByteArray
        get() = signature.toByteArray(Charsets.US_ASCII).keccak256().take(4).toByteArray()

    fun encodeParameters(params: List<SolidityBase.Type>): ByteArray {
        return methodEncoding + SolidityBase.encodeTuple(params).hexToByteArray()
    }

    fun decodeInputData(rawData: ByteArray): Map<String, Any>? {
        var data = rawData
        when (rawData.size % 32) {
            0 -> {
            }
            4 -> {
                data = rawData.copyOfRange(4, rawData.size)
            }
            else -> return null
        }
        if (data.isEmpty() && inputs.size == 1) {
            val name = "0"
            val value = ""
            val returnArray = mutableMapOf<String, Any>()
            returnArray[name] = value
            if (inputs[0].first != "") {
                returnArray[inputs[0].first] = value
            }
            return returnArray
        }

        if (inputs.size * 32 > data.size) {
            return null
        }
        val returnArray = mutableMapOf<String, Any>()
        var i = 0
        val values = AbiDecoder.decode(inputs.toTypedArray(), data) ?: return null

        for (input in inputs) {
            val name = i.toString()
            returnArray[name] = values[i]
            if (input.first != "") {
                returnArray[input.first] = values[i]
            }
            i += 1
        }
        return returnArray
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbiFunction

        if (functionName != other.functionName) return false
        if (inputs != other.inputs) return false
        if (!outputs.contentEquals(other.outputs)) return false
        if (isConstant != other.isConstant) return false
        if (isPayable != other.isPayable) return false
        if (erc20transfer != other.erc20transfer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = functionName.hashCode()
        result = 31 * result + inputs.hashCode()
        result = 31 * result + outputs.contentHashCode()
        result = 31 * result + isConstant.hashCode()
        result = 31 * result + isPayable.hashCode()
        result = 31 * result + erc20transfer.hashCode()
        return result
    }

    enum class Method(val methodName: String) {
        ERC20_TRANSFER("transfer")
    }
}
