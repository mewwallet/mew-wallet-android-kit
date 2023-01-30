package com.myetherwallet.mewwalletkit.eip.eip712

import android.os.Parcelable
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.myetherwallet.mewwalletkit.bip.bip44.Address
import com.myetherwallet.mewwalletkit.core.data.rlp.*
import com.myetherwallet.mewwalletkit.core.extension.*
import com.myetherwallet.mewwalletkit.core.extension.toRlp
import com.myetherwallet.mewwalletkit.eip.eip155.Transaction
import com.myetherwallet.mewwalletkit.eip.eip155.TransactionCurrency
import com.myetherwallet.mewwalletkit.eip.eip155.TransactionSignature
import com.myetherwallet.mewwalletkit.eip.eip2930.AccessList
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

@Parcelize
class Eip712Transaction(
    override var nonce: BigInteger = BigInteger.ZERO,
    val maxPriorityFeePerErg: BigInteger = BigInteger.ZERO,
    val maxFeePerErg: BigInteger = BigInteger.ZERO,
    val meta: Meta = Meta(),
    override var gasLimit: BigInteger = BigInteger.ZERO,
    override var to: Address?,
    override var value: BigInteger = BigInteger.ZERO,
    override var data: ByteArray = ByteArray(0),
    override var from: Address? = null,
    override var signature: TransactionSignature? = null,
    override var chainId: BigInteger? = null,
    override var currency: TransactionCurrency? = null
) : Transaction(
    nonce,
    gasLimit,
    to,
    value,
    data,
    from,
    signature,
    chainId,
    currency,
    EIPTransactionType.EIP712
) {


    /**
     *  static var types: MessageTypes {
    return [
    "EIP712Domain": [
    .init(name: "name",                     type: "string"),
    .init(name: "version",                  type: "string"),
    .init(name: "chainId",                  type: "uint256")
    ],
    "Transaction": [
    .init(name: "txType",                   type: "uint256"),
    .init(name: "from",                     type: "uint256"),
    .init(name: "to",                       type: "uint256"),
    .init(name: "ergsLimit",                type: "uint256"),
    .init(name: "ergsPerPubdataByteLimit",  type: "uint256"),
    .init(name: "maxFeePerErg",             type: "uint256"),
    .init(name: "maxPriorityFeePerErg",     type: "uint256"),
    .init(name: "paymaster",                type: "uint256"),
    .init(name: "nonce",                    type: "uint256"),
    .init(name: "value",                    type: "uint256"),
    .init(name: "data",                     type: "bytes"),
    .init(name: "factoryDeps",              type: "bytes32[]"),
    .init(name: "paymasterInput",           type: "bytes")
    ]
    ]
    }
     */

    /**
    "txType": Int(self.eipType.rawValue.stringRemoveHexPrefix(), radix: 16) as AnyObject,
    "from": (from?.address ?? "") as AnyObject,
    "to": (to?.address ?? "") as AnyObject,
    "ergsLimit": _ergsLimit as AnyObject,
    "ergsPerPubdataByteLimit": meta.ergsPerPubdata as AnyObject,
    "maxFeePerErg": _maxFeePerErg as AnyObject,
    "maxPriorityFeePerErg": _maxPriorityFeePerErg as AnyObject,
    "paymaster": (meta.paymaster?.paymaster.address ?? "") as AnyObject,
    "nonce": _nonce as AnyObject,
    "value": _value as AnyObject,
    "data": data as AnyObject,
    "factoryDeps": (meta.factoryDeps ?? []) as AnyObject,
    "paymasterInput": (meta.paymaster?.input ?? Data()) as AnyObject
     */



    fun eip712Message(): String {
        val result = JsonObject()

        val factoryDeps = JsonArray().apply {
            meta.factoryDeps?.forEach {
                add(it.toHexString())
            }
        }

        val input = JsonObject().apply {
            addProperty("txType", eipType.data.first().toInt())
            addProperty("from", from?.address ?: "0x")
            addProperty("to", to?.address ?: "0x")
            addProperty("ergsLimit", ergsLimit.toHexString().addHexPrefix())
            addProperty("ergsPerPubdataByteLimit", meta.ergsPerPubdata.toHexString().addHexPrefix())
            addProperty("maxFeePerErg", maxFeePerErg.toHexString().addHexPrefix())
            addProperty("maxPriorityFeePerErg", maxPriorityFeePerErg.toHexString().addHexPrefix())
            addProperty("paymaster", meta.paymaster?.paymaster?.address ?: "0x")
            addProperty("nonce", nonce.toHexString().addHexPrefix())
            addProperty("value", value.toHexString().addHexPrefix())
            addProperty("data", data.toHexString().addHexPrefix())
            add("factoryDeps",factoryDeps)
            addProperty("paymasterInput", meta.paymaster?.input?.toHexString() ?: "0x")
        }
        result.add("types", EIP712Message.typesJson)
        result.addProperty("primaryType","Transaction")
        result.add("domain", EIP712Message.domain(chainId))
        result.add("message", input)
        return result.toString()
    }


    object EIP712Message {


        fun domain(chainId: BigInteger?): JsonObject = JsonObject().apply {
            addProperty("name","zkSync")
            addProperty("version","2")
            addProperty("chainId", chainId)
            add("verifyingContract", null)
        }

        val typesJson: JsonObject
        get() {
            val types = JsonObject()
            val eip712Domain = JsonArray()
            val transaction = JsonArray()

            eip712Domain.apply {
                add(type("name","string"))
                add(type("version","string"))
                add(type("chainId","uint256"))
            }

            transaction.apply {
                add(type("txType","uint256"))
                add(type("from","uint256"))
                add(type("to","uint256"))
                add(type("ergsLimit","uint256"))
                add(type("ergsPerPubdataByteLimit","uint256"))
                add(type("maxFeePerErg","uint256"))
                add(type("maxPriorityFeePerErg","uint256"))
                add(type("paymaster","uint256"))
                add(type("nonce","uint256"))
                add(type("value","uint256"))
                add(type("data","bytes"))
                add(type("factoryDeps","bytes32[]"))
                add(type("paymasterInput","bytes"))
            }

            types.add("EIP712Domain", eip712Domain)
            types.add("Transaction", transaction)
            return types
        }

        private fun type(name: String, type: String): JsonObject {
            return JsonObject().apply {
                addProperty("name", name)
                addProperty("type", type)
            }
        }
        val typesString: String =
            """
        {
   "types":{
      "EIP712Domain":[
         {
            "name":"name",
            "type":"string"
         },
         {
            "name":"version",
            "type":"string"
         },
         {
            "name":"chainId",
            "type":"uint256"
         }
      ],
      "Transaction":[
         {
            "name":"txType",
            "type":"uint256"
         },
         {
            "name":"from",
            "type":"uint256"
         },
         {
            "name":"to",
            "type":"uint256"
         },
         {
            "name":"ergsLimit",
            "type":"uint256"
         },
         {
            "name":"ergsPerPubdataByteLimit",
            "type":"uint256"
         },
         {
            "name":"maxFeePerErg",
            "type":"uint256"
         },
         {
            "name":"maxPriorityFeePerErg",
            "type":"uint256"
         },
         {
            "name":"paymaster",
            "type":"uint256"
         },
         {
            "name":"nonce",
            "type":"uint256"
         },
         {
            "name":"value",
            "type":"uint256"
         },
         {
            "name":"data",
            "type":"bytes"
         },
         {
            "name":"factoryDeps",
            "type":"bytes32[]"
         },
         {
            "name":"paymasterInput",
            "type":"bytes"
         }
      ]
   }
}
            """
    }

    @Parcelize
    data class Meta(
        var ergsPerPubdata: BigInteger = BigInteger("160000"), //DEFAULT_ERGS_PER_PUBDATA_LIMIT
        var customSignature: ByteArray? = null,
        val paymaster: Paymaster? = null,
        val factoryDeps: Array<ByteArray>? = null
    ): Parcelable

    @Parcelize
    data class Paymaster(
        val paymaster: Address,
        val input: ByteArray?
    ): Parcelable

    val ergsLimit: BigInteger
        get() = gasLimit

    constructor(
        nonce: ByteArray = byteArrayOf(0x00),
        maxPriorityFeePerErg: ByteArray = byteArrayOf(0x00),
        maxFeePerErg: ByteArray = byteArrayOf(0x00),
        meta: Meta = Meta(),
        ergsLimit: ByteArray = byteArrayOf(0x00),
        to: Address?,
        value: ByteArray = byteArrayOf(0x00),
        data: ByteArray = byteArrayOf(0x00),
        from: Address? = null,
        signature: TransactionSignature? = null,
        chainId: ByteArray?,
        currency: TransactionCurrency? = null
    ) : this(
        nonce.toBigInteger(),
        maxPriorityFeePerErg.toBigInteger(),
        maxFeePerErg.toBigInteger(),
        meta,
        ergsLimit.toBigInteger(),
        to,
        value.toBigInteger(),
        data,
        from,
        signature,
        chainId?.toBigInteger(),
        currency
    )

    constructor(
        nonce: String = "0x00",
        maxPriorityFeePerErg: String = "0x00",
        maxFeePerErg: String = "0x00",
        meta: Meta = Meta(),
        ergsLimit: String = "0x00",
        to: Address?,
        value: String = "0x00",
        data: ByteArray,
        from: Address? = null,
        signature: TransactionSignature? = null,
        chainId: ByteArray? = null,
        currency: TransactionCurrency? = null
    ) : this(
        nonce.hexToBigInteger(),
        maxPriorityFeePerErg.hexToBigInteger(),
        maxFeePerErg.hexToBigInteger(),
        meta,
        ergsLimit.hexToBigInteger(),
        to,
        value.hexToBigInteger(),
        data,
        from,
        signature,
        chainId?.toBigInteger(),
        currency
    )

    override fun toString(): String {
        var description = "Transaction\n"
        description += "EIPType: " + eipType.data.toHexString() + "\n"
        description += "Nonce: " + nonce.toHexStringWithoutLeadingZeroByte() + "\n"
        description += "Max Priority Fee Per Gas: " + maxPriorityFeePerErg.toHexStringWithoutLeadingZeroByte() + "\n"
        description += "Max Fee Per Gas: " + maxFeePerErg.toHexStringWithoutLeadingZeroByte() + "\n"
        description += "Meta: $meta\n"
        description += "Gas Limit: " + gasLimit.toHexStringWithoutLeadingZeroByte() + "\n"
        description += "From: $from\n"
        description += "To: " + to?.address + "\n"
        description += "Value: " + value.toHexStringWithoutLeadingZeroByte() + "\n"
        description += "Data: " + data.toHexString() + "\n"
        description += "ChainId: " + chainId?.toHexStringWithoutLeadingZeroByte() + "\n"
        description += "Signature: " + (signature ?: "none") + "\n"
        description += "Hash: " + (hash()?.toHexString() ?: "none")
        return description
    }

    //
    // Creates and returns rlp array with order:
    // RLP([nonce, maxPriorityFeePerErg, maxFeePerErg, ergsLimit, to? || "", value, input,
    // (signatureYParity, signatureR, signatureS) || (chainID, "", ""), chainID, from, ergPerPubdata,
    // factoryDeps || [], customSignature || Data(), [paymaster, paymasterInput] || []])
    //
    override fun rlpData(chainId: BigInteger?, forSignature: Boolean): RlpArray {
        if (chainId == null) {
            return RlpArray()
        }

        // 1: nonce
        // 2: maxPriorityFeePerErg
        // 3: maxFeePerErg
        // 4: ergsLimit
        val fields = mutableListOf<Rlp>(
            nonce.toRlp(),
            maxPriorityFeePerErg.toRlp(),
            maxFeePerErg.toRlp(),
            ergsLimit.toRlp()
        )

        // 5: to || 0x
        to?.address?.let {
            fields.add(RlpString(it))
        } ?: fields.add(RlpString(""))

        // 6: value
        // 7: input
        fields.add(value.toRlp())
        fields.add(RlpByteArray(data))

        // 8: ([yParity, r, s] || (chainID, "", "")
        if (signature != null && !forSignature) {
            val signatureYParity = signature!!.signatureYParity
            val r = RlpBigInteger(signature!!.r)
            val s = RlpBigInteger(signature!!.s)

            r.dataLength = RlpBigInteger(signature!!.r).dataLength
            signatureYParity.dataLength = signature!!.signatureYParity.dataLength
            s.dataLength = RlpBigInteger(signature!!.s).dataLength
            fields.add(signatureYParity)
            fields.add(r)
            fields.add(s)
        } else {
            fields.add(chainId.toRlp())
            fields.add(RlpString(""))
            fields.add(RlpString(""))
        }
        // 9: chainID
        // 10: from
        // 11: ergsPerPubdata
        fields.add(chainId.toRlp())
        fields.add(RlpString(from!!.address))
        fields.add(meta.ergsPerPubdata.toRlp())

        // 12: factoryDeps
        fields.add((meta.factoryDeps ?: RlpArray()) as RlpArray)

        // 13: Signature
        fields.add(RlpByteArray(meta.customSignature ?: ByteArray(0)))

        // 14: Paymaster
        meta.paymaster?.let {

            fields.add(RlpArray(*listOf(
                RlpString(it.paymaster.address),
                RlpByteArray(it.input ?: ByteArray(0))
            ).toTypedArray()))

        } ?: fields.add(RlpArray())

        return RlpArray(*fields.toTypedArray())
    }

}
