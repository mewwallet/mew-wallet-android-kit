package com.myetherwallet.mewwalletkit.examples

import com.myetherwallet.mewwalletkit.solana.Solana
import com.myetherwallet.mewwalletkit.solana.bip44.SolanaWallet
import com.myetherwallet.mewwalletkit.solana.crypto.SolanaPrivateKey

/**
 * Example usage of the Solana implementation
 * This shows how to:
 * 1. Create wallets
 * 2. Generate addresses
 * 3. Create and sign transactions
 */
fun main() {
    println("=== Solana Android Implementation Example ===\n")

    // 1. Generate a new random wallet
    println("1. Generating new Solana wallet...")
    val wallet = Solana.generateWallet()
    val address = wallet.getAddress(0)
    println("   Address: $address")

    // 2. Create wallet from mnemonic
    println("\n2. Creating wallet from mnemonic...")
    val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
    val walletFromMnemonic = Solana.createWallet(mnemonic)
    val mnemonicAddress = walletFromMnemonic.getAddress(0)
    println("   Address: $mnemonicAddress")

    // 3. Generate multiple addresses
    println("\n3. Generating multiple addresses...")
    val accounts = wallet.deriveAccounts(0, 3)
    accounts.forEach { account ->
        println("   Account ${account.index}: ${account.address}")
    }

    // 4. Create a transfer transaction
    println("\n4. Creating transfer transaction...")
    val fromPrivateKey = SolanaPrivateKey.generate()
    val fromAddress = fromPrivateKey.publicKey()
    val toAddress = Solana.publicKey("11111111111111111111111111111111") // System program as example

    val lamports = Solana.solToLamports(0.001) // 0.001 SOL
    val recentBlockhash = "11111111111111111111111111111111" // Dummy blockhash

    val transaction = Solana.createTransfer(
        from = fromAddress,
        to = toAddress,
        lamports = lamports,
        recentBlockhash = recentBlockhash
    )

    // 5. Sign the transaction
    println("   Signing transaction...")
    transaction.sign(fromPrivateKey)

    // 6. Get transaction details
    val serialized = transaction.serialize()
    println("   Transaction size: ${serialized.size} bytes")
    println("   Signatures: ${transaction.signatures.size}")
    println("   From: ${fromAddress.toBase58()}")
    println("   To: ${toAddress.toBase58()}")
    println("   Amount: ${Solana.lamportsToSol(lamports)} SOL")

    // 7. Validate addresses
    println("\n5. Address validation...")
    val validAddresses = listOf(
        "11111111111111111111111111111111",
        mnemonicAddress,
        address
    )

    validAddresses.forEach { addr ->
        val isValid = Solana.isValidAddress(addr)
        println("   $addr: ${if (isValid) "✓ Valid" else "✗ Invalid"}")
    }

    // 8. Conversion examples
    println("\n6. Conversion examples...")
    println("   1 SOL = ${Solana.solToLamports(1.0)} lamports")
    println("   1,000,000 lamports = ${Solana.lamportsToSol(1_000_000)} SOL")

    println("\n=== Example Complete ===")
}

/**
 * Real-world usage example for creating and sending a transaction
 */
fun realWorldExample() {
    // This is how you would use it in a real application:

    // 1. Create wallet from user's mnemonic
    val userMnemonic = "your twelve word mnemonic phrase goes here and so on"
    val wallet = SolanaWallet.fromMnemonic(userMnemonic)

    // 2. Get user's address and private key
    val userAddress = wallet.getAddress(0)
    val userPrivateKey = wallet.getPrivateKey(0)

    // 3. Create transfer to another address
    val recipientAddress = "RecipientSolanaAddressGoesHere123456789"
    val amountSol = 0.1 // 0.1 SOL

    // 4. You would get this from Solana RPC
    val recentBlockhash = "get_this_from_solana_rpc_getLatestBlockhash"

    // 5. Create transaction
    val transaction = Solana.createTransfer(
        fromAddress = userAddress,
        toAddress = recipientAddress,
        lamports = Solana.solToLamports(amountSol),
        recentBlockhash = recentBlockhash
    )

    // 6. Sign transaction
    transaction.sign(userPrivateKey)

    // 7. Serialize for sending to network
    val signedTransaction = transaction.serialize()

    // 8. Send to Solana network via RPC
    // sendToSolanaNetwork(signedTransaction)
}