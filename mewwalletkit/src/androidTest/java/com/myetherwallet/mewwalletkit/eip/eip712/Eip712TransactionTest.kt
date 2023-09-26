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
            "8e67015a432c2990214401d35fa2b2b91e6857bb12bbc4f708a864a8dbe2e3195751e3d8f9b0d9a743351d5642f1c4d67457e1069822092a21068e672dd2f28a1c"
        val expectedSerializedTransaction =
            "71f890808405f5e1008405f5e100834c4b4094dc544d1aa88ff8bbd2f2aec754b1f1e99e1812fd85e8d4a5100080820118808082011894dc544d1aa88ff8bbd2f2aec754b1f1e99e1812fd82c350c0b8418e67015a432c2990214401d35fa2b2b91e6857bb12bbc4f708a864a8dbe2e3195751e3d8f9b0d9a743351d5642f1c4d67457e1069822092a21068e672dd2f28a1cc0"

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

        val expectedPaymasterInput =
            "8c5a344500000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000003aabbcc0000000000000000000000000000000000000000000000000000000000"
        val expectedCustomSignature =
            "0d44b41225e470d984d77061101fdea43cc8d28c74be4d0bbe126d17535a4dc1690e629a5fc23f586d2d1fffa862b4a5e0e34fcbbb116a1e80e3a89711ddd4ff1c"
        val expectedSerializedTransaction =
            "71f9010c808405f5e1008405f5e100834c4b4094dc544d1aa88ff8bbd2f2aec754b1f1e99e1812fd85e8d4a5100080820118808082011894dc544d1aa88ff8bbd2f2aec754b1f1e99e1812fd82c350c0b8410d44b41225e470d984d77061101fdea43cc8d28c74be4d0bbe126d17535a4dc1690e629a5fc23f586d2d1fffa862b4a5e0e34fcbbb116a1e80e3a89711ddd4ff1cf87b940265d9a5af8af5fe070933e5e549d8fef08e09f4b8648c5a344500000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000003aabbcc0000000000000000000000000000000000000000000000000000000000"

        val paymasterInputResult = transaction.meta.paymaster?.input?.toHexString()
        val customSignatureResult = transaction.meta.customSignature?.toHexString()
        val serializedTransactionResult = transaction.serialize()?.toHexString()
        assertEquals(expectedPaymasterInput, paymasterInputResult)
        assertEquals(expectedCustomSignature, customSignatureResult)
        assertEquals(expectedSerializedTransaction, serializedTransactionResult)
    }

}