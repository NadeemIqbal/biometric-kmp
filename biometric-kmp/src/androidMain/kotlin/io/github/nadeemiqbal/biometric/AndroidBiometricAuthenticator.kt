package io.github.nadeemiqbal.biometric

import android.os.Handler
import android.os.Looper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * [BiometricAuthenticator] backed by AndroidX [BiometricPrompt].
 *
 * Build via [rememberBiometricAuthenticator] or the [BiometricAuthenticator] constructor function.
 */
internal class AndroidBiometricAuthenticator(
    private val activity: FragmentActivity,
) : BiometricAuthenticator {

    @Volatile
    private var lastPrompt: BiometricPrompt? = null

    override val nativeHandle: Any?
        get() = lastPrompt

    private fun authenticators(prompt: BiometricPromptInfo): Int =
        if (prompt.allowDeviceCredential) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }

    override suspend fun canAuthenticate(): BiometricAvailability {
        val manager = BiometricManager.from(activity)
        return when (manager.canAuthenticate(authenticators(BiometricPromptInfo()))) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.Available
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NotEnrolled
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            -> BiometricAvailability.NoHardware
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricAvailability.Unsupported
            else -> BiometricAvailability.Unknown
        }
    }

    override suspend fun authenticate(prompt: BiometricPromptInfo): AuthResult =
        suspendCancellableCoroutine { cont ->
            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (cont.isActive) cont.resume(AuthResult.Success)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (cont.isActive) cont.resume(mapError(errorCode, errString.toString()))
                }
                // onAuthenticationFailed: a single attempt was not recognized; the prompt stays
                // visible for retry, so we do not resume here.
            }

            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(prompt.title)
                .apply {
                    prompt.subtitle?.let { setSubtitle(it) }
                    prompt.description?.let { setDescription(it) }
                    setConfirmationRequired(prompt.confirmationRequired)
                    if (prompt.allowDeviceCredential) {
                        setAllowedAuthenticators(authenticators(prompt))
                    } else {
                        setNegativeButtonText(prompt.negativeButtonText)
                    }
                }
                .build()

            // BiometricPrompt must be created and shown on the main thread.
            Handler(Looper.getMainLooper()).post {
                if (!cont.isActive) return@post
                val biometricPrompt = BiometricPrompt(activity, executor, callback)
                lastPrompt = biometricPrompt
                cont.invokeOnCancellation { biometricPrompt.cancelAuthentication() }
                runCatching { biometricPrompt.authenticate(info) }
                    .onFailure { e ->
                        if (cont.isActive) {
                            cont.resume(AuthResult.Error(AuthError.Unknown, e.message, e))
                        }
                    }
            }
        }

    private fun mapError(code: Int, message: String): AuthResult = when (code) {
        BiometricPrompt.ERROR_USER_CANCELED,
        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
        BiometricPrompt.ERROR_CANCELED,
        -> AuthResult.Cancelled
        BiometricPrompt.ERROR_LOCKOUT -> AuthResult.Error(AuthError.Lockout, message)
        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> AuthResult.Error(AuthError.LockoutPermanent, message)
        BiometricPrompt.ERROR_NO_BIOMETRICS -> AuthResult.Error(AuthError.NotEnrolled, message)
        BiometricPrompt.ERROR_HW_NOT_PRESENT,
        BiometricPrompt.ERROR_HW_UNAVAILABLE,
        -> AuthResult.Error(AuthError.NoHardware, message)
        BiometricPrompt.ERROR_TIMEOUT -> AuthResult.Error(AuthError.Timeout, message)
        else -> AuthResult.Error(AuthError.Unknown, message)
    }
}
