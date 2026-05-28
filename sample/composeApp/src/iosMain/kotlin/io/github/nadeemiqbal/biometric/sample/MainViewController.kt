package io.github.nadeemiqbal.biometric.sample

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** iOS entry point, consumed by the sample/iosApp Xcode project as MainViewControllerKt. */
fun MainViewController(): UIViewController = ComposeUIViewController { SampleApp() }
