package io.github.nadeemiqbal.biometric

import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Collections
import kotlin.coroutines.resume

/**
 * macOS Touch ID backend over the `LocalAuthentication` framework, reached through the [ObjC] shim.
 *
 * Requires a signed and entitled app bundle: an unsigned `./gradlew run` build typically will not
 * present the system prompt, so this is verified from a packaged build rather than in CI.
 */
internal class MacBiometricAuthenticator : BiometricAuthenticator {

    /** One reusable LAContext, also handed out via [nativeHandle] for advanced configuration. */
    private val context: Pointer? by lazy {
        ObjC.sendPtr(ObjC.sendPtr(ObjC.objClass("LAContext"), "alloc"), "init")
    }

    /** Pins live block/callback graphs so the GC cannot collect them before the reply fires. */
    private val pinned = Collections.synchronizedSet(mutableSetOf<Any>())

    override val nativeHandle: Any?
        get() = context

    private fun policy(allowDeviceCredential: Boolean): Long =
        if (allowDeviceCredential) POLICY_DEVICE_OWNER else POLICY_BIOMETRICS

    override suspend fun canAuthenticate(): BiometricAvailability {
        val errorRef = PointerByReference()
        val canEvaluate = ObjC.sendBool(
            context,
            "canEvaluatePolicy:error:",
            policy(allowDeviceCredential = true),
            errorRef,
        )
        if (canEvaluate) return BiometricAvailability.Available
        return when (errorRef.value?.let { ObjC.sendLong(it, "code") }) {
            LA_PASSCODE_NOT_SET, LA_BIOMETRY_NOT_ENROLLED -> BiometricAvailability.NotEnrolled
            LA_BIOMETRY_NOT_AVAILABLE -> BiometricAvailability.NoHardware
            else -> BiometricAvailability.Unknown
        }
    }

    override suspend fun authenticate(prompt: BiometricPromptInfo): AuthResult =
        suspendCancellableCoroutine { cont ->
            // The block stays pinned until the OS delivers the reply (even on cancel/dismiss); the
            // callback unpins itself so the block graph survives exactly as long as native needs it.
            var block: ObjC.Block? = null
            val callback = ObjC.ReplyCallback { _, success, error ->
                block?.let(pinned::remove)
                if (!cont.isActive) return@ReplyCallback
                cont.resume(if (success.toInt() != 0) AuthResult.Success else mapError(error))
            }
            block = ObjC.globalBlock(callback).also(pinned::add)

            ObjC.sendVoid(
                context,
                "evaluatePolicy:localizedReason:reply:",
                policy(prompt.allowDeviceCredential),
                ObjC.nsString(prompt.reason),
                block.pointer,
            )
        }

    private fun mapError(error: Pointer?): AuthResult {
        val code = error?.let { ObjC.sendLong(it, "code") }
        val message = error?.let { ObjC.nsStringToKotlin(ObjC.sendPtr(it, "localizedDescription")) }
        return when (code) {
            LA_USER_CANCEL, LA_SYSTEM_CANCEL, LA_USER_FALLBACK -> AuthResult.Cancelled
            LA_AUTHENTICATION_FAILED -> AuthResult.Failed()
            LA_BIOMETRY_LOCKOUT -> AuthResult.Error(AuthError.Lockout, message)
            LA_PASSCODE_NOT_SET, LA_BIOMETRY_NOT_ENROLLED ->
                AuthResult.Error(AuthError.NotEnrolled, message)
            LA_BIOMETRY_NOT_AVAILABLE -> AuthResult.Error(AuthError.NoHardware, message)
            else -> AuthResult.Error(AuthError.Unknown, message)
        }
    }

    private companion object {
        const val POLICY_BIOMETRICS = 1L
        const val POLICY_DEVICE_OWNER = 2L

        const val LA_AUTHENTICATION_FAILED = -1L
        const val LA_USER_CANCEL = -2L
        const val LA_USER_FALLBACK = -3L
        const val LA_SYSTEM_CANCEL = -4L
        const val LA_PASSCODE_NOT_SET = -5L
        const val LA_BIOMETRY_NOT_AVAILABLE = -6L
        const val LA_BIOMETRY_NOT_ENROLLED = -7L
        const val LA_BIOMETRY_LOCKOUT = -8L
    }
}
