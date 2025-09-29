package com.myetherwallet.mewwalletkit.solana.rpc.models

/**
 * RPC commitment levels for transaction confirmation
 */
enum class Commitment(val value: String) {
    PROCESSED("processed"),
    CONFIRMED("confirmed"),
    FINALIZED("finalized");

    override fun toString(): String = value
}

/**
 * Blockhash information from RPC
 */
data class BlockhashInfo(
    val blockhash: String,
    val lastValidBlockHeight: Long
)

/**
 * Account information from RPC
 */
data class AccountInfo(
    val lamports: Long,
    val owner: String,
    val executable: Boolean,
    val rentEpoch: Long,
    val data: String? = null
)

/**
 * Token account information
 */
data class TokenAccountInfo(
    val pubkey: String,
    val account: AccountInfo
)

/**
 * Transaction simulation result
 */
data class SimulationResult(
    val err: String? = null,
    val logs: List<String> = emptyList(),
    val unitsConsumed: Long? = null
)

/**
 * Transaction status information
 */
data class TransactionStatus(
    val slot: Long,
    val confirmations: Int? = null,
    val err: String? = null,
    val confirmationStatus: String? = null
)

/**
 * Fee information
 */
data class FeeInfo(
    val blockhash: String,
    val feeCalculator: FeeCalculator
)

/**
 * Fee calculator
 */
data class FeeCalculator(
    val lamportsPerSignature: Long
)

/**
 * Cluster information
 */
data class ClusterInfo(
    val cluster: String,
    val version: String
)

/**
 * RPC request wrapper
 */
data class RpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int,
    val method: String,
    val params: List<Any> = emptyList()
)

/**
 * RPC response wrapper
 */
data class RpcResponse<T>(
    val jsonrpc: String,
    val id: Int,
    val result: T? = null,
    val error: RpcError? = null
)

/**
 * RPC error information
 */
data class RpcError(
    val code: Int,
    val message: String,
    val data: Any? = null
)