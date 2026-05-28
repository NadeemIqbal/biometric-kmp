package io.github.nadeemiqbal.biometric

/**
 * Desktop (JVM) authenticator. Compose Desktop runs on the JVM regardless of OS, so this single
 * backend dispatches on the host OS to a per-OS delegate.
 *
 * macOS uses Touch ID via [MacBiometricAuthenticator]. Windows Hello (a WinRT shim) and Linux have
 * no implementation yet and report [BiometricAvailability.Unsupported].
 */
internal class DesktopBiometricAuthenticator : BiometricAuthenticator {

    private enum class HostOs { MacOs, Windows, Linux, Other }

    private val hostOs: HostOs = run {
        val name = System.getProperty("os.name")?.lowercase().orEmpty()
        when {
            name.contains("mac") || name.contains("darwin") -> HostOs.MacOs
            name.contains("win") -> HostOs.Windows
            name.contains("nux") || name.contains("nix") -> HostOs.Linux
            else -> HostOs.Other
        }
    }

    private val delegate: BiometricAuthenticator? =
        if (hostOs == HostOs.MacOs) runCatching { MacBiometricAuthenticator() }.getOrNull() else null

    override val nativeHandle: Any?
        get() = delegate?.nativeHandle

    override suspend fun canAuthenticate(): BiometricAvailability =
        delegate?.canAuthenticate() ?: BiometricAvailability.Unsupported

    override suspend fun authenticate(prompt: BiometricPromptInfo): AuthResult =
        delegate?.authenticate(prompt)
            ?: AuthResult.Error(
                reason = AuthError.Unsupported,
                message = "Desktop biometric authentication is not yet implemented for host OS: $hostOs.",
            )
}
