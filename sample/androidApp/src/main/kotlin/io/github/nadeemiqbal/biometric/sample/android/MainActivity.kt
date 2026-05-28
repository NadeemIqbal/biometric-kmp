package io.github.nadeemiqbal.biometric.sample.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import io.github.nadeemiqbal.biometric.sample.SampleApp

// FragmentActivity is required: rememberBiometricAuthenticator() resolves the host
// FragmentActivity from the composition for androidx.biometric BiometricPrompt.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { SampleApp() }
    }
}
