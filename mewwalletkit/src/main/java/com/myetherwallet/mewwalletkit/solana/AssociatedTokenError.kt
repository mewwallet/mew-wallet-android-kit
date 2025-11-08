package com.myetherwallet.mewwalletkit.solana

/**
 * Errors thrown while deriving associated token accounts or PDAs.
 */
sealed class AssociatedTokenError : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)

    /**
     * Owner must be on-curve when allowOwnerOffCurve == false
     */
    class OwnerOffCurve : AssociatedTokenError("Owner must be on-curve")

    /**
     * Unable to find a valid PDA for any bump (255…0)
     */
    class NoAddress : AssociatedTokenError("Unable to find a valid PDA for any bump")

    /**
     * A single seed exceeded 32 bytes
     */
    class MaxSeedLengthExceeded : AssociatedTokenError("A single seed exceeded 32 bytes")

    /**
     * Internal/format error constructing the public key
     */
    class InternalError(message: String = "Internal error") : AssociatedTokenError(message)

    /**
     * Derived point is on-curve (invalid for PDA)
     */
    class InvalidSeed : AssociatedTokenError("Derived point is on-curve (invalid for PDA)")

    /**
     * Wrapped dependency error
     */
    class Underlying(cause: Throwable) : AssociatedTokenError("Underlying error", cause)
}
