package com.myetherwallet.mewwalletkit.solana.core

/**
 * Solana blockchain constants
 */
object SolanaConstants {

    // Key sizes
    const val PUBLIC_KEY_LENGTH = 32
    const val PRIVATE_KEY_LENGTH = 32
    const val SIGNATURE_LENGTH = 64

    // BIP44 derivation path
    const val COIN_TYPE = 501
    const val BIP44_PATH_PREFIX = "m/44'/501'"

    // Network constants
    const val MAX_SEED_LENGTH = 32
    const val MAX_TX_SIZE = 1232

    // Program IDs
    object ProgramIds {
        const val SYSTEM_PROGRAM = "11111111111111111111111111111111"
        const val STAKE_PROGRAM = "Stake11111111111111111111111111111111111111"
        const val VOTE_PROGRAM = "Vote111111111111111111111111111111111111111"
        const val BPF_LOADER = "BPFLoaderUpgradeab1e11111111111111111111111"
    }

    // System Instruction types
    object SystemInstruction {
        const val CREATE_ACCOUNT = 0
        const val ASSIGN = 1
        const val TRANSFER = 2
        const val CREATE_ACCOUNT_WITH_SEED = 3
        const val ADVANCE_NONCE_ACCOUNT = 4
        const val WITHDRAW_NONCE_ACCOUNT = 5
        const val INITIALIZE_NONCE_ACCOUNT = 6
        const val AUTHORIZE_NONCE_ACCOUNT = 7
        const val ALLOCATE = 8
        const val ALLOCATE_WITH_SEED = 9
        const val ASSIGN_WITH_SEED = 10
        const val TRANSFER_WITH_SEED = 11
    }

    // Transaction limits
    const val MAX_TX_INSTRUCTION_COUNT = 64
    const val MAX_TX_ACCOUNT_COUNT = 64
    const val PACKET_DATA_SIZE = 1280
}