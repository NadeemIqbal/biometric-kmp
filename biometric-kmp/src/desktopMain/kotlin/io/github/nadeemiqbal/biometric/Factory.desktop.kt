package io.github.nadeemiqbal.biometric

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Create a desktop authenticator for non-Compose call sites. */
fun BiometricAuthenticator(): BiometricAuthenticator = DesktopBiometricAuthenticator()

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator =
    remember { DesktopBiometricAuthenticator() }
