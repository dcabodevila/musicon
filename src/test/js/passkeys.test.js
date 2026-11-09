const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const SOURCE = fs.readFileSync(
    path.join(__dirname, '../../main/resources/static/js/passkeys.js'), 'utf8');

const bytes = (...values) => new Uint8Array(values).buffer;

function base64url(buffer) {
    return Buffer.from(buffer).toString('base64').replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
}

function jsonResponse(status, body) {
    return {ok: status >= 200 && status < 300, status, json: async () => body};
}

/** Loads passkeys.js in a fake browser with scripted fetch responses. */
function load({responses = [], credentials = {}, publicKeyCredential = {isConditionalMediationAvailable: async () => true}, contextPath = '/'} = {}) {
    const requests = [];
    const metas = {_csrf: 'token-123', _csrf_header: 'X-CSRF-TOKEN'};
    const window = {
        PublicKeyCredential: publicKeyCredential,
        btoa: (s) => Buffer.from(s, 'binary').toString('base64'),
        atob: (s) => Buffer.from(s, 'base64').toString('binary'),
        AbortController,
    };
    const document = {
        currentScript: {dataset: {contextPath}},
        querySelector: (selector) => {
            const name = /name="([^"]+)"/.exec(selector)[1];
            return metas[name] ? {getAttribute: () => metas[name], content: metas[name]} : null;
        },
    };
    const fetch = async (url, options) => {
        requests.push({url, options, body: options.body ? JSON.parse(options.body) : undefined});
        const next = responses.shift();
        if (!next) throw new Error('Unexpected request ' + url);
        return next;
    };
    const context = {window, document, fetch, navigator: {credentials}, Uint8Array, ArrayBuffer, String, JSON, Error, Promise, AbortController, console};
    window.window = window;
    vm.createContext(context);
    vm.runInContext(SOURCE, context);
    return {api: window.FestiaPasskeys, requests};
}

function assertion() {
    return {
        id: 'cred-1',
        rawId: bytes(1, 2, 3),
        type: 'public-key',
        authenticatorAttachment: 'platform',
        response: {
            authenticatorData: bytes(4),
            clientDataJSON: bytes(5),
            signature: bytes(6),
            userHandle: bytes(7, 8),
        },
        getClientExtensionResults: () => ({}),
    };
}

test('authenticate: pide el reto con CSRF, firma con el dispositivo y devuelve la URL de destino', async () => {
    let getOptions;
    const {api, requests} = load({
        responses: [
            jsonResponse(200, {challenge: base64url(bytes(9, 9)), rpId: 'festia.es', allowCredentials: []}),
            jsonResponse(200, {authenticated: true, redirectUrl: '/'}),
        ],
        credentials: {get: async (options) => { getOptions = options; return assertion(); }},
    });

    const redirectUrl = await api.authenticate();

    assert.equal(redirectUrl, '/');
    assert.equal(requests[0].url, '/webauthn/authenticate/options');
    assert.equal(requests[0].options.method, 'POST');
    assert.equal(requests[0].options.headers['X-CSRF-TOKEN'], 'token-123');
    assert.deepEqual([...new Uint8Array(getOptions.publicKey.challenge)], [9, 9]);
    assert.equal(getOptions.publicKey.rpId, 'festia.es');
    assert.equal(getOptions.mediation, undefined);
    assert.equal(requests[1].url, '/login/webauthn');
    assert.equal(requests[1].options.headers['X-CSRF-TOKEN'], 'token-123');
    assert.deepEqual(requests[1].body, {
        id: 'cred-1',
        rawId: base64url(bytes(1, 2, 3)),
        response: {
            authenticatorData: base64url(bytes(4)),
            clientDataJSON: base64url(bytes(5)),
            signature: base64url(bytes(6)),
            userHandle: base64url(bytes(7, 8)),
        },
        credType: 'public-key',
        clientExtensionResults: {},
        authenticatorAttachment: 'platform',
    });
});

test('authenticate condicional: usa mediation conditional para el autorrelleno', async () => {
    let getOptions;
    const {api} = load({
        responses: [
            jsonResponse(200, {challenge: base64url(bytes(1))}),
            jsonResponse(200, {authenticated: true, redirectUrl: '/'}),
        ],
        credentials: {get: async (options) => { getOptions = options; return assertion(); }},
    });

    await api.authenticate({conditional: true});

    assert.equal(getOptions.mediation, 'conditional');
});

