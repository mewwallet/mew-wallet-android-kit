package com.myetherwallet.mewwalletkit.solana

import com.myetherwallet.mewwalletkit.bip.bip44.Network
import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.core.extension.decodeBase58
import java.nio.ByteBuffer
import java.nio.ByteOrder

object TokenProgram {
    const val PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
    const val ACCOUNT_SIZE: Int = 165

    fun programId(): PublicKey {
        val alphabet = Network.SOLANA.alphabet()
            ?: throw IllegalStateException("Failed to get Solana alphabet")
        val publicKeyBytes = PROGRAM_ID.decodeBase58(alphabet)
            ?: throw IllegalStateException("Failed to decode Token Program ID")
        return PublicKey(publicKeyBytes, Network.SOLANA)
    }

    fun transfer(
        source: PublicKey,
        destination: PublicKey,
        owner: PublicKey,
        amount: ULong,
        multiSigners: List<PublicKey> = emptyList(),
        tokenProgramId: PublicKey = programId()
    ): TransactionInstruction {
        val keys = mutableListOf<AccountMeta>()
        keys.add(AccountMeta(source, isSigner = false, isWritable = true))
        keys.add(AccountMeta(destination, isSigner = false, isWritable = true))
        keys.add(AccountMeta(owner, isSigner = multiSigners.isEmpty(), isWritable = false))
        multiSigners.forEach { signer ->
            keys.add(AccountMeta(signer, isSigner = true, isWritable = false))
        }

        val data = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN)
        data.put(3)
        data.putLong(amount.toLong())

        return TransactionInstruction(
            keys = keys,
            programId = tokenProgramId,
            data = data.array()
        )
    }
}

object AssociatedTokenProgram {

    const val PROGRAM_ID = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"

    fun programId(): PublicKey {
        val alphabet = Network.SOLANA.alphabet()
            ?: throw IllegalStateException("Failed to get Solana alphabet")
        val publicKeyBytes = PROGRAM_ID.decodeBase58(alphabet)
            ?: throw IllegalStateException("Failed to decode Associated Token Program ID")
        return PublicKey(publicKeyBytes, Network.SOLANA)
    }

    fun createAssociatedTokenAccount(
        payer: PublicKey,
        owner: PublicKey,
        mint: PublicKey,
        tokenProgramId: PublicKey = TokenProgram.programId()
    ): TransactionInstruction {
        val associatedTokenAddress = owner.associatedTokenAddress(
            tokenMint = mint,
            tokenProgramId = tokenProgramId
        )

        val keys = listOf(
            AccountMeta(payer, isSigner = true, isWritable = true),
            AccountMeta(associatedTokenAddress, isSigner = false, isWritable = true),
            AccountMeta(owner, isSigner = false, isWritable = false),
            AccountMeta(mint, isSigner = false, isWritable = false),
            AccountMeta(SystemProgram.programId(), isSigner = false, isWritable = false),
            AccountMeta(tokenProgramId, isSigner = false, isWritable = false)
        )

        return TransactionInstruction(
            keys = keys,
            programId = programId(),
            data = byteArrayOf()
        )
    }
}
