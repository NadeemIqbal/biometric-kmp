# Changelog

All notable changes to biometric-kmp are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this
project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Core API: `BiometricAuthenticator` with `canAuthenticate()`, `authenticate()`, `capabilities`,
  and a `nativeHandle` escape hatch.
- Forward-compatible result and status types: `BiometricAvailability`, `AuthResult`, `AuthError`,
  `BiometricPromptInfo`.
- Capability interfaces designed for future native features (`CryptoBoundAuthenticator`,
  `WebAuthnAuthenticator`, `CredentialEnrollment`), advertised via `capabilities` and queried with
  `as?` so they slot in without breaking callers.
- Compose helper `rememberBiometricAuthenticator()` plus non-Compose constructors on every target.
- Android backend over `androidx.biometric` BiometricPrompt (full verify, API 24+).
- iOS backend over `LocalAuthentication` LAContext (Face ID / Touch ID).
- Web (wasmJs) WebAuthn local gate over `navigator.credentials`, with a pluggable
  `CredentialStore` (default backed by browser `localStorage`).
- Desktop macOS (JVM) Touch ID over `LocalAuthentication` via JNA. Windows and Linux report
  `Unsupported` on the same desktop target until their phases ship.
- Compose Multiplatform sample app under `sample/` (Android, iOS, Desktop, Web) with an Unlock
  screen that exercises `canAuthenticate()` and `authenticate()`.

### Changed

- Web `canAuthenticate()` now maps any failure of the platform-authenticator availability probe to
  `BiometricAvailability.Unknown` instead of propagating, so the status check is always safe to call
  before showing UI (verified by running the web sample).