test('authenticate: respeta el context path de la aplicacion', async () => {
    const {api, requests} = load({
        contextPath: '/gestmusica/',
        responses: [
            jsonResponse(200, {challenge: base64url(bytes(1))}),
            jsonResponse(200, {authenticated: true, redirectUrl: '/gestmusica/'}),
        ],
        credentials: {get: async () => assertion()},
    });

    await api.authenticate();

    assert.equal(requests[0].url, '/gestmusica/webauthn/authenticate/options');
    assert.equal(requests[1].url, '/gestmusica/login/webauthn');
});

test('authenticate: passkey rechazada por el servidor lanza error "rejected"', async () => {
    const {api} = load({
        responses: [jsonResponse(200, {challenge: base64url(bytes(1))}), jsonResponse(401, {})],
        credentials: {get: async () => assertion()},
    });

    await assert.rejects(api.authenticate(), (err) => err.code === 'rejected');
});

test('authenticate: si el usuario cancela lanza error "cancelled" sin llamar al login', async () => {
    const cancelled = Object.assign(new Error('The operation either timed out or was not allowed.'), {name: 'NotAllowedError'});
    const {api, requests} = load({
        responses: [jsonResponse(200, {challenge: base64url(bytes(1))})],
        credentials: {get: async () => { throw cancelled; }},
    });

    await assert.rejects(api.authenticate(), (err) => err.code === 'cancelled');
    assert.equal(requests.length, 1);
});

test('register: crea la passkey con la etiqueta y la envia para guardarla', async () => {
    let createOptions;
    const {api, requests} = load({
        responses: [
            jsonResponse(200, {
                challenge: base64url(bytes(2)),
                user: {id: base64url(bytes(3)), name: 'jperez', displayName: 'Juan Perez'},
                excludeCredentials: [{id: base64url(bytes(4)), type: 'public-key'}],
            }),
            jsonResponse(200, {success: true}),
        ],
        credentials: {
            create: async (options) => {
                createOptions = options;
                return {
                    id: 'new-cred', rawId: bytes(5), type: 'public-key', authenticatorAttachment: 'platform',
                    response: {attestationObject: bytes(6), clientDataJSON: bytes(7), getTransports: () => ['internal', 'hybrid']},
                    getClientExtensionResults: () => ({credProps: {rk: true}}),
                };
            },
        },
    });

    await api.register('iPhone de Juan');

    assert.equal(requests[0].url, '/webauthn/register/options');
    assert.equal(requests[0].options.headers['X-CSRF-TOKEN'], 'token-123');
    assert.deepEqual([...new Uint8Array(createOptions.publicKey.user.id)], [3]);
    assert.deepEqual([...new Uint8Array(createOptions.publicKey.excludeCredentials[0].id)], [4]);
    assert.equal(requests[1].url, '/webauthn/register');
    assert.deepEqual(requests[1].body, {
        publicKey: {
            credential: {
                id: 'new-cred',
                rawId: base64url(bytes(5)),
                response: {attestationObject: base64url(bytes(6)), clientDataJSON: base64url(bytes(7)), transports: ['internal', 'hybrid']},
                type: 'public-key',
                clientExtensionResults: {credProps: {rk: true}},
                authenticatorAttachment: 'platform',
            },
            label: 'iPhone de Juan',
        },
    });
});

test('register: sin etiqueta no llama al servidor', async () => {
    const {api, requests} = load();

    await assert.rejects(api.register('  '), (err) => err.code === 'label-required');
    assert.equal(requests.length, 0);
});

test('register: respuesta sin success lanza error "rejected"', async () => {
    const {api} = load({
        responses: [
            jsonResponse(200, {challenge: base64url(bytes(2)), user: {id: base64url(bytes(3))}}),
            jsonResponse(200, {success: false}),
        ],
        credentials: {create: async () => ({id: 'x', rawId: bytes(1), type: 'public-key', response: {attestationObject: bytes(1), clientDataJSON: bytes(1)}, getClientExtensionResults: () => ({})})},
    });

    await assert.rejects(api.register('Portatil'), (err) => err.code === 'rejected');
});

test('isSupported: false si el navegador no tiene WebAuthn', () => {
    const {api} = load({publicKeyCredential: null});

    assert.equal(api.isSupported(), false);
});

test('isConditionalAvailable: true solo si el navegador ofrece autorrelleno de passkeys', async () => {
    assert.equal(await load().api.isConditionalAvailable(), true);
    assert.equal(await load({publicKeyCredential: {}}).api.isConditionalAvailable(), false);
});
