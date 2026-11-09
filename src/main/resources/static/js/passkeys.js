/*
 * Passkeys (WebAuthn): login and registration with fingerprint, Face ID or Windows Hello.
 * Talks to the Spring Security WebAuthn endpoints; only public keys ever reach the server.
 * Exposes window.FestiaPasskeys. Errors carry a `code`: cancelled | rejected | label-required | unavailable.
 */
(function (window, document) {
    'use strict';

    const script = document.currentScript;
    const contextPath = ((script && script.dataset && script.dataset.contextPath) || '/').replace(/\/+$/, '');

    function base64urlEncode(buffer) {
        const binary = String.fromCharCode(...new Uint8Array(buffer));
        return window.btoa(binary).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
    }

    function base64urlDecode(value) {
        const binary = window.atob(value.replace(/-/g, '+').replace(/_/g, '/'));
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) {
            bytes[i] = binary.charCodeAt(i);
        }
        return bytes.buffer;
    }

    function failure(code, message, cause) {
        const error = new Error(message);
        error.code = code;
        error.cause = cause;
        return error;
    }

    function meta(name) {
        const element = document.querySelector('meta[name="' + name + '"]');
        return element ? element.getAttribute('content') : null;
    }

    async function post(path, body) {
        const headers = {'Content-Type': 'application/json'};
        const csrfHeader = meta('_csrf_header');
        const csrfToken = meta('_csrf');
        if (csrfHeader && csrfToken) {
            headers[csrfHeader] = csrfToken;
        }
        const options = {method: 'POST', headers, credentials: 'same-origin'};
        if (body !== undefined) {
            options.body = JSON.stringify(body);
        }
        return fetch(contextPath + path, options);
    }

    async function postJson(path, body) {
        const response = await post(path, body);
        if (!response.ok) {
            throw failure('rejected', 'HTTP ' + response.status + ' on ' + path);
        }
        return response.json();
    }

    let currentCeremony = null;

    function newSignal() {
        if (currentCeremony) {
            currentCeremony.abort();
        }
        currentCeremony = new window.AbortController();
        return currentCeremony.signal;
    }

    function deviceFailure(err) {
        if (err && (err.name === 'NotAllowedError' || err.name === 'AbortError')) {
            return failure('cancelled', 'The user cancelled or the request timed out', err);
        }
        if (err && err.name === 'InvalidStateError') {
            return failure('already-registered', 'This device already has a passkey for this account', err);
        }
        return failure('unavailable', (err && err.message) || 'Passkeys are not available', err);
    }

    function isSupported() {
        return !!window.PublicKeyCredential;
    }

    async function isConditionalAvailable() {
        const pkc = window.PublicKeyCredential;
        return !!(pkc && typeof pkc.isConditionalMediationAvailable === 'function'
            && await pkc.isConditionalMediationAvailable());
    }

    async function isPlatformAuthenticatorAvailable() {
        const pkc = window.PublicKeyCredential;
        return !!(pkc && typeof pkc.isUserVerifyingPlatformAuthenticatorAvailable === 'function'
            && await pkc.isUserVerifyingPlatformAuthenticatorAvailable());
    }

    /** Signs in with a passkey and returns the URL to go to. */
    async function authenticate(settings) {
        const conditional = !!(settings && settings.conditional);
        const options = await postJson('/webauthn/authenticate/options');
        const publicKey = Object.assign({}, options, {
            challenge: base64urlDecode(options.challenge),
            allowCredentials: (options.allowCredentials || []).map((cred) =>
                Object.assign({}, cred, {id: base64urlDecode(cred.id)})),
        });
        const request = {publicKey, signal: newSignal()};
        if (conditional) {
            request.mediation = 'conditional';
        }

        let credential;
        try {
            credential = await navigator.credentials.get(request);
        } catch (err) {
            throw deviceFailure(err);
        }

        const response = credential.response;
        const result = await postJson('/login/webauthn', {
            id: credential.id,
            rawId: base64urlEncode(credential.rawId),
            response: {
                authenticatorData: base64urlEncode(response.authenticatorData),
                clientDataJSON: base64urlEncode(response.clientDataJSON),
                signature: base64urlEncode(response.signature),
                userHandle: response.userHandle ? base64urlEncode(response.userHandle) : undefined,
            },
            credType: credential.type,
            clientExtensionResults: credential.getClientExtensionResults(),
            authenticatorAttachment: credential.authenticatorAttachment,
        });
        if (!(result && result.authenticated && result.redirectUrl)) {
            throw failure('rejected', 'Unexpected login response');
        }
        return result.redirectUrl;
    }

    /** Creates a passkey on this device for the signed-in user. */
    async function register(label) {
        const name = (label || '').trim();
        if (!name) {
            throw failure('label-required', 'A device name is required');
        }
        const options = await postJson('/webauthn/register/options');
        const publicKey = Object.assign({}, options, {
            challenge: base64urlDecode(options.challenge),
            user: Object.assign({}, options.user, {id: base64urlDecode(options.user.id)}),
            excludeCredentials: (options.excludeCredentials || []).map((cred) =>
                Object.assign({}, cred, {id: base64urlDecode(cred.id)})),
        });

        let credential;
        try {
            credential = await navigator.credentials.create({publicKey, signal: newSignal()});
        } catch (err) {
            throw deviceFailure(err);
        }

        const response = credential.response;
        const result = await postJson('/webauthn/register', {
            publicKey: {
                credential: {
                    id: credential.id,
                    rawId: base64urlEncode(credential.rawId),
                    response: {
                        attestationObject: base64urlEncode(response.attestationObject),
                        clientDataJSON: base64urlEncode(response.clientDataJSON),
                        transports: response.getTransports ? response.getTransports() : [],
                    },
                    type: credential.type,
                    clientExtensionResults: credential.getClientExtensionResults(),
                    authenticatorAttachment: credential.authenticatorAttachment,
                },
                label: name,
            },
        });
        if (!(result && result.success)) {
            throw failure('rejected', 'The server did not save the passkey');
        }
    }

    window.FestiaPasskeys = {
        isSupported,
        isConditionalAvailable,
        isPlatformAuthenticatorAvailable,
        authenticate,
        register,
    };
})(window, document);
