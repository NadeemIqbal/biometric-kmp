package io.github.nadeemiqbal.biometric

/**
 * Whether the current platform can perform a biometric / local authentication right now.
 *
 * [Unknown] is a forward-compatible catch-all: a backend returns it when the native layer
 * reports a state this version does not model, so adding new states later is non-breaking.
 */
enum class BiometricAvailability {
    /** Hardware present, user enrolled, ready to authenticate. */
    Available,

    /** Hardware present but the user has not enrolled any biometric or device credential. */
    NotEnrolled,

    /** No biometric hardware on this device. */
    NoHardware,

    /** This platform/build cannot perform local authentication (e.g. Linux desktop, unsigned app). */
    Unsupported,

    /** A native state not modeled by this version. */
    Unknown,
}
