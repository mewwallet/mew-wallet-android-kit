package com.myetherwallet.mewwalletkit.bip.bip44

import com.myetherwallet.mewwalletkit.core.extension.*
import fr.acinq.secp256k1.Secp256k1
import java.nio.ByteOrder

/**
 * Created by BArtWell on 10.06.2019.
 */

private const val PUBLIC_KEY_COMPRESSED_SIZE = 33
private const val PUBLIC_KEY_DECOMPRESSED_SIZE = 65
private const val SOLANA_PUBLIC_KEY_SIZE = 32

class PublicKey : Key {

    private val raw: ByteArray
    private val chainCode: ByteArray
    private val depth: Byte
    private val fingerprint: ByteArray
    private val index: Int
    private val network: Network

    constructor(
        privateKey: ByteArray,
        compressed: Boolean = false,
        chainCode: ByteArray,
        depth: Byte,
        fingerprint: ByteArray,
        index: Int,
        network: Network
    ) {
        val publicKey = Secp256k1.pubkeyCreate(privateKey)
        raw = if (compressed) {
            Secp256k1.pubKeyCompress(publicKey)
        } else {
            publicKey
        }
        this.chainCode = chainCode
        this.depth = depth
        this.fingerprint = fingerprint
        this.index = index
        this.network = network
    }

    constructor(publicKey: ByteArray, compressed: Boolean = false, network: Network) {
        this.raw = publicKey
        this.chainCode = ByteArray(0)
        this.depth = 0
        this.fingerprint = ByteArray(0)
        this.index = 0
        this.network = network
    }

    /**
     * Constructor for Solana Ed25519 public keys
     */
    constructor(ed25519PublicKey: ByteArray, network: Network) {
        require(network == Network.SOLANA) { "This constructor is only for Solana network" }
        require(ed25519PublicKey.size == SOLANA_PUBLIC_KEY_SIZE) { "Solana public key must be 32 bytes" }
        this.raw = ed25519PublicKey
        this.chainCode = ByteArray(0)
        this.depth = 0
        this.fingerprint = ByteArray(0)
        this.index = 0
        this.network = network
    }

    override fun string() = raw.toHexString()

    override fun extended(): String? {
        val alphabet = network.alphabet() ?: return null
        var extendedKey = ByteArray(0)
        extendedKey += network.publicKeyPrefix().toByteArray(ByteOrder.LITTLE_ENDIAN)
        extendedKey += depth.toByteArray(ByteOrder.BIG_ENDIAN)
        extendedKey += fingerprint
        extendedKey += index.toByteArray(ByteOrder.BIG_ENDIAN)
        extendedKey += chainCode
        extendedKey += raw
        val checksum = extendedKey.sha256().sha256().prefix(4)
        extendedKey += checksum
        return extendedKey.encodeBase58String(alphabet)
    }

    override fun data() = raw

    override fun address(): Address? {
        when (network) {
            Network.BITCOIN, Network.LITECOIN -> {
                if (raw.size != PUBLIC_KEY_COMPRESSED_SIZE) {
                    return null
                }
                val alphabet = network.alphabet() ?: return null
                val prefix = byteArrayOf(network.publicKeyHash())
                val publicKey = raw
                val payload = publicKey.sha256().ripemd160()
                val checksum = (prefix + payload).sha256().sha256().prefix(4)
                val data = prefix + payload + checksum
                val stringAddress = data.encodeBase58String(alphabet) ?: return null
                return Address.createRaw(stringAddress)
            }
            Network.SOLANA -> {
                if (raw.size != SOLANA_PUBLIC_KEY_SIZE) {
                    return null
                }
                // For Solana, the public key bytes are directly used as the address
                val alphabet = Network.SOLANA.alphabet() ?: return null
                val base58Address = raw.encodeBase58String(alphabet) ?: return null
                return Address.createRaw(base58Address)
            }
            else -> {
                if (raw.size != PUBLIC_KEY_DECOMPRESSED_SIZE) {
                    return null
                }
                val publicKey = raw
                val formattedData = (network.addressPrefix().hexToByteArray() + publicKey).drop(1).toByteArray()
                val addressData = formattedData.keccak256().takeLast(20).toByteArray()
                val eip55 = addressData.eip55() ?: return null
                return Address.create(eip55, network.addressPrefix())
            }
        }
    }



    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PublicKey

        if (!raw.contentEquals(other.raw)) return false
        if (!chainCode.contentEquals(other.chainCode)) return false
        if (depth != other.depth) return false
        if (!fingerprint.contentEquals(other.fingerprint)) return false
        if (index != other.index) return false
        if (network != other.network) return false

        return true
    }

    override fun hashCode(): Int {
        var result = raw.contentHashCode()
        result = 31 * result + chainCode.contentHashCode()
        result = 31 * result + depth
        result = 31 * result + fingerprint.contentHashCode()
        result = 31 * result + index
        result = 31 * result + network.hashCode()
        return result
    }

}