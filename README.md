# biometric-kmp

**One Compose Multiplatform API to gate app content behind the platform's native user
verification.** Face ID / Touch ID, Android BiometricPrompt, device PIN fallback, with a layered
design that leaves room to grow into crypto-bound keys and full WebAuthn without breaking callers.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.nadeemiqbal/biometric-kmp)](https://central.sonatype.com/artifact/io.github.nadeemiqbal/biometric-kmp)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?logo=kotlin)](https://kotlinlang.org)
![Android](https://img.shields.io/badge/Android-24%2B-3DDC84?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-x64%20%7C%20arm64%20%7C%20simulator-000000?logo=apple&logoColor=white)
![Desktop](https://img.shields.io/badge/Desktop-JVM-007396?logo=openjdk&logoColor=white)
![Web](https://img.shields.io/badge/Web-wasmJs-654FF0?logo=webassembly&logoColor=white)

> ### Using a pure Android Compose project? You're covered.
>
> `biometric-kmp` is published as a Kotlin Multiplatform library, but **you don't need any KMP
> setup to use it**. Drop it into a regular `:app` module's `dependencies { ... }` block and
> Gradle picks the Android variant automatically, same as Coil, Ktor, or any other modern KMP
> library.
>
> ```kotlin
> // app/build.gradle.kts
> dependencies {
>     implementation("io.github.nadeemiqbal:biometric-kmp:0.1.0")
> }
> ```
>
> Requires **`minSdk >= 24`**, and the host must be a `FragmentActivity` (a
> `ComponentActivity` / `AppCompatActivity` qualifies). See
> [Android-only setup](#android-only-setup) below for the full snippet.

## Why this library

Biometric prompts are easy on a single platform and miserable across four. `androidx.biometric`
covers Android, `LocalAuthentication` covers Apple, and the existing Kotlin options
(moko-biometry) stop at Android plus iOS. `biometric-kmp` gives you **one suspend call** that
returns a typed result on every Compose Multiplatform target, plus a deliberate extension surface
so deeper native features can be added later as opt-in capabilities, never as breaking changes to
the core API.

## Platform support

| Platform | behavior | Native mechanism | Tested |
|----------|----------|------------------|--------|
| Android  | Full verify | `androidx.biometric` BiometricPrompt | unit + compile |
| iOS      | Full verify | `LocalAuthentication` LAContext (Face ID / Touch ID) | compile |
| Web (wasmJs) | Local gate | WebAuthn platform authenticator (`navigator.credentials`) | compile (browser test pending) |
| Desktop macOS (JVM) | Touch ID | `LocalAuthentication` LAContext via JNA | compile (needs signed app to run) |
| Desktop Windows / Linux (JVM) | Reports `Unsupported` | Windows Hello shim lands later; Linux has no standard API | compile |

On the single desktop (JVM) target the backend dispatches on the host OS: macOS drives Touch ID,
while Windows and Linux report a clean `Unsupported` until their phases ship. So an app can target
all platforms now and light up the rest with no API change. See
[Web (WebAuthn local gate)](#web-webauthn-local-gate) and
[Desktop macOS (Touch ID)](#desktop-macos-touch-id) for the per-target caveats.

## Installation

### Compose Multiplatform setup

`gradle/libs.versions.toml`:

```toml
[libraries]
biometric-kmp = { module = "io.github.nadeemiqbal:biometric-kmp", version = "0.1.0" }
```

`commonMain` dependencies:

```kotlin
commonMain.dependencies {
    implementation(libs.biometric.kmp)
}
```

### Android-only setup

If you're not using Compose Multiplatform, just a regular `com.android.application` /
`com.android.library` module with Compose, add the dependency straight to the Android module:

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("io.github.nadeemiqbal:biometric-kmp:0.1.0")
}

android {
    defaultConfig { minSdk = 24 }   // biometric-kmp requires API 24+
}
```

The library declares the `USE_BIOMETRIC` permission in its manifest, so you do not need to add it
yourself. The host Activity must be a `FragmentActivity` (most apps already use
`ComponentActivity` / `AppCompatActivity`, both of which qualify).

## Quick start

```kotlin
@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val authenticator = rememberBiometricAuthenticator()
    val scope = rememberCoroutineScope()

    Button(onClick = {
        scope.launch {
            when (val result = authenticator.authenticate(
                BiometricPromptInfo(
                    title = "Unlock",
                    subtitle = "Confirm it is you",
                ),
            )) {
                AuthResult.Success -> onUnlocked()
                AuthResult.Cancelled -> { /* user dismissed */ }
                is AuthResult.Failed -> { /* not recognized, prompt may retry */ }
                is AuthResult.Error -> when (result.reason) {
                    AuthError.NotEnrolled -> { /* send user to settings */ }
                    else -> { /* show result.message */ }
                }
            }
        }
    }) { Text("Unlock") }
}
```

Check availability before showing UI:

```kotlin
when (authenticator.canAuthenticate()) {
    BiometricAvailability.Available -> { /* show the unlock button */ }
    BiometricAvailability.NotEnrolled -> { /* prompt to enroll */ }
    BiometricAvailability.NoHardware,
    BiometricAvailability.Unsupported -> { /* fall back to app passcode */ }
    BiometricAvailability.Unknown -> { /* treat as unavailable */ }
}
```

## API model

The surface is intentionally small:

```kotlin
interface BiometricAuthenticator {
    suspend fun canAuthenticate(): BiometricAvailability
    suspend fun authenticate(prompt: BiometricPromptInfo = BiometricPromptInfo()): AuthResult
    val capabilities: Set<BiometricCapability>   // advertised opt-in features
    val nativeHandle: Any?                        // escape hatch to the platform object
}
```

- **`BiometricAvailability`**: `Available`, `NotEnrolled`, `NoHardware`, `Unsupported`, `Unknown`.
- **`AuthResult`**: `Success`, `Cancelled`, `Failed(attemptsRemaining)`, `Error(reason, message, cause)`.
  Only `Error` is open-ended, carrying an `AuthError` so new native failure categories map on
  without changing the hierarchy.
- **`BiometricPromptInfo`**: `title`, `subtitle`, `description`, `reason`, `negativeButtonText`,
  `allowDeviceCredential` (default `true`), `confirmationRequired` (default `false`). Each platform
  uses the fields it supports and ignores the rest.

### Obtaining an instance

| Platform | Composable | Non-Compose constructor |
|----------|-----------|-------------------------|
| Android  | `rememberBiometricAuthenticator()` | `BiometricAuthenticator(activity: FragmentActivity)` |
| iOS      | `rememberBiometricAuthenticator()` | `BiometricAuthenticator()` |
| Desktop  | `rememberBiometricAuthenticator()` | `BiometricAuthenticator()` |
| Web      | `rememberBiometricAuthenticator()` | `BiometricAuthenticator()` |

The Android composable resolves the host `FragmentActivity` from the composition automatically.

## Designed to grow into full native functionality

The core stays a boolean-style verify because that is the dominant use case. Richer features are
modeled now and implemented later as **optional capability interfaces** a backend MAY also
implement. You discover them at runtime and never break when they appear:

```kotlin
val authenticator = rememberBiometricAuthenticator()

if (BiometricCapability.CryptoBoundKeys in authenticator.capabilities) {
    val crypto = authenticator as? CryptoBoundAuthenticator
    crypto?.authenticateForKey("my-key-alias", BiometricPromptInfo())
}
```

Planned capability interfaces (designed in v0.1, implemented in add-on modules later):

- **`CryptoBoundAuthenticator`**: unlock a Keystore / Keychain-bound key (Android `CryptoObject`,
  Apple `SecAccessControl`).
- **`WebAuthnAuthenticator`**: full register / assert credential model, passkeys, server challenge
  handling, the full native web path.
- **`CredentialEnrollment`**: list / enroll / remove credentials where the platform allows.

Two more extension points keep power users from forking the library:

- **`nativeHandle`** exposes the underlying platform object (`BiometricPrompt`/`CryptoObject`,
  `LAContext`, and so on) for advanced interop.
- Result and status types are forward compatible (the `Unknown` / `Other` arms), so new platform
  behavior maps on without a breaking change.

## Web (WebAuthn local gate)

On the web target, `authenticate()` drives the browser's WebAuthn platform authenticator:

- **First call** registers a platform credential (`navigator.credentials.create`, behind Touch ID /
  Windows Hello / Android screen lock) and stores its id via a [`CredentialStore`](#).
- **Later calls** verify the user against that stored credential (`navigator.credentials.get`).

This gates local content without a server. It is **not** server-backed passkey verification: there
is no attestation check and the challenge is client-generated, so it proves "the same user with the
same device unlocked again", not a cryptographic assertion to your backend. Full server-backed
passkeys are the job of the planned `WebAuthnAuthenticator` capability.

The credential id is persisted via a pluggable `CredentialStore`. The default uses the browser
`localStorage`; supply your own to store it elsewhere:

```kotlin
val authenticator = BiometricAuthenticator(
    store = myCredentialStore,          // implements CredentialStore
    relyingPartyName = "My App",        // shown by the browser at registration
)
```

WebAuthn requires a secure context (HTTPS or `localhost`). Runtime behavior must be verified in a
real browser with a platform authenticator; CI only compiles this target.

## Desktop macOS (Touch ID)

The desktop backend talks to the macOS `LocalAuthentication` framework through JNA (the
Objective-C runtime plus a hand-built reply block), so no extra native artifact ships with the
library. `canAuthenticate()` maps `LAContext.canEvaluatePolicy`, and `authenticate()` drives
`evaluatePolicy:localizedReason:reply:`.

**The app must be signed and entitled to prompt.** An unsigned `./gradlew run` build generally will
not show the system Touch ID dialog. Package and sign the app (for example with the Compose Desktop
packaging tasks) and ensure code signing is in place before expecting a prompt. This path is
verified on-device rather than in CI.

Windows and Linux currently return `Unsupported` on the same desktop target.

## Sample app

A Compose Multiplatform sample lives under `sample/`. It queries `canAuthenticate()`, runs
`authenticate()` behind an Unlock button, and shows the typed result and advertised capabilities
on every target. The shared UI is in `sample/composeApp`; each platform has a thin launcher.

```bash
./gradlew :sample:desktopApp:run                          # Desktop (JVM)
./gradlew :sample:webApp:wasmJsBrowserDevelopmentRun      # Web (wasmJs)
./gradlew :sample:androidApp:installDebug                  # Android (device/emulator)
```

For iOS, open `sample/iosApp/iosApp.xcodeproj` in Xcode and run it on a simulator or device. See
[sample/iosApp/README.md](sample/iosApp/README.md) for the signing and Face ID enrollment steps.
The Android launcher hosts the UI in a `FragmentActivity`, which `rememberBiometricAuthenticator()`
requires.

## Roadmap

1. **v0.1**: core API, Android + iOS verify, web WebAuthn local gate, desktop macOS Touch ID;
   Windows / Linux desktop compile as `Unsupported`. (this release)
2. Desktop Windows Hello via a UserConsentVerifier shim.
3. Linux fallback, docs, 1.0.
4. `:biometric-kmp-crypto` and `:biometric-kmp-webauthn` add-on modules.

## License

Apache 2.0, see [LICENSE](LICENSE).
