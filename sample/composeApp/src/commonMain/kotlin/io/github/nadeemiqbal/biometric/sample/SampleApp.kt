package io.github.nadeemiqbal.biometric.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nadeemiqbal.biometric.AuthResult
import io.github.nadeemiqbal.biometric.BiometricAvailability
import io.github.nadeemiqbal.biometric.BiometricCapability
import io.github.nadeemiqbal.biometric.BiometricPromptInfo
import io.github.nadeemiqbal.biometric.rememberBiometricAuthenticator
import kotlinx.coroutines.launch

@Composable
fun SampleApp() {
    var darkTheme by remember { mutableStateOf(false) }
    MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            val authenticator = rememberBiometricAuthenticator()
            val scope = rememberCoroutineScope()

            var availability by remember { mutableStateOf<BiometricAvailability?>(null) }
            var result by remember { mutableStateOf<AuthResult?>(null) }
            var unlocked by remember { mutableStateOf(false) }
            var allowDeviceCredential by remember { mutableStateOf(true) }
            var busy by remember { mutableStateOf(false) }

            suspend fun refresh() {
                availability = try {
                    authenticator.canAuthenticate()
                } catch (t: Throwable) {
                    BiometricAvailability.Unknown
                }
            }

            LaunchedEffect(authenticator) { refresh() }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            ) {
                Text(
                    text = "biometric-kmp",
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                )
                Text(
                    text = "One Compose Multiplatform call to gate content behind the platform's " +
                        "native user verification.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )

                LockBadge(unlocked = unlocked)

                StatusCard(
                    availabilityLabel = availability?.label() ?: "Checking...",
                    availabilityColor = statusColor(availability),
                    resultLabel = result?.label(),
                    resultColor = result?.let(::resultColor) ?: MaterialTheme.colorScheme.onSurface,
                    capabilities = authenticator.capabilities,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
                ) {
                    Text("Allow device PIN / passcode fallback", modifier = Modifier.weight(1f))
                    Switch(checked = allowDeviceCredential, onCheckedChange = { allowDeviceCredential = it })
                }

                Button(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            val outcome = authenticator.authenticate(
                                BiometricPromptInfo(
                                    title = "Unlock biometric-kmp",
                                    subtitle = "Confirm it is you",
                                    reason = "Unlock the sample",
                                    allowDeviceCredential = allowDeviceCredential,
                                ),
                            )
                            result = outcome
                            if (outcome is AuthResult.Success) unlocked = true
                            refresh()
                            busy = false
                        }
                    },
                    modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.height(0.dp))
                        Text("  Prompting...")
                    } else {
                        Text(if (unlocked) "Unlock again" else "Unlock")
                    }
                }

                if (unlocked) {
                    OutlinedButton(
                        onClick = {
                            unlocked = false
                            result = null
                        },
                        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
                    ) { Text("Lock") }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Dark theme")
                    Switch(checked = darkTheme, onCheckedChange = { darkTheme = it })
                }
            }
        }
    }
}

@Composable
private fun LockBadge(unlocked: Boolean) {
    val container = if (unlocked) Color(0xFF1B5E20) else Color(0xFF7A1F1F)
    Surface(
        color = container,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.size(120.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = if (unlocked) "UNLOCKED" else "LOCKED",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun StatusCard(
    availabilityLabel: String,
    availabilityColor: Color,
    resultLabel: String?,
    resultColor: Color,
    capabilities: Set<BiometricCapability>,
) {
    Card(
        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
        ) {
            LabeledValue("canAuthenticate()", availabilityLabel, availabilityColor)
            LabeledValue("last authenticate()", resultLabel ?: "not run yet", resultColor)
            LabeledValue(
                "capabilities",
                if (capabilities.isEmpty()) "none" else capabilities.joinToString { it.name },
                MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String, valueColor: Color) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

private fun BiometricAvailability.label(): String = when (this) {
    BiometricAvailability.Available -> "Available"
    BiometricAvailability.NotEnrolled -> "Not enrolled"
    BiometricAvailability.NoHardware -> "No hardware"
    BiometricAvailability.Unsupported -> "Unsupported on this platform"
    BiometricAvailability.Unknown -> "Unknown"
}

private fun AuthResult.label(): String = when (this) {
    AuthResult.Success -> "Success"
    AuthResult.Cancelled -> "Cancelled by user"
    is AuthResult.Failed ->
        "Not recognized" + (attemptsRemaining?.let { " ($it attempts left)" } ?: "")
    is AuthResult.Error -> "Error: $reason" + (message?.let { ": $it" } ?: "")
}

private val Available = Color(0xFF2E7D32)
private val Warn = Color(0xFFB26A00)
private val Bad = Color(0xFFC62828)

private fun statusColor(availability: BiometricAvailability?): Color = when (availability) {
    BiometricAvailability.Available -> Available
    BiometricAvailability.NotEnrolled -> Warn
    BiometricAvailability.NoHardware,
    BiometricAvailability.Unsupported,
    BiometricAvailability.Unknown,
    -> Bad
    null -> Warn
}

private fun resultColor(result: AuthResult): Color = when (result) {
    AuthResult.Success -> Available
    AuthResult.Cancelled -> Warn
    is AuthResult.Failed -> Warn
    is AuthResult.Error -> Bad
}
