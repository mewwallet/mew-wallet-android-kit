package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.Network
import com.myetherwallet.mewwalletkit.bip.bip44.PrivateKey
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.core.extension.hexToByteArray
import com.myetherwallet.mewwalletkit.core.extension.toHexString
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

/**
 * Transaction signing tests.
 */
class TransactionSignTest {

    @Test
    fun `test parse wire format and serialize`() {
        val sender = PrivateKey.createWithPrivateKey(
            ByteArray(32) { 0x08 }, // Arbitrary known account
            Network.SOLANA
        )
        val senderPublicKey = sender.publicKey()!!

        assertEquals(
            "1398f62c6d1a457c51ba6a4b5f3dbd2f69fca93216218dc8997e416bd17d93ca",
            senderPublicKey.data().toHexString()
        )

        val recentBlockhash = "EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k" // Arbitrary known recentBlockhash
        val recipient = PublicKey.createWithBase58(
            "J3dxNj7nDRRqRRXuEMynDG57DkZK4jYRuv3Garmb1i99", // Arbitrary known public key
            Network.SOLANA
        )

        val transfer = SystemProgram.transfer(
            fromPubkey = senderPublicKey,
            toPubkey = recipient,
            lamports = 49uL
        )

        val expectedTransaction = Transaction(
            feePayer = senderPublicKey,
            recentBlockhash = recentBlockhash
        )
        expectedTransaction.add(transfer)
        expectedTransaction.sign(sender)

        val expectedSerializedTransaction = expectedTransaction.serialize()

        val serializedTransactionBase64 = "AVuErQHaXv0SG0/PchunfxHKt8wMRfMZzqV0tkC5qO6owYxWU2v871AoWywGoFQr4z+q/7mE8lIufNl/kxj+nQ0BAAEDE5j2LG0aRXxRumpLXz29L2n8qTIWIY3ImX5Ba9F9k8r9Q5/Mtmcn8onFxt47xKj+XdXXd3C8j/FcPu7csUrz/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAxJrndgN4IFTxep3s6kO0ROug7bEsbx0xxuDkqEvwUusBAgIAAQwCAAAAMQAAAAAAAAA="
        val serializedTransaction = Base64.getDecoder().decode(serializedTransactionBase64)

        assertArrayEquals(serializedTransaction, expectedSerializedTransaction)
    }

    @Test
    fun `test partialSign`() {
        val account1 = PrivateKey.createWithPrivateKey(
            "59a1aceb689ed3fe7adc1b44d78be38d0f1f0ec99263ce6199fbb369722a773c".hexToByteArray(),
            Network.SOLANA
        )
        val account2 = PrivateKey.createWithPrivateKey(
            "d4db5154833691176b4172b6dce524d40a035698738400ca6d131e8f6decc764".hexToByteArray(),
            Network.SOLANA
        )
        val recentBlockhash = "9U2wM3MUToUCRiBsR6zAcnqJw43kcXSxp27QxsKEJSBf"

        val transfer = SystemProgram.transfer(
            fromPubkey = account1.publicKey()!!,
            toPubkey = account2.publicKey()!!,
            lamports = 123uL
        )

        var transaction = Transaction(recentBlockhash = recentBlockhash)
        transaction.add(transfer)
        transaction.sign(account1, account2)
        val serialized = transaction.serialize()
        assertEquals("02c11bb529ce91cd1325556573862ea67ffd1c941f5acbff6246909e517768681375a214db732d6a5b8140f8c9fd21b173872571c5ae8c1703ba36e499ffcdf603b09d91d092a237fe97cfde7b62fe4c7450dbdf27e72d9b2e80e8240cfe7d50386030c289f57661e44d55f4b0786ca64f2fa8edec6ba27e56e24003c521b27204020001037dca5e0f34700786cefd219b3a1310e5b52913b7404c89d31fad7ee632dcefa61ed68ea990632c02ef770046016e3b1c88ce995a656a74463a7eca61db0448cd00000000000000000000000000000000000000000000000000000000000000007dca5e0f34700786cefd219b3a1310e5b52913b7404c89d31fad7ee632dcefa601020200010c020000007b00000000000000", serialized.toHexString())

        var partialTransaction = Transaction(recentBlockhash = recentBlockhash)
        partialTransaction.add(transfer)
        partialTransaction.setSigners(account1.publicKey()!!, account2.publicKey()!!)

        assertNull(partialTransaction.getSignatures()[0].signature)
        assertNull(partialTransaction.getSignatures()[1].signature)

        partialTransaction.partialSign(account1)
        assertNotNull(partialTransaction.getSignatures()[0].signature)
        assertNull(partialTransaction.getSignatures()[1].signature)

        try {
            partialTransaction.serialize()
            fail()
        } catch (e: ValidationException) {
            // Expected - missing signatures
        }

        partialTransaction.serialize(requireAllSignatures = false)

        partialTransaction.partialSign(account2)
        assertNotNull(partialTransaction.getSignatures()[0].signature)
        assertNotNull(partialTransaction.getSignatures()[1].signature)

        partialTransaction.serialize()

        assertArrayEquals(serialized, partialTransaction.serialize())

        partialTransaction.addSignature(account1.publicKey()!!, ByteArray(64) { 0x01 })

        try {
            partialTransaction.serialize(requireAllSignatures = false, verifySignatures = true)
            fail()
        } catch (e: ValidationException) {
            // Expected - invalid signature
        }

        partialTransaction.serialize(requireAllSignatures = false, verifySignatures = false)
    }
}
