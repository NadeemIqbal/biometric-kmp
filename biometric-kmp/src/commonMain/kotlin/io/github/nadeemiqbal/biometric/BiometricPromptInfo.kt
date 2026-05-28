package io.github.nadeemiqbal.biometric

/**
 * Text and behavior for the authentication prompt. Platforms use what they support and ignore
 * the rest (for example iOS Face ID shows [reason] as the LAContext localized reason, while
 * Android maps [title]/[subtitle]/[description] onto the BiometricPrompt dialog).
 *
 * @param title primary prompt title.
 * @param subtitle optional secondary line.
 * @param description optional longer body text.
 * @param reason short justification shown where the platform requires one (iOS/macOS, web).
 * @param negativeButtonText label for the cancel/fallback button where the platform shows one.
 * @param allowDeviceCredential allow falling back to device PIN/password/pattern.
 * @param confirmationRequired require an explicit confirmation step after a passive match.
 */
data class BiometricPromptInfo(
    val title: String = "Authenticate",
    val subtitle: String? = null,
    val description: String? = null,
    val reason: String = "Confirm it is you",
    val negativeButtonText: String = "Cancel",
    val allowDeviceCredential: Boolean = true,
    val confirmationRequired: Boolean = false,
)
