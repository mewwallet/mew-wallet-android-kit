package com.myetherwallet.mewwalletkit.core.extension

import com.myetherwallet.mewwalletkit.bip.bip44.PrivateKey
import com.myetherwallet.mewwalletkit.bip.bip44.exception.InternalErrorException
import com.myetherwallet.mewwalletkit.core.data.rlp.RlpTransaction
import com.myetherwallet.mewwalletkit.eip.eip155.Transaction
import com.myetherwallet.mewwalletkit.eip.eip155.TransactionSignature
import com.myetherwallet.mewwalletkit.eip.eip155.exception.InvalidChainIdException
import com.myetherwallet.mewwalletkit.eip.eip155.exception.InvalidPrivateKeyException
import com.myetherwallet.mewwalletkit.eip.eip155.exception.InvalidPublicKeyException
import com.myetherwallet.mewwalletkit.eip.eip155.exception.InvalidSignatureException
import com.myetherwallet.mewwalletkit.eip.eip712.Eip712Transaction
import com.myetherwallet.mewwalletkit.eip.eip712.Eip712Utils

/**
 * Created by BArtWell on 13.06.2019.
 */

fun Transaction.encode() = RlpTransaction(this).rlpEncode()

fun Transaction.sign(key: PrivateKey, extraEntropy: Boolean = false) {
    if (this.chainId == null) {
        this.chainId = key.network.chainId
    }
    val chainId = this.chainId ?: throw InvalidChainIdException()

    if (eipType == Transaction.EIPTransactionType.EIP712) {
        (this as? Eip712Transaction)?.apply {
            val signature = Eip712Utils.getHash(eip712Message).secp256k1RecoverableSign(key.data())
            val serialized = signature?.secp256k1SerializeSignature()!!
            val rs = serialized.copyOfRange(0, 64)
            val v = (serialized[64] + 27).toByte()
            val normalized = rs + v
            meta.customSignature = normalized
        }
    } else {

        val publicKeyData = key.publicKey()?.data() ?: throw InvalidPublicKeyException()
        val signature = this.eip155sign(key, extraEntropy)
        val serializedSignature = signature.first ?: throw InvalidSignatureException()
        val transactionSignature = TransactionSignature(serializedSignature, chainId)
        val recoveredPublicKey =
            transactionSignature.recoverPublicKey(this) ?: throw InvalidSignatureException()
        if (!publicKeyData.secureCompare(recoveredPublicKey)) {
            throw InvalidPublicKeyException()
        }
        this.signature = transactionSignature
    }
}

fun Transaction.eip155sign(
    privateKey: PrivateKey,
    extraEntropy: Boolean = false
): Pair<ByteArray?, ByteArray?> {
    val privateKeyData = privateKey.data()
    if (!privateKeyData.secp256k1Verify()) {
        throw InvalidPrivateKeyException()
    }
    if (this.chainId == null) {
        throw InvalidChainIdException()
    }

    this.signature = null
    val publicKey = privateKey.publicKey(true)?.data() ?: throw InvalidPublicKeyException()
    val hash = this.hash(this.chainId, true) ?: throw InternalErrorException()
    for (i in 0 until 1024) {
        val signature = hash.secp256k1RecoverableSign(privateKeyData, extraEntropy) ?: continue
        val recoveredPublicKey = signature.secp256k1RecoverPublicKey(hash, true) ?: continue
        if (!recoveredPublicKey.secureCompare(publicKey)) {
            continue
        }
        val serializedSignature = signature.secp256k1SerializeSignature() ?: continue
        return Pair(serializedSignature, signature)
    }

    return Pair(null, null)
}
