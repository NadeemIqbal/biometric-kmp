package io.github.nadeemiqbal.biometric

/**
 * Cross-platform entry point for biometric / local authentication.
 *
 * The high-level contract is a boolean-style verify: call [authenticate] and branch on the
 * [AuthResult]. Richer, platform-specific features are exposed through optional capability
 * interfaces ([CryptoBoundAuthenticator], [WebAuthnAuthenticator], [CredentialEnrollment]) that a
 * backend MAY also implement; discover them via [capabilities] and an `as?` cast. This keeps the
 * core API stable while leaving room to grow into full native functionality.
 *
 * Obtain an instance with the platform `rememberBiometricAuthenticator()` composable or a
 * platform constructor (e.g. `BiometricAuthenticator(activity)` on Android).
 */
interface BiometricAuthenticator {

    /** Current ability to authenticate. Cheap to call; safe before showing UI. */
    suspend fun canAuthenticate(): BiometricAvailability

    /** Present the platform prompt and verify the user. */
    suspend fun authenticate(prompt: BiometricPromptInfo = BiometricPromptInfo()): AuthResult

    /** Optional features this backend also implements. See [BiometricCapability]. */
    val capabilities: Set<BiometricCapability>
        get() = emptySet()

    /**
     * The underlying native object for advanced interop, or null. Examples by platform:
     * Android `BiometricPrompt`/`CryptoObject`, iOS/macOS `LAContext`, Windows verifier,
     * web `PublicKeyCredential`. Cast at the call site; treat as platform-specific.
     */
    val nativeHandle: Any?
        get() = null
}

/** Advertised optional capabilities, queried via [BiometricAuthenticator.capabilities]. */
enum class BiometricCapability {
    /** Implements [CryptoBoundAuthenticator]. */
    CryptoBoundKeys,

    /** Implements [WebAuthnAuthenticator]. */
    WebAuthn,

    /** Implements [CredentialEnrollment]. */
    CredentialEnrollment,
}
