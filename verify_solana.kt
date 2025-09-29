#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.iwebpp:tweetnacl-java:1.1.2")
@file:DependsOn("io.github.novacrypto:Base58:2022.01.17")

import com.iwebpp.crypto.TweetNaclFast
import io.github.novacrypto.base58.Base58
import java.security.SecureRandom

// Simulate SolanaConstants
object SolanaConstants {
    const val PUBLIC_KEY_LENGTH = 32
    const val PRIVATE_KEY_LENGTH = 32
    const val SIGNATURE_LENGTH = 64
}

// Simplified SolanaPublicKey
class SolanaPublicKey private constructor(private val bytes: ByteArray) {
    init {
        require(bytes.size == SolanaConstants.PUBLIC_KEY_LENGTH) {
            "Public key must be ${SolanaConstants.PUBLIC_KEY_LENGTH} bytes"
        }
    }

    fun bytes(): ByteArray = bytes.copyOf()
    fun toBase58(): String = Base58.base58Encode(bytes)
    fun toHexString(): String = bytes.joinToString("") { "%02x".format(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SolanaPublicKey) return false
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    companion object {
        fun fromBase58(base58: String): SolanaPublicKey {
            return SolanaPublicKey(Base58.base58Decode(base58))
        }

        fun fromHex(hex: String): SolanaPublicKey {
            val cleanHex = hex.removePrefix("0x")
            val bytes = cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            return SolanaPublicKey(bytes)
        }

        fun fromBytes(bytes: ByteArray): SolanaPublicKey {
            return SolanaPublicKey(bytes)
        }
    }
}

// Simplified SolanaPrivateKey
class SolanaPrivateKey private constructor(private val seed: ByteArray) {
    init {
        require(seed.size == SolanaConstants.PRIVATE_KEY_LENGTH) {
            "Private key seed must be ${SolanaConstants.PRIVATE_KEY_LENGTH} bytes"
        }
    }

    private val keyPair: TweetNaclFast.Signature.KeyPair by lazy {
        TweetNaclFast.Signature.keyPair_fromSeed(seed)
    }

    fun publicKey(): SolanaPublicKey {
        return SolanaPublicKey.fromBytes(keyPair.publicKey)
    }

    fun sign(message: ByteArray): ByteArray {
        return TweetNaclFast.Signature(keyPair.secretKey).detached(message)
    }

    fun seed(): ByteArray = seed.copyOf()

    companion object {
        fun generate(): SolanaPrivateKey {
            val seed = ByteArray(SolanaConstants.PRIVATE_KEY_LENGTH)
            SecureRandom().nextBytes(seed)
            return SolanaPrivateKey(seed)
        }

        fun fromSeed(seed: ByteArray): SolanaPrivateKey {
            return SolanaPrivateKey(seed.copyOf())
        }
    }
}

// Test functions
fun testKeyGeneration(): Boolean {
    println("Testing key generation...")
    val privateKey = SolanaPrivateKey.generate()
    val publicKey = privateKey.publicKey()

    println("  Private key seed length: ${privateKey.seed().size}")
    println("  Public key length: ${publicKey.bytes().size}")
    println("  Public key (Base58): ${publicKey.toBase58()}")

    return privateKey.seed().size == 32 && publicKey.bytes().size == 32
}

fun testSigning(): Boolean {
    println("\nTesting signing...")
    val privateKey = SolanaPrivateKey.generate()
    val publicKey = privateKey.publicKey()
    val message = "Hello Solana!".toByteArray()

    val signature = privateKey.sign(message)
    println("  Signature length: ${signature.size}")

    // Verify signature
    val isValid = TweetNaclFast.Signature.detached_verify(message, signature, publicKey.bytes())
    println("  Signature valid: $isValid")

    return signature.size == 64 && isValid
}

fun testBase58Encoding(): Boolean {
    println("\nTesting Base58 encoding...")
    val testBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
                               17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32)

    val publicKey = SolanaPublicKey.fromBytes(testBytes)
    val base58 = publicKey.toBase58()
    val decoded = SolanaPublicKey.fromBase58(base58)

    println("  Original bytes: ${testBytes.contentToString()}")
    println("  Base58: $base58")
    println("  Round-trip successful: ${publicKey == decoded}")

    return publicKey == decoded
}

fun testDeterministicKeys(): Boolean {
    println("\nTesting deterministic keys...")
    val seed = ByteArray(32) { it.toByte() }

    val privateKey1 = SolanaPrivateKey.fromSeed(seed)
    val privateKey2 = SolanaPrivateKey.fromSeed(seed)

    val publicKey1 = privateKey1.publicKey()
    val publicKey2 = privateKey2.publicKey()

    println("  Same seed produces same keys: ${publicKey1 == publicKey2}")
    println("  Public key: ${publicKey1.toBase58()}")

    return publicKey1 == publicKey2
}

// Main test runner
fun main() {
    println("=== Solana Implementation Verification ===\n")

    val tests = listOf(
        "Key Generation" to ::testKeyGeneration,
        "Signing and Verification" to ::testSigning,
        "Base58 Encoding" to ::testBase58Encoding,
        "Deterministic Keys" to ::testDeterministicKeys
    )

    var passed = 0
    var total = tests.size

    tests.forEach { (name, test) ->
        try {
            if (test()) {
                println("✓ $name: PASSED")
                passed++
            } else {
                println("✗ $name: FAILED")
            }
        } catch (e: Exception) {
            println("✗ $name: ERROR - ${e.message}")
            e.printStackTrace()
        }
    }

    println("\n=== Results ===")
    println("Tests passed: $passed/$total")

    if (passed == total) {
        println("🎉 All tests passed! Solana implementation is working correctly.")
    } else {
        println("❌ Some tests failed. Please check the implementation.")
    }
}