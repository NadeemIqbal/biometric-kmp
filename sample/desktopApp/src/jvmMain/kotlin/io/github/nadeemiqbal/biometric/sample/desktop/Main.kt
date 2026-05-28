package io.github.nadeemiqbal.biometric.sample.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.nadeemiqbal.biometric.sample.SampleApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "biometric-kmp Sample",
        state = rememberWindowState(width = 480.dp, height = 800.dp),
    ) {
        SampleApp()
    }
}
