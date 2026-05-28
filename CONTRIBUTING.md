# Contributing to biometric-kmp

Thanks for your interest in improving this library. Contributions of all kinds are welcome:
bug reports, feature requests, docs, new platform backends, and code.

## Project layout

```
biometric-kmp/                         The published Kotlin Multiplatform library.
  src/commonMain/                      Public API + shared types.
    BiometricAuthenticator.kt           The core interface.
    BiometricAvailability.kt            canAuthenticate() status type.
    AuthResult.kt                       authenticate() result + AuthError.
    BiometricPromptInfo.kt              Prompt configuration.
    Capabilities.kt                     Opt-in capability interfaces + BiometricCapability.
    CredentialStore.kt                  Pluggable store for the web credential id.
    Factory.kt                          expect rememberBiometricAuthenticator().
  src/androidMain/                     actual backend over androidx.biometric BiometricPrompt.
  src/iosMain/                         actual backend over LocalAuthentication LAContext.
  src/desktopMain/                     actual backend; dispatches on host OS.
    DesktopBiometricAuthenticator.kt    OS dispatch (macOS -> Touch ID, else Unsupported).
    MacBiometricAuthenticator.kt        macOS LocalAuthentication driver.
    ObjC.kt                             JNA bridge to the Objective-C runtime + a global block.
  src/wasmJsMain/                      actual backend; WebAuthn local gate.
    WebBiometricAuthenticator.kt        register-then-verify against a platform authenticator.
    WebAuthnInterop.wasmJs.kt           navigator.credentials + localStorage interop.
    LocalStorageCredentialStore.kt      default CredentialStore.
  src/commonTest/                      Pure-logic tests, run on every target.
sample/composeApp/                     Shared Compose sample UI (Unlock screen).
sample/androidApp/                     Android launcher (FragmentActivity host).
sample/desktopApp/                     Desktop (JVM) launcher.
sample/webApp/                         Web (wasmJs) launcher.
sample/iosApp/                         iOS launcher (standalone Xcode project).
```

## Where to put what

- **New platform backend** goes in its source set as `XxxBiometricAuthenticator.kt`, wired in
  through that target's `Factory.<target>.kt` actual.
- **New advanced feature** is added as an opt-in capability interface in `Capabilities.kt`, never
  as a new method on `BiometricAuthenticator`. A backend that supports it implements the interface
  and advertises it in `capabilities`; callers discover it with `as?`. This keeps the core API
  stable as native depth grows.
- **Keep result and status types forward compatible.** Map new native outcomes onto the existing
  `Unknown` / `Other` arms rather than adding a breaking case.
- Larger feature sets (crypto-bound keys, full server-backed WebAuthn) are intended to ship as
  separate optional modules (`:biometric-kmp-crypto`, `:biometric-kmp-webauthn`) depending on this
  core, so the base library stays small.

## Building & testing

```bash
./gradlew :biometric-kmp:desktopTest                    # commonTest on JVM, fastest feedback
./gradlew :biometric-kmp:compileReleaseKotlinAndroid    # Android
./gradlew :biometric-kmp:compileKotlinIosSimulatorArm64 # iOS
./gradlew :biometric-kmp:compileKotlinDesktop           # Desktop (JVM)
./gradlew :biometric-kmp:compileKotlinWasmJs            # Web (wasmJs)
./gradlew :biometric-kmp:publishToMavenLocal            # dry-run release (POM + signatures)
./gradlew :sample:desktopApp:run                        # run the sample (Desktop)
```

There is no biometric hardware in CI, so the cross-platform jobs compile each target and run the
pure-logic tests. Anything that actually drives a prompt is verified by hand:

- **Android**: enrolled fingerprint device or emulator, exercise Success / Cancelled / NotEnrolled.
- **iOS**: simulator Face ID match and no-match.
- **macOS desktop**: a signed and entitled build (an unsigned `./gradlew run` generally will not
  show the system Touch ID dialog).
- **Web**: a real browser with a platform authenticator over HTTPS or `localhost`.

## Conventions

- Public API gets KDoc.
- Add or update tests for every behavior change. API-shape and status-mapping logic belongs in
  `commonTest`.
- Prefer `kotlinx.coroutines` and Kotlin stdlib over platform-specific APIs in `commonMain`.
- Add a `CHANGELOG.md` entry under `## [Unreleased]`.
- Do not use em dashes in prose or code; use a hyphen, comma, colon, or split the sentence.

## Submitting a PR

1. Fork and create a topic branch.
2. Make your change, with tests.
3. Make sure the compile tasks above and `:biometric-kmp:desktopTest` pass.
4. Open a PR against `main`.

## Releasing

Releases are published from a maintainer's machine, not from CI. Run
`./gradlew :biometric-kmp:publishToMavenLocal` first as a dry run, then
`./gradlew :biometric-kmp:publishAndReleaseToMavenCentral` (via the
`com.vanniktech.maven.publish` plugin) to publish to Maven Central.
