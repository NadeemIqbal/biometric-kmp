@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.github.nadeemiqbal.biometric

import kotlin.js.Promise

/** True when the browser exposes the WebAuthn API at all. */
internal fun webAuthnSupported(): Boolean =
    js("typeof window !== 'undefined' && typeof window.PublicKeyCredential !== 'undefined'")

/** Resolves true when a user-verifying platform authenticator (Touch ID, Hello, etc.) is present. */
internal fun platformAuthenticatorAvailable(): Promise<JsBoolean> =
    js("PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable()")

/**
 * Create a platform credential and resolve to its base64url raw id, or null if the user dismissed
 * the prompt (NotAllowedError). Any other failure rejects so the caller can map it.
 */
internal fun registerPlatformCredential(
    rpName: String,
    userName: String,
): Promise<JsString?> =
    js(
        """
        (function () {
          function b64url(buf) {
            var bytes = new Uint8Array(buf);
            var str = '';
            for (var i = 0; i < bytes.length; i++) str += String.fromCharCode(bytes[i]);
            return btoa(str).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+${'$'}/, '');
          }
          var challenge = new Uint8Array(32);
          crypto.getRandomValues(challenge);
          var userId = new Uint8Array(16);
          crypto.getRandomValues(userId);
          var options = {
            publicKey: {
              challenge: challenge,
              rp: { name: rpName },
              user: { id: userId, name: userName, displayName: userName },
              pubKeyCredParams: [ { type: 'public-key', alg: -7 }, { type: 'public-key', alg: -257 } ],
              authenticatorSelection: {
                authenticatorAttachment: 'platform',
                userVerification: 'required',
                residentKey: 'preferred'
              },
              timeout: 60000,
              attestation: 'none'
            }
          };
          return navigator.credentials.create(options)
            .then(function (cred) { return b64url(cred.rawId); })
            .catch(function (e) { if (e && e.name === 'NotAllowedError') return null; throw e; });
        })()
        """,
    )

/**
 * Verify the user against a previously stored credential id. Resolves true on success, false if the
 * user dismissed the prompt; rejects on other failures.
 */
internal fun assertPlatformCredential(credentialIdB64Url: String): Promise<JsBoolean> =
    js(
        """
        (function () {
          function fromB64url(s) {
            s = s.replace(/-/g, '+').replace(/_/g, '/');
            while (s.length % 4) s += '=';
            var bin = atob(s);
            var bytes = new Uint8Array(bin.length);
            for (var i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
            return bytes;
          }
          var challenge = new Uint8Array(32);
          crypto.getRandomValues(challenge);
          var options = {
            publicKey: {
              challenge: challenge,
              allowCredentials: [ { type: 'public-key', id: fromB64url(credentialIdB64Url) } ],
              userVerification: 'required',
              timeout: 60000
            }
          };
          return navigator.credentials.get(options)
            .then(function () { return true; })
            .catch(function (e) { if (e && e.name === 'NotAllowedError') return false; throw e; });
        })()
        """,
    )

internal fun localStorageGet(key: String): JsString? =
    js("(typeof localStorage !== 'undefined') ? localStorage.getItem(key) : null")

internal fun localStorageSet(key: String, value: String) {
    js("if (typeof localStorage !== 'undefined') localStorage.setItem(key, value)")
}

internal fun localStorageRemove(key: String) {
    js("if (typeof localStorage !== 'undefined') localStorage.removeItem(key)")
}
