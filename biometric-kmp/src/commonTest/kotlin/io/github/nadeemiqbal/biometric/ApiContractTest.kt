package io.github.nadeemiqbal.biometric

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeAuthenticator(
    private val availability: BiometricAvailability,
    private val result: AuthResult,
    override val capabilities: Set<BiometricCapability> = emptySet(),
) : BiometricAuthenticator {
    override suspend fun canAuthenticate() = availability
    override suspend fun authenticate(prompt: BiometricPromptInfo) = result
}

class ApiContractTest {

    @Test
    fun defaultCapabilitiesAndNativeHandleAreEmpty() {
        val auth = FakeAuthenticator(BiometricAvailability.Available, AuthResult.Success)
        assertTrue(auth.capabilities.isEmpty())
        assertNull(auth.nativeHandle)
    }

    @Test
    fun promptInfoHasUsableDefaults() {
        val info = BiometricPromptInfo()
        assertEquals("Authenticate", info.title)
        assertTrue(info.allowDeviceCredential)
        assertEquals(false, info.confirmationRequired)
    }

    @Test
    fun authenticateReturnsConfiguredResult() = runTest {
        val auth = FakeAuthenticator(
            availability = BiometricAvailability.Available,
            result = AuthResult.Error(AuthError.Lockout, "locked"),
        )
        assertEquals(BiometricAvailability.Available, auth.canAuthenticate())
        val result = auth.authenticate()
        assertTrue(result is AuthResult.Error)
        assertEquals(AuthError.Lockout, result.reason)
    }

    @Test
    fun capabilityCastDiscoversOptionalFeatures() {
        val auth = FakeAuthenticator(
            availability = BiometricAvailability.Available,
            result = AuthResult.Success,
            capabilities = setOf(BiometricCapability.WebAuthn),
        )
        assertTrue(BiometricCapability.WebAuthn in auth.capabilities)
        // Fake does not implement the interface, so the cast must fail gracefully.
        val asAuthenticator: BiometricAuthenticator = auth
        assertNull(asAuthenticator as? WebAuthnAuthenticator)
    }
}
