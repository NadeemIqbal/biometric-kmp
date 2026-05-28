package io.github.nadeemiqbal.biometric

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Create an iOS/macOS authenticator for non-Compose call sites. */
fun BiometricAuthenticator(): BiometricAuthenticator = IosBiometricAuthenticator()

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator =
    remember { IosBiometricAuthenticator() }
