package io.github.nadeemiqbal.biometric

import androidx.compose.runtime.Composable

/**
 * Remember a [BiometricAuthenticator] bound to the current platform context.
 *
 * This is the primary cross-platform entry point. On Android it resolves the hosting
 * `FragmentActivity` from the composition; on iOS/macOS, desktop, and web it builds the
 * platform backend directly. For non-Compose call sites, use a platform constructor where one
 * is provided (e.g. `BiometricAuthenticator(activity)` on Android).
 */
@Composable
expect fun rememberBiometricAuthenticator(): BiometricAuthenticator
