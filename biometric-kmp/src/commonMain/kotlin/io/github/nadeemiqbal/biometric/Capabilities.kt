package io.github.nadeemiqbal.biometric

/**
 * Optional capability: gate access to a hardware-backed key behind user verification.
 * On Android this maps to a `BiometricPrompt.CryptoObject` bound to an Android Keystore key; on
 * Apple platforms to a Keychain item protected by a `SecAccessControl`.
 *
 * Designed in v1, implemented by the `biometric-kmp-crypto` add-on module later.
 */
interface CryptoBoundAuthenticator {
    /** Verify the user and unlock the key identified by [keyAlias]. */
    suspend fun authenticateForKey(
        keyAlias: String,
        prompt: BiometricPromptInfo = BiometricPromptInfo(),
    ): CryptoAuthResult
}

/** Result of a [CryptoBoundAuthenticator] call. [handle] is the unlocked native crypto object. */
sealed interface CryptoAuthResult {
    data class Success(val handle: Any?) : CryptoAuthResult
    data object Cancelled : CryptoAuthResult
    data class Error(val reason: AuthError, val message: String? = null, val cause: Throwable? = null) : CryptoAuthResult
}

/**
 * Optional capability: the full WebAuthn / passkey credential model (register + assert with
 * challenge/response, attestation, server verification). The core [BiometricAuthenticator] only
 * exposes a local gate on web; this is the upgrade path.
 *
 * Designed in v1, implemented by the `biometric-kmp-webauthn` add-on module later.
 */
interface WebAuthnAuthenticator {
    suspend fun register(options: WebAuthnRegistrationOptions): WebAuthnCredential
    suspend fun assert(options: WebAuthnAssertionOptions): WebAuthnAssertion
}

data class WebAuthnRegistrationOptions(
    val challenge: ByteArray,
    val rpId: String,
    val rpName: String,
    val userId: ByteArray,
    val userName: String,
    val userDisplayName: String = userName,
)

data class WebAuthnAssertionOptions(
    val challenge: ByteArray,
    val rpId: String,
    val allowCredentialIds: List<ByteArray> = emptyList(),
)

data class WebAuthnCredential(
    val credentialId: ByteArray,
    val publicKey: ByteArray,
    val attestation: ByteArray? = null,
)

data class WebAuthnAssertion(
    val credentialId: ByteArray,
    val authenticatorData: ByteArray,
    val clientDataJson: ByteArray,
    val signature: ByteArray,
    val userHandle: ByteArray? = null,
)

/** Optional capability: enumerate and manage stored credentials where the platform allows it. */
interface CredentialEnrollment {
    suspend fun listCredentials(): List<String>
    suspend fun removeCredential(id: String): Boolean
}
