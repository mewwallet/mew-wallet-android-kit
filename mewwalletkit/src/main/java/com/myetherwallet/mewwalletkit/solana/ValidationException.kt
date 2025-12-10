package com.myetherwallet.mewwalletkit.solana

/**
 * Exception thrown when transaction signature validation fails.
 *
 * Aggregates multiple signature issues discovered during validation.
 * Each error corresponds to a specific public key that has a missing or invalid signature.
 *
 * @property errors List of validation errors (missing or invalid signatures)
 */
class ValidationException(
    val errors: List<ValidationError>
) : Exception(formatMessage(errors)) {

    init {
        require(errors.isNotEmpty()) { "ValidationException must contain at least one error" }
    }

    companion object {
        /**
         * Creates a ValidationException with a single error.
         */
        fun single(error: ValidationError): ValidationException {
            return ValidationException(listOf(error))
        }

        /**
         * Formats the error list into a human-readable message.
         */
        private fun formatMessage(errors: List<ValidationError>): String {
            if (errors.isEmpty()) return "Transaction validation failed"

            val errorMessages = errors.joinToString(separator = "\n  - ") { it.toString() }
            return "Transaction validation failed with ${errors.size} error(s):\n  - $errorMessages"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ValidationException) return false
        return errors == other.errors
    }

    override fun hashCode(): Int {
        return errors.hashCode()
    }
}
