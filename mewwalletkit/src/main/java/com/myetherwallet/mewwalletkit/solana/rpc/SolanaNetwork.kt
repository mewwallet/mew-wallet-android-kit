package com.myetherwallet.mewwalletkit.solana.rpc

/**
 * Solana network configurations
 */
enum class SolanaNetwork(val displayName: String, val rpcUrl: String, val wsUrl: String) {
    MAINNET_BETA(
        displayName = "Mainnet Beta",
        rpcUrl = "https://api.mainnet-beta.solana.com",
        wsUrl = "wss://api.mainnet-beta.solana.com"
    ),
    DEVNET(
        displayName = "Devnet",
        rpcUrl = "https://api.devnet.solana.com",
        wsUrl = "wss://api.devnet.solana.com"
    ),
    TESTNET(
        displayName = "Testnet",
        rpcUrl = "https://api.testnet.solana.com",
        wsUrl = "wss://api.testnet.solana.com"
    ),
    LOCALHOST(
        displayName = "Localhost",
        rpcUrl = "http://127.0.0.1:8899",
        wsUrl = "ws://127.0.0.1:8900"
    );

    companion object {
        /**
         * Get network by display name
         */
        fun fromDisplayName(name: String): SolanaNetwork? {
            return values().find { it.displayName.equals(name, ignoreCase = true) }
        }

        /**
         * Get network by RPC URL
         */
        fun fromRpcUrl(url: String): SolanaNetwork? {
            return values().find { it.rpcUrl == url }
        }
    }
}