package com.myetherwallet.mewwalletkit.bip.bip44

import android.os.Parcel
import android.os.Parcelable
import com.myetherwallet.mewwalletkit.core.extension.*
import fr.acinq.secp256k1.Secp256k1
import java.nio.ByteOrder

/**
 * Created by BArtWell on 10.06.2019.
 */

private const val PUBLIC_KEY_COMPRESSED_SIZE = 33
private const val PUBLIC_KEY_DECOMPRESSED_SIZE = 65
private const val SOLANA_PUBLIC_KEY_SIZE = 32


class PublicKey : Key, Parcelable {

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
        raw = when (network) {
            Network.SOLANA -> {
                val (_, publicKey) = privateKey.generateEd25519KeyPair()
                publicKey
            }
            else -> {
                val publicKey = Secp256k1.pubkeyCreate(privateKey)
                if (compressed) {
                    Secp256k1.pubKeyCompress(publicKey)
                } else {
                    publicKey
                }
            }
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

    /**
     * Private constructor for Parcelable restoration
     */
    private constructor(parcel: Parcel) {
        this.raw = parcel.createByteArray() ?: ByteArray(0)
        this.chainCode = parcel.createByteArray() ?: ByteArray(0)
        this.depth = parcel.readByte()
        this.fingerprint = parcel.createByteArray() ?: ByteArray(0)
        this.index = parcel.readInt()

        // Read network properties
        val networkTitle = parcel.readString() ?: ""
        val networkPath = parcel.readString() ?: ""
        val networkChainId = parcel.readInt()
        val networkSymbol = parcel.readString() ?: ""

        // Find matching network or create custom
        this.network = findNetwork(networkTitle, networkPath, networkChainId, networkSymbol)
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
        if (network != other.network) return false

        return true
    }

    override fun hashCode(): Int {
        var result = raw.contentHashCode()
        result = 31 * result + network.hashCode()
        return result
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByteArray(raw)
        dest.writeByteArray(chainCode)
        dest.writeByte(depth)
        dest.writeByteArray(fingerprint)
        dest.writeInt(index)

        // Write network properties
        dest.writeString(network.title)
        dest.writeString(network.path)
        dest.writeInt(network.chainId.toInt())
        dest.writeString(network.symbol)
    }

    override fun describeContents(): Int = 0

    companion object {
        /**
         * Creates a PublicKey from a Base58-encoded string.
         *
         * @param base58 Base58-encoded public key
         * @param network The blockchain network
         * @return PublicKey instance
         * @throws IllegalArgumentException if Base58 string is invalid or network doesn't support Base58
         */
        fun createWithBase58(base58: String, network: Network): PublicKey {
            val alphabet = network.alphabet()
                ?: throw IllegalArgumentException("Network $network doesn't support Base58")

            val decodedBytes = base58.decodeBase58(alphabet)
                ?: throw IllegalArgumentException("Invalid Base58 string")

            return when (network) {
                Network.SOLANA -> {
                    require(decodedBytes.size == SOLANA_PUBLIC_KEY_SIZE) {
                        "Solana public key must be 32 bytes, got ${decodedBytes.size}"
                    }
                    PublicKey(decodedBytes, network)
                }
                else -> {
                    PublicKey(decodedBytes, false, network)
                }
            }
        }

        /**
         * Find matching network by properties or create custom
         */
        private fun findNetwork(title: String, path: String, chainId: Int, symbol: String): Network {
            // List all known networks
            val knownNetworks = listOf(
                Network.BITCOIN, Network.LITECOIN, Network.SINGULAR_DTV, Network.ROPSTEN,
                Network.EXPANSE, Network.LEDGER_LIVE_ETHEREUM, Network.KEEPKEY_ETHEREUM,
                Network.LEDGER_ETHEREUM, Network.ETHEREUM, Network.LEDGER_ETHEREUM_CLASSIC,
                Network.LEDGER_ETHEREUM_CLASSIC_VINTAGE, Network.LEDGER_LIVE_ETHEREUM_CLASSIC,
                Network.KEEPKEY_ETHEREUM_CLASSIC, Network.ETHEREUM_CLASSIC, Network.MIX_BLOCKCHAIN,
                Network.UBIQ, Network.RSK_MAINNET, Network.ELLAISM, Network.PIRL,
                Network.MUSICOIN, Network.CALLISTO, Network.TOMO_CHAIN, Network.THUNDERCORE,
                Network.ETHEREUM_SOCIAL, Network.ATHEIOS, Network.ETHER_GEM, Network.EOS_CLASSIC,
                Network.GO_CHAIN, Network.ETHER_SOCIAL_NETWORK, Network.RSK_TESTNET,
                Network.AKROMA, Network.IOLITE, Network.ETHER1, Network.GOERLI, Network.SOLANA,
                Network.ANONYMIZED_ID, Network.SOLANA_ANONYMIZED_ID, Network.PROFILE_ID,
                Network.SAMSUNG_PROFILE_ID
            )

            // Find exact match
            for (network in knownNetworks) {
                if (network.title == title &&
                    network.path == path &&
                    network.chainId.toInt() == chainId &&
                    network.symbol == symbol) {
                    return network
                }
            }

            // If no exact match, create custom network
            return Network.CUSTOM(title, path, chainId)
        }

        @JvmField
        val CREATOR = object : Parcelable.Creator<PublicKey> {
            override fun createFromParcel(parcel: Parcel): PublicKey {
                return PublicKey(parcel)
            }

            override fun newArray(size: Int): Array<PublicKey?> {
                return arrayOfNulls(size)
            }
        }
    }

}