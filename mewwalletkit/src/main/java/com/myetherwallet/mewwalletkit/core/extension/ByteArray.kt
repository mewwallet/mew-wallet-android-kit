package com.myetherwallet.mewwalletkit.core.extension

import com.myetherwallet.mewwalletkit.core.util.BitReader
import fr.acinq.secp256k1.Secp256k1
import org.spongycastle.crypto.digests.RIPEMD160Digest
import org.spongycastle.jcajce.provider.digest.Keccak
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.experimental.or
import kotlin.experimental.xor

/**
 * Created by BArtWell on 13.05.2019.
 */

fun ByteArray.toBits(position: Int, length: Int): Int? {
    return toBits(position until position + length)
}

fun ByteArray.toBits(range: IntRange): Int? {
    if (range.start < 0 || range.start > range.last || range.last >= this.size * 8) {
        return null
    }
    val reader = BitReader(this)
    return reader.get(range)
}

fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

fun ByteArray.toBigInteger(): BigInteger = if (this.isEmpty()) BigInteger.ZERO else BigInteger(1, this)

fun ByteArray.prefix(count: Int) = this.copyOfRange(0, count)

fun ByteArray.padLeft(length: Int, byte: Byte = 0x00): ByteArray {
    return if (length > this.size) {
        val data = ByteArray(length - this.size)
        data.fill(byte)
        data + this
    } else {
        this
    }
}

fun ByteArray.padRight(length: Int, byte: Byte = 0x00): ByteArray {
    return if (length > this.size) {
        val data = ByteArray(length - this.size)
        data.fill(byte)
        this + data
    } else {
        this
    }
}


fun ByteArray.sha256() = sha("SHA-256")

fun ByteArray.sha512() = sha("SHA-512")

fun ByteArray.md5(): ByteArray {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(this))
        .toString(16)
        .padStart(32, '0')
        .hexToByteArray()
}

private fun ByteArray.sha(algorithm: String): ByteArray {
    val messageDigest = MessageDigest.getInstance(algorithm)
    messageDigest.update(this)
    return messageDigest.digest()
}

fun ByteArray.keccak256(): ByteArray {
    val digest = Keccak.Digest256()
    digest.update(this)
    return digest.digest()
}

fun ByteArray.ripemd160(): ByteArray {
    val data = this.sha256()
    val digest = RIPEMD160Digest()
    digest.update(data, 0, data.size)
    val output = ByteArray(digest.digestSize)
    digest.doFinal(output, 0)
    return output
}

fun ByteArray.eip55() = this.toHexString().eip55()

fun ByteArray.encodeBase58(alphabet: String): ByteArray? {
    if (this.isEmpty()) {
        return ByteArray(0)
    }
    val radix = BigInteger.valueOf(alphabet.length.toLong())
    val extraZero = ByteArray(this.size + 1)
    extraZero[0] = 0
    var result = ""
    System.arraycopy(this, 0, extraZero, 1, this.size)
    var value = BigInteger(extraZero)
    while (value.compareTo(BigInteger.ZERO) == 1) {
        val (quotient, modulus) = value.divideAndRemainder(radix)
        value = quotient
        val c = alphabet[modulus.toInt()]
        result += c
    }
    result = String(StringBuffer(result).reverse())
    var i = 0
    while (i < this.size && this[i].toInt() == 0) {
        result = alphabet[0] + result
        i++
    }
    return result.toByteArray()
}

fun ByteArray.encodeBase58String(alphabet: String): String? {
    val data = this.encodeBase58(alphabet) ?: return null
    return String(data)
}

fun ByteArray.secp256k1Verify() = Secp256k1.secKeyVerify(this)

fun ByteArray.secp256k1RecoverableSign(privateKey: ByteArray, addExtraEntropy: Boolean = false): ByteArray? {
    if (this.size != 32 || privateKey.size != 32) {
        return null
    }
    // Sign the message to get a 64-byte compact signature (big-endian r||s)
    // Note: secp256k1-kmp uses deterministic RFC-6979 signing, addExtraEntropy is ignored
    val signature = Secp256k1.sign(this, privateKey)

    // Calculate recovery ID by trying all possibilities (0-3)
    val publicKey = Secp256k1.pubkeyCreate(privateKey)
    var recid = -1
    for (id in 0..3) {
        try {
            val recovered = Secp256k1.ecdsaRecover(signature, this, id)
            if (recovered.contentEquals(publicKey)) {
                recid = id
                break
            }
        } catch (e: Exception) {
            // Try next recovery ID
            continue
        }
    }

    if (recid == -1) {
        return null
    }

    // Return 65-byte recoverable signature in BIG-endian format (internal format):
    // r (32 bytes) + s (32 bytes) + recovery ID (1 byte)
    return signature + byteArrayOf(recid.toByte())
}

fun ByteArray.secp256k1SerializeSignature(): ByteArray? {
    if (this.size != 65) {
        return null
    }
    // Input is BIG-endian: r (32 bytes) + s (32 bytes) + recovery ID (1 byte)
    // Output must be BIG-endian for TransactionSignature storage (for RLP encoding)
    // TransactionSignature will reverse them when needed for recovery
    val signature = this.copyOfRange(0, 64)
    val recid = this[64].toInt()

    // Validate recovery ID (0-3 are valid)
    if (recid !in 0..3) {
        return null
    }

    // Keep in big-endian format: r + s + recid
    return signature + byteArrayOf(recid.toByte())
}

fun ByteArray.secp256k1ParseSignature(): ByteArray? {
    if (this.size != 65) {
        return null
    }
    // Input is BIG-endian: r (32 bytes) + s (32 bytes) + recovery ID (1 byte)
    // Output is the same (no conversion needed)
    val recid = this[64].toInt()

    // Validate recovery ID
    if (recid !in 0..3) {
        return null
    }

    // Return as-is (already in correct format)
    return this.copyOf()
}

fun ByteArray.parsePublicKey(): ByteArray {
    return Secp256k1.pubkeyParse(this)
}

fun ByteArray.secp256k1RecoverPublicKey(hash: ByteArray, compressed: Boolean): ByteArray? {
    if (this.size < 65 || hash.size != 32) {
        return null
    }
    // Input is BIG-endian: r (32 bytes) + s (32 bytes) + recovery_id (1 byte)
    // This is the internal format used throughout the codebase
    val signature = this.copyOfRange(0, 64)
    val recid = this[64].toInt()

    // Recover the public key (returns uncompressed 65-byte key)
    val publicKey = Secp256k1.ecdsaRecover(signature, hash, recid)

    // Compress if requested
    return if (compressed) {
        Secp256k1.pubKeyCompress(publicKey)
    } else {
        publicKey
    }
}

fun ByteArray.secureCompare(rhs: ByteArray): Boolean {
    if (this.size != rhs.size) {
        return false
    }
    var difference = 0x00.toByte()
    for (i in 0 until this.size) {
        difference = difference or (this[i] xor rhs[i])
    }
    return difference == 0x00.toByte()
}

fun ByteArray.hashPersonalMessage(): ByteArray {
    var prefix = "\u0019Ethereum Signed Message:\n"
    prefix += this.size.toString()
    val prefixData = prefix.toByteArray()
    val data = prefixData + this
    return data.keccak256()
}

fun ByteArray?.isNullOrEmpty() = this == null || this.isEmpty()

/**
 * Generate Ed25519 keypair from 32-byte seed
 * @return Pair of (32-byte private key, 32-byte public key)
 */
fun ByteArray.generateEd25519KeyPair(): Pair<ByteArray, ByteArray> {
    require(this.size == 32) { "Ed25519 seed must be 32 bytes" }

    // Ed25519 keypair generation from seed (matching TweetNacl behavior)
    // The seed is hashed with SHA-512, then the private scalar is derived from first 32 bytes
    // BouncyCastle's Ed25519PrivateKeyParameters expects the seed, not a pre-processed key
    val privateKeyParams = org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters(this, 0)
    val publicKeyParams = privateKeyParams.generatePublicKey()

    // Return (seed, publicKey) - when concatenated this forms the 64-byte "secret key"
    // that matches TweetNacl's behavior: secretKey = seed (32 bytes) + publicKey (32 bytes)
    return Pair(this, publicKeyParams.encoded)
}

/**
 * Sign a message using Ed25519 for Solana network
 * @param privateKey 32-byte Ed25519 private key
 * @return Ed25519 signature (64 bytes)
 */
fun ByteArray.signSolanaMessage(privateKey: ByteArray): ByteArray {
    require(privateKey.size == 32) { "Ed25519 private key must be 32 bytes" }

    val privateKeyParams = org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters(privateKey, 0)
    val signer = org.bouncycastle.crypto.signers.Ed25519Signer()
    signer.init(true, privateKeyParams)
    signer.update(this, 0, this.size)
    return signer.generateSignature()
}
