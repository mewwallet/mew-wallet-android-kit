package com.myetherwallet.mewwalletkit.solana.token

import com.myetherwallet.mewwalletkit.bip.bip44.PublicKey
import com.myetherwallet.mewwalletkit.solana.encoding.BinaryWriter
import com.myetherwallet.mewwalletkit.solana.instruction.SolanaAccountMeta
import com.myetherwallet.mewwalletkit.solana.instruction.SolanaTransactionInstruction
import com.myetherwallet.mewwalletkit.solana.core.SolanaPublicKey

/**
 * SPL Token Program implementation
 * Handles all standard token operations on Solana
 */
object TokenProgram {

    /**
     * SPL Token Program ID
     */
    val PROGRAM_ID = PublicKey.fromSolanaBase58("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")

    /**
     * Standard token account size in bytes
     */
    const val ACCOUNT_SIZE = 165L

    /**
     * Standard mint account size in bytes
     */
    const val MINT_SIZE = 82L

    /**
     * Token instruction types
     */
    private object Instructions {
        const val INITIALIZE_MINT = 0
        const val INITIALIZE_ACCOUNT = 1
        const val INITIALIZE_MULTISIG = 2
        const val TRANSFER = 3
        const val APPROVE = 4
        const val REVOKE = 5
        const val SET_AUTHORITY = 6
        const val MINT_TO = 7
        const val BURN = 8
        const val CLOSE_ACCOUNT = 9
        const val FREEZE_ACCOUNT = 10
        const val THAW_ACCOUNT = 11
        const val TRANSFER_CHECKED = 12
        const val APPROVE_CHECKED = 13
        const val MINT_TO_CHECKED = 14
        const val BURN_CHECKED = 15
    }

