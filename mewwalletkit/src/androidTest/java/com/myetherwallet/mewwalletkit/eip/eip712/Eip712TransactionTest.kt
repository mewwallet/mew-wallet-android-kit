package com.myetherwallet.mewwalletkit.eip.eip712

import com.myetherwallet.mewwalletkit.bip.bip44.Address
import com.myetherwallet.mewwalletkit.bip.bip44.Network
import com.myetherwallet.mewwalletkit.bip.bip44.PrivateKey
import com.myetherwallet.mewwalletkit.core.extension.eip155sign
import com.myetherwallet.mewwalletkit.core.extension.hexToByteArray
import com.myetherwallet.mewwalletkit.core.extension.toHexString
import com.myetherwallet.mewwalletkit.eip.eip681.AbiFunction
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import java.math.BigInteger

class Eip712TransactionTest {

    @Test
    fun checkForEncoding() {
        val function = AbiFunction(
            "general",
            inputs = listOf(
                Pair("input", "bytes")
            ),
            arrayOf(),
            isConstant = false,
            isPayable = false
        )

        val parameters = listOf(Solidity.Bytes("0xaabbcc".hexToByteArray()))

        val result = function.encodeParameters(parameters).toHexString()
        val expected = "8c5a344500000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000003aabbcc0000000000000000000000000000000000000000000000000000000000"
        assertEquals(expected, result)
    }


    @Test
    fun checkEncodeTuple() {
        assertEquals(
            "00000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000003aabbcc0000000000000000000000000000000000000000000000000000000000",
            SolidityBase.encodeTuple(listOf(Solidity.Bytes("0xaabbcc".hexToByteArray())))
        )
    }

    @Test
    fun checkForEip712MessageJsonConvert() {
        val result = Eip712Transaction.EIP712Message.typesJson.toString()
        val expected = JSONObject(Eip712Transaction.EIP712Message.typesString).toString()
        assertEquals(expected, result)
    }


    @Test
    fun checkEip712Signature() {
        val privateKey = PrivateKey.createWithPrivateKey(
            "0x58d23b55bc9cdce1f18c2500f40ff4ab7245df9a89505e9b1fa4851f623d241d".hexToByteArray(),
            Network.ETHEREUM
        )

        val transaction = Eip712Transaction(
            nonce = BigInteger("0"),
            maxPriorityFeePerErg = BigInteger("100000000"),
            maxFeePerErg = BigInteger("100000000"),
            gasLimit = BigInteger("5000000"),
            from = privateKey.address(),
            to = privateKey.address(),
            value = BigInteger("1000000000000"),
            chainId = BigInteger("280")
        )

        transaction.eip155sign(privateKey)

        val expectedCustomSignature =
            "54761417b8b9ad2395901586b60139bd0cfcc2f99b182fee75a65551c9d7063c56d0369dd68c3150a87d31cdae42b9b00e18ac99e0c85318296e64b4a3cd3fbc1b"
        val expectedSerializedTransaction =
            "71f891808405f5e1008405f5e100834c4b4094dc544d1aa88ff8bbd2f2aec754b1f1e99e1812fd85e8d4a5100080820118808082011894dc544d1aa88ff8bbd2f2aec754b1f1e99e1812fd83027100c0b84154761417b8b9ad2395901586b60139bd0cfcc2f99b182fee75a65551c9d7063c56d0369dd68c3150a87d31cdae42b9b00e18ac99e0c85318296e64b4a3cd3fbc1bc0"

        assertEquals(expectedCustomSignature, transaction.meta.customSignature?.toHexString())
        assertEquals(expectedSerializedTransaction, transaction.serialize()?.toHexString())
    }


    @Test
    fun checkEip712SignatureWithPaymaster() {
        val privateKey = PrivateKey.createWithPrivateKey(
            "0x58d23b55bc9cdce1f18c2500f40ff4ab7245df9a89505e9b1fa4851f623d241d".hexToByteArray(),
            Network.ETHEREUM
        )

        val transaction = Eip712Transaction(
            nonce = BigInteger("0"),
            maxPriorityFeePerErg = BigInteger("100000000"),
            maxFeePerErg = BigInteger("100000000"),
            gasLimit = BigInteger("5000000"),
            from = privateKey.address(),
            to = privateKey.address(),
            value = BigInteger("1000000000000"),
            chainId = BigInteger("280")
        )

        transaction.meta = Eip712Transaction.Meta(
            paymaster = Eip712Transaction.Paymaster.general(
                paymaster = Address("0x0265d9a5af8af5fe070933e5e549d8fef08e09f4"),
                innerInput = "0xaabbcc".hexToByteArray()
            )
        )

        transaction.eip155sign(privateKey)

        val expectedCustomSignature =
            "43e0371c523a089bda9024ae0ca175138b70124e6100b063d8e952f14a5bb8d90cdf4e693007ac90b612d27e09a25e0ba7761c3f06d9d8a869f2f612b2ce5dd01c"
        val expectedSerializedTransaction =
            "71f8cf808405f5e1008405f5e100834c4b4094dc544d1aa88ff8bbd2f2aec754b1f1e99e1812fd85e8d4a5100080820118808082011894dc544d1aa88ff8bbd2f2aec754b1f1e99e1812fd83027100c0b84143e0371c523a089bda9024ae0ca175138b70124e6100b063d8e952f14a5bb8d90cdf4e693007ac90b612d27e09a25e0ba7761c3f06d9d8a869f2f612b2ce5dd01cf83d940265d9a5af8af5fe070933e5e549d8fef08e09f4a78c5a34450000000000000000000000000000000000000000000000000000000000000020aabbcc"

        val customSignatureResult = transaction.meta.customSignature?.toHexString()
        val serializedTransactionResult = transaction.serialize()?.toHexString()
        assertEquals(expectedCustomSignature, customSignatureResult)
        assertEquals(expectedSerializedTransaction, serializedTransactionResult)
    }

}