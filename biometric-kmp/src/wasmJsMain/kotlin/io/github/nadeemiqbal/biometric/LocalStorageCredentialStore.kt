@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.github.nadeemiqbal.biometric

/** Default web [CredentialStore] backed by the browser `localStorage`. */
internal class LocalStorageCredentialStore : CredentialStore {

    override suspend fun get(key: String): String? = localStorageGet(key)?.toString()

    override suspend fun put(key: String, value: String) = localStorageSet(key, value)

    override suspend fun remove(key: String) = localStorageRemove(key)
}
