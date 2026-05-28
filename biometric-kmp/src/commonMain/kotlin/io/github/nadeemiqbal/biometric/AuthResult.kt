package io.github.nadeemiqbal.biometric

/**
 * Outcome of an [BiometricAuthenticator.authenticate] call.
 *
 * The set is closed except for [Error], which carries an open-ended [AuthError] reason so new
 * native failure categories map onto it without changing this hierarchy.
 */
sealed interface AuthResult {
    /** The user was verified successfully. */
    data object Success : AuthResult

    /** The user dismissed the prompt or pressed the negative button. */
    data object Cancelled : AuthResult

    /**
     * The prompt was shown but the presented biometric was not recognized. The prompt may still
     * be on screen for another attempt depending on the platform; [attemptsRemaining] is provided
     * when the platform reports it.
     */
    data class Failed(val attemptsRemaining: Int? = null) : AuthResult

    /** Authentication could not proceed or terminated for the given [reason]. */
    data class Error(
        val reason: AuthError,
        val message: String? = null,
        val cause: Throwable? = null,
    ) : AuthResult
}

/** Categorized reason behind an [AuthResult.Error]. [Unknown] keeps the enum forward compatible. */
enum class AuthError {
    NotAvailable,
    NotEnrolled,
    NoHardware,
    Lockout,
    LockoutPermanent,
    Timeout,
    Unsupported,
    Unknown,
}
