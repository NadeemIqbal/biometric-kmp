package io.github.nadeemiqbal.biometric

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorAuthenticationFailed
import platform.LocalAuthentication.LAErrorBiometryLockout
import platform.LocalAuthentication.LAErrorBiometryNotAvailable
import platform.LocalAuthentication.LAErrorBiometryNotEnrolled
import platform.LocalAuthentication.LAErrorPasscodeNotSet
import platform.LocalAuthentication.LAErrorSystemCancel
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAErrorUserFallback
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume

/** [BiometricAuthenticator] backed by the iOS/macOS `LocalAuthentication` framework. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class IosBiometricAuthenticator : BiometricAuthenticator {

    private val context = LAContext()

    override val nativeHandle: Any
        get() = context

    private fun policy(allowDeviceCredential: Boolean): Long =
        if (allowDeviceCredential) {
            LAPolicyDeviceOwnerAuthentication
        } else {
            LAPolicyDeviceOwnerAuthenticationWithBiometrics
        }

    override suspend fun canAuthenticate(): BiometricAvailability = memScoped {
        val errorVar = alloc<ObjCObjectVar<NSError?>>()
        val canEvaluate = LAContext().canEvaluatePolicy(
            policy(allowDeviceCredential = true),
            errorVar.ptr,
        )
        if (canEvaluate) return@memScoped BiometricAvailability.Available
        when (errorVar.value?.code) {
            LAErrorBiometryNotEnrolled, LAErrorPasscodeNotSet -> BiometricAvailability.NotEnrolled
            LAErrorBiometryNotAvailable -> BiometricAvailability.NoHardware
            null -> BiometricAvailability.Unknown
            else -> BiometricAvailability.Unknown
        }
    }

    override suspend fun authenticate(prompt: BiometricPromptInfo): AuthResult =
        suspendCancellableCoroutine { cont ->
            context.evaluatePolicy(
                policy(prompt.allowDeviceCredential),
                localizedReason = prompt.reason,
            ) { success, error ->
                if (!cont.isActive) return@evaluatePolicy
                if (success) {
                    cont.resume(AuthResult.Success)
                } else {
                    cont.resume(mapError(error))
                }
            }
        }

    private fun mapError(error: NSError?): AuthResult = when (error?.code) {
        LAErrorUserCancel, LAErrorSystemCancel, LAErrorUserFallback -> AuthResult.Cancelled
        LAErrorAuthenticationFailed -> AuthResult.Failed()
        LAErrorBiometryLockout -> AuthResult.Error(AuthError.Lockout, error.localizedDescription)
        LAErrorBiometryNotEnrolled, LAErrorPasscodeNotSet ->
            AuthResult.Error(AuthError.NotEnrolled, error.localizedDescription)
        LAErrorBiometryNotAvailable ->
            AuthResult.Error(AuthError.NoHardware, error.localizedDescription)
        else -> AuthResult.Error(AuthError.Unknown, error?.localizedDescription)
    }
}