    /**
     * Create a transfer instruction
     * @param source Source token account
     * @param destination Destination token account
     * @param owner Owner of source account
     * @param amount Amount to transfer (in token's smallest unit)
     * @return Transfer instruction
     */
    fun transfer(
        source: PublicKey,
        destination: PublicKey,
        owner: PublicKey,
        amount: Long
    ): SolanaTransactionInstruction {
        require(amount > 0) { "Transfer amount must be positive" }

        val data = BinaryWriter.write { writer ->
            writer.writeByte(Instructions.TRANSFER.toByte())
            writer.writeUInt64(amount)
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                SolanaAccountMeta.writable(source),
                SolanaAccountMeta.writable(destination),
                SolanaAccountMeta.signer(owner, isWritable = false)
            ),
            data = data
        )
    }

    /**
     * Create a checked transfer instruction (with explicit decimals)
     * @param source Source token account
     * @param mint Token mint
     * @param destination Destination token account
     * @param owner Owner of source account
     * @param amount Amount to transfer
     * @param decimals Token decimals
     * @return Checked transfer instruction
     */
    fun transferChecked(
        source: PublicKey,
        mint: PublicKey,
        destination: PublicKey,
        owner: PublicKey,
        amount: Long,
        decimals: Byte
    ): SolanaTransactionInstruction {
        require(amount > 0) { "Transfer amount must be positive" }
        require(decimals <= 9) { "Decimals must be <= 9" }

        val data = BinaryWriter.write { writer ->
            writer.writeByte(Instructions.TRANSFER_CHECKED.toByte())
            writer.writeUInt64(amount)
            writer.writeByte(decimals)
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                SolanaAccountMeta.writable(source),
                SolanaAccountMeta.readOnly(mint),
                SolanaAccountMeta.writable(destination),
                SolanaAccountMeta.signer(owner, isWritable = false)
            ),
            data = data
        )
    }

    /**
     * Create a mint to instruction
     * @param mint Token mint account
     * @param account Destination token account
     * @param authority Mint authority
     * @param amount Amount to mint
     * @return Mint instruction
     */
    fun mintTo(
        mint: PublicKey,
        account: PublicKey,
        authority: PublicKey,
        amount: Long
    ): SolanaTransactionInstruction {
        require(amount > 0) { "Mint amount must be positive" }

        val data = BinaryWriter.write { writer ->
            writer.writeByte(Instructions.MINT_TO.toByte())
            writer.writeUInt64(amount)
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                SolanaAccountMeta.writable(mint),
                SolanaAccountMeta.writable(account),
                SolanaAccountMeta.signer(authority, isWritable = false)
            ),
            data = data
        )
    }

    /**
     * Create a burn instruction
     * @param account Token account to burn from
     * @param mint Token mint
     * @param owner Owner of token account
     * @param amount Amount to burn
     * @return Burn instruction
     */
    fun burn(
        account: PublicKey,
        mint: PublicKey,
        owner: PublicKey,
        amount: Long
    ): SolanaTransactionInstruction {
        require(amount > 0) { "Burn amount must be positive" }

        val data = BinaryWriter.write { writer ->
            writer.writeByte(Instructions.BURN.toByte())
            writer.writeUInt64(amount)
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                SolanaAccountMeta.writable(account),
                SolanaAccountMeta.writable(mint),
                SolanaAccountMeta.signer(owner, isWritable = false)
            ),
            data = data
        )
    }

    /**
     * Create an initialize account instruction
     * @param account New token account
     * @param mint Token mint
     * @param owner Owner of the new account
     * @return Initialize account instruction
     */
    fun initializeAccount(
        account: PublicKey,
        mint: PublicKey,
        owner: PublicKey
    ): SolanaTransactionInstruction {
        val data = BinaryWriter.write { writer ->
            writer.writeByte(Instructions.INITIALIZE_ACCOUNT.toByte())
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                SolanaAccountMeta.writable(account),
                SolanaAccountMeta.readOnly(mint),
                SolanaAccountMeta.readOnly(owner),
                SolanaAccountMeta.readOnly(SolanaPublicKey.SYSTEM_PROGRAM) // Rent sysvar
            ),
            data = data
        )
    }

    /**
     * Create a close account instruction
     * @param account Token account to close
     * @param destination Where to send remaining SOL
     * @param owner Owner of token account
     * @return Close account instruction
     */
    fun closeAccount(
        account: PublicKey,
        destination: PublicKey,
        owner: PublicKey
    ): SolanaTransactionInstruction {
        val data = BinaryWriter.write { writer ->
            writer.writeByte(Instructions.CLOSE_ACCOUNT.toByte())
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                SolanaAccountMeta.writable(account),
                SolanaAccountMeta.writable(destination),
                SolanaAccountMeta.signer(owner, isWritable = false)
            ),
            data = data
        )
    }

    /**
     * Create an approve instruction
     * @param source Token account to approve spending from
     * @param delegate Account to approve
     * @param owner Owner of source account
     * @param amount Amount to approve
     * @return Approve instruction
     */
    fun approve(
        source: PublicKey,
        delegate: PublicKey,
        owner: PublicKey,
        amount: Long
    ): SolanaTransactionInstruction {
        require(amount > 0) { "Approve amount must be positive" }

        val data = BinaryWriter.write { writer ->
            writer.writeByte(Instructions.APPROVE.toByte())
            writer.writeUInt64(amount)
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                SolanaAccountMeta.writable(source),
                SolanaAccountMeta.readOnly(delegate),
                SolanaAccountMeta.signer(owner, isWritable = false)
            ),
            data = data
        )
    }

    /**
     * Create a revoke instruction
     * @param source Token account to revoke approval from
     * @param owner Owner of token account
     * @return Revoke instruction
     */
    fun revoke(
        source: PublicKey,
        owner: PublicKey
    ): SolanaTransactionInstruction {
        val data = BinaryWriter.write { writer ->
            writer.writeByte(Instructions.REVOKE.toByte())
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                SolanaAccountMeta.writable(source),
                SolanaAccountMeta.signer(owner, isWritable = false)
            ),
            data = data
        )
    }

    /**
     * Create an initialize mint instruction
     * @param mint Mint account to initialize
     * @param decimals Number of decimals for the mint
     * @param mintAuthority Mint authority
     * @param freezeAuthority Optional freeze authority
     * @return Initialize mint instruction
     */
    fun initializeMint(
        mint: PublicKey,
        decimals: Byte,
        mintAuthority: PublicKey,
        freezeAuthority: PublicKey? = null
    ): SolanaTransactionInstruction {
        require(decimals <= 9) { "Decimals must be <= 9" }

        val data = BinaryWriter.write { writer ->
            writer.writeByte(Instructions.INITIALIZE_MINT.toByte())
            writer.writeByte(decimals)
            writer.writePublicKey(mintAuthority)
            if (freezeAuthority != null) {
                writer.writeByte(1) // COption::Some
                writer.writePublicKey(freezeAuthority)
            } else {
                writer.writeByte(0) // COption::None
            }
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                SolanaAccountMeta.writable(mint),
                SolanaAccountMeta.readOnly(SolanaPublicKey.SYSTEM_PROGRAM) // Rent sysvar
            ),
            data = data
        )
    }

    /**
     * Authority types for setAuthority instruction
     */
    enum class AuthorityType(val value: Byte) {
        MINT_TOKENS(0),
        FREEZE_ACCOUNT(1),
        ACCOUNT_OWNER(2),
        CLOSE_ACCOUNT(3)
    }

    /**
     * Create a set authority instruction
     * @param account Account to change authority for
     * @param currentAuthority Current authority
     * @param authorityType Type of authority to change
     * @param newAuthority New authority (null to remove)
     * @return Set authority instruction
     */
    fun setAuthority(
        account: PublicKey,
        currentAuthority: PublicKey,
        authorityType: AuthorityType,
        newAuthority: PublicKey? = null
    ): SolanaTransactionInstruction {
        val data = BinaryWriter.write { writer ->
            writer.writeByte(Instructions.SET_AUTHORITY.toByte())
            writer.writeByte(authorityType.value)
            if (newAuthority != null) {
                writer.writeByte(1) // COption::Some
                writer.writePublicKey(newAuthority)
            } else {
                writer.writeByte(0) // COption::None
            }
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                SolanaAccountMeta.writable(account),
                SolanaAccountMeta.signer(currentAuthority, isWritable = false)
            ),
            data = data
        )
    }

    /**
     * Create a freeze account instruction
     * @param account Token account to freeze
     * @param mint Token mint
     * @param authority Freeze authority
     * @return Freeze instruction
     */
    fun freezeAccount(
        account: PublicKey,
        mint: PublicKey,
        authority: PublicKey
    ): SolanaTransactionInstruction {
        val data = BinaryWriter.write { writer ->
            writer.writeByte(Instructions.FREEZE_ACCOUNT.toByte())
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                SolanaAccountMeta.writable(account),
                SolanaAccountMeta.readOnly(mint),
                SolanaAccountMeta.signer(authority, isWritable = false)
            ),
            data = data
        )
    }

    /**
     * Create a thaw account instruction
     * @param account Token account to thaw
     * @param mint Token mint
     * @param authority Freeze authority
     * @return Thaw instruction
     */
    fun thawAccount(
        account: PublicKey,
        mint: PublicKey,
        authority: PublicKey
    ): SolanaTransactionInstruction {
        val data = BinaryWriter.write { writer ->
            writer.writeByte(Instructions.THAW_ACCOUNT.toByte())
        }

        return SolanaTransactionInstruction(
            programId = PROGRAM_ID,
            accounts = listOf(
                SolanaAccountMeta.writable(account),
                SolanaAccountMeta.readOnly(mint),
                SolanaAccountMeta.signer(authority, isWritable = false)
            ),
            data = data
        )
    }
}