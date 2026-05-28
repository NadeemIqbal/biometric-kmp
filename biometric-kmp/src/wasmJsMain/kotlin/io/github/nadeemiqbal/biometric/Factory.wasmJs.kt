package io.github.nadeemiqbal.biometric

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Create a web authenticator for non-Compose call sites.
 *
 * @param store where the registered credential id is persisted. Defaults to `localStorage`.
 * @param relyingPartyName name the browser shows during credential registration.
 */
fun BiometricAuthenticator(
    store: CredentialStore = LocalStorageCredentialStore(),
    relyingPartyName: String = "biometric-kmp",
): BiometricAuthenticator = WebBiometricAuthenticator(store, relyingPartyName)

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator =
    remember { WebBiometricAuthenticator() }
