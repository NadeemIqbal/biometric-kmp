package io.github.nadeemiqbal.biometric

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity

/** Create an authenticator bound to [activity]. Use this from non-Compose call sites. */
fun BiometricAuthenticator(activity: FragmentActivity): BiometricAuthenticator =
    AndroidBiometricAuthenticator(activity)

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator {
    val context = LocalContext.current
    val activity = remember(context) {
        context.findFragmentActivity()
            ?: error(
                "rememberBiometricAuthenticator requires the host Activity to be a FragmentActivity " +
                    "(ComponentActivity/AppCompatActivity qualify). Pass one explicitly with " +
                    "BiometricAuthenticator(activity) if it cannot be resolved from the composition.",
            )
    }
    return remember(activity) { AndroidBiometricAuthenticator(activity) }
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
