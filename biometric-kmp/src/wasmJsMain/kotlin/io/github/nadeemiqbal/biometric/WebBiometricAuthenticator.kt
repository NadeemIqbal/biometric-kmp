@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.github.nadeemiqbal.biometric

import kotlinx.coroutines.await

/**
 * Web (Wasm/JS) authenticator implementing a WebAuthn local gate.
 *
 * On first [authenticate] it registers a platform authenticator credential (Touch ID, Windows
 * Hello, Android screen lock behind the browser) and remembers its id via the supplied
 * [CredentialStore]. Later calls verify the user against that stored credential. This gates local
 * content without a server; full server-backed passkey verification is the job of the
 * [WebAuthnAuthenticator] capability, designed for but not implemented in v1.
 *
 * @param store where the registered credential id is persisted. Defaults to `localStorage`.
 * @param relyingPartyName human-readable name shown by the browser during registration.
 */
internal class WebBiometricAuthenticator(
    private val store: CredentialStore = LocalStorageCredentialStore(),
    private val relyingPartyName: String = "biometric-kmp",
) : BiometricAuthenticator {

    override suspend fun canAuthenticate(): BiometricAvailability {
        if (!webAuthnSupported()) return BiometricAvailability.Unsupported
        // A status probe must never throw: some browsers reject or mis-shape the availability
        // promise. Map any failure to the forward-compatible Unknown rather than crashing callers.
        return try {
            if (platformAuthenticatorAvailable().await<JsBoolean>().toBoolean()) {
                BiometricAvailability.Available
            } else {
                BiometricAvailability.NoHardware
            }
        } catch (t: Throwable) {
            BiometricAvailability.Unknown
        }
    }

    override suspend fun authenticate(prompt: BiometricPromptInfo): AuthResult {
        if (!webAuthnSupported()) {
            return AuthResult.Error(
                reason = AuthError.Unsupported,
                message = "WebAuthn is not available in this browser.",
            )
        }
        return try {
            when (val storedId = store.get(CREDENTIAL_KEY)) {
                null -> registerThenGate(prompt)
                else -> gateWith(storedId)
            }
        } catch (t: Throwable) {
            AuthResult.Error(AuthError.Unknown, t.message, t)
        }
    }

    private suspend fun registerThenGate(prompt: BiometricPromptInfo): AuthResult {
        val newId = registerPlatformCredential(relyingPartyName, prompt.title)
            .await<JsString?>()?.toString()
        return if (newId.isNullOrEmpty()) {
            AuthResult.Cancelled
        } else {
            store.put(CREDENTIAL_KEY, newId)
            AuthResult.Success
        }
    }

    private suspend fun gateWith(credentialId: String): AuthResult =
        if (assertPlatformCredential(credentialId).await<JsBoolean>().toBoolean()) {
            AuthResult.Success
        } else {
            AuthResult.Cancelled
        }

    private companion object {
        const val CREDENTIAL_KEY = "io.github.nadeemiqbal.biometric.credentialId"
    }
}
