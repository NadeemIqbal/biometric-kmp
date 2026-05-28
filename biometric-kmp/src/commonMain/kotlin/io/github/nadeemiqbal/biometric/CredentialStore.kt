package io.github.nadeemiqbal.biometric

/**
 * Pluggable persistence for the web local-gate credential id.
 *
 * The web backend ([BiometricAuthenticator] on wasmJs) registers a platform WebAuthn credential on
 * first use and keeps its id so later unlocks can target it with `navigator.credentials.get()`.
 * Where that id lives is the app's choice: the default persists to the browser `localStorage`, but
 * an app that already has secure storage (or a server) can supply its own implementation.
 *
 * Other platforms do not use this; their verification is stateless.
 */
interface CredentialStore {

    /** Return the stored value for [key], or null if absent. */
    suspend fun get(key: String): String?

    /** Persist [value] under [key], replacing any existing value. */
    suspend fun put(key: String, value: String)

    /** Remove any value stored under [key]. */
    suspend fun remove(key: String)
}
