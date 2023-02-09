package com.myetherwallet.mewwalletkit.eip.eip712

import com.myetherwallet.mewwalletkit.bip.bip44.Address
import com.myetherwallet.mewwalletkit.bip.bip44.Network
import com.myetherwallet.mewwalletkit.bip.bip44.PrivateKey
import com.myetherwallet.mewwalletkit.core.extension.eip155sign
import com.myetherwallet.mewwalletkit.core.extension.hexToByteArray
import com.myetherwallet.mewwalletkit.core.extension.sign
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
            maxPriorityFeePerGas = BigInteger("100000000"),
            maxFeePerGas = BigInteger("100000000"),
            gasLimit = BigInteger("5000000"),
            from = privateKey.address(),
            to = privateKey.address(),
            value = BigInteger("1000000000000"),
            chainId = BigInteger("280")
        )

        transaction.sign(privateKey)

        val expectedCustomSignature =
            "8ab421f3cfacd51e08054c79db85525d082cff45484c0cc4031d85b95b81377c16e4b6666077d456cad752acd3c0ebf403938012746517262f8e97591de3a2591c"
        val expectedSerializedTransaction =
            "71f890808405f5e1008405f5e100834c4b4094dc544d1aa88ff8bbd2f2aec754b1f1e99e1812fd85e8d4a5100080820118808082011894dc544d1aa88ff8bbd2f2aec754b1f1e99e1812fd820320c0b8418ab421f3cfacd51e08054c79db85525d082cff45484c0cc4031d85b95b81377c16e4b6666077d456cad752acd3c0ebf403938012746517262f8e97591de3a2591cc0"

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
            maxPriorityFeePerGas = BigInteger("100000000"),
            maxFeePerGas = BigInteger("100000000"),
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

        transaction.sign(privateKey)

        val expectedCustomSignature =
            "b882c515ee8a55a2ff6b8b970cf7eb4fdcd38895ff621416555f1d51eafe23cd47c14d498a32b73a9a73cfae2c64d4476987ca9bfea4096f2fbcd558927fe0a01b"
        val expectedSerializedTransaction =
            "71f8ce808405f5e1008405f5e100834c4b4094dc544d1aa88ff8bbd2f2aec754b1f1e99e1812fd85e8d4a5100080820118808082011894dc544d1aa88ff8bbd2f2aec754b1f1e99e1812fd820320c0b841b882c515ee8a55a2ff6b8b970cf7eb4fdcd38895ff621416555f1d51eafe23cd47c14d498a32b73a9a73cfae2c64d4476987ca9bfea4096f2fbcd558927fe0a01bf83d940265d9a5af8af5fe070933e5e549d8fef08e09f4a78c5a34450000000000000000000000000000000000000000000000000000000000000020aabbcc"

        val customSignatureResult = transaction.meta.customSignature?.toHexString()
        val serializedTransactionResult = transaction.serialize()?.toHexString()
        assertEquals(expectedCustomSignature, customSignatureResult)
        assertEquals(expectedSerializedTransaction, serializedTransactionResult)
    }

}