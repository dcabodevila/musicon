const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const SOURCE = fs.readFileSync(
    path.join(__dirname, '../../main/resources/static/js/login-passkey.js'), 'utf8');

function element() {
    const listeners = {};
    return {
        hidden: true,
        disabled: false,
        textContent: '',
        addEventListener(type, handler) {
            listeners[type] = handler;
        },
        async fire(type) {
            await listeners[type]({preventDefault() {}});
        },
    };
}

/** Runs login-passkey.js against a fake page and a scripted FestiaPasskeys. */
async function load(passkeys) {
    const button = element();
    const error = element();
    const location = {href: '/auth/login'};
    const document = {
        readyState: 'complete',
        getElementById: (id) => ({'passkey-login': button, 'passkey-login-error': error}[id] || null),
        addEventListener() {},
    };
    const window = {FestiaPasskeys: passkeys, location};
    const context = {window, document, console: {error() {}}, Promise, Error};
    vm.createContext(context);
    vm.runInContext(SOURCE, context);
    await new Promise((resolve) => setImmediate(resolve));
    return {button, error, location};
}

function passkeys(overrides = {}) {
    const calls = [];
    return Object.assign({
        calls,
        isSupported: () => true,
        isConditionalAvailable: async () => false,
        authenticate: async (settings) => {
            calls.push(settings || {});
            return '/';
        },
    }, overrides);
}

test('sin soporte de passkeys el boton sigue oculto y no se inicia nada', async () => {
    const api = passkeys({isSupported: () => false});

    const {button} = await load(api);

    assert.equal(button.hidden, true);
    assert.equal(api.calls.length, 0);
});

test('con soporte se muestra el boton y al pulsarlo entra y redirige', async () => {
    const api = passkeys();
    const {button, location} = await load(api);

    assert.equal(button.hidden, false);
    await button.fire('click');

    assert.equal(api.calls.length, 1);
    assert.notEqual(api.calls[0].conditional, true);
    assert.equal(location.href, '/');
});

test('con autorrelleno disponible se ofrece la passkey en el campo de usuario', async () => {
    const api = passkeys({isConditionalAvailable: async () => true});

    await load(api);

    assert.equal(api.calls.length, 1);
    assert.equal(api.calls[0].conditional, true);
});

test('passkey rechazada muestra un mensaje y deja usar la contrasena', async () => {
    const api = passkeys({
        authenticate: async () => {
            throw Object.assign(new Error('401'), {code: 'rejected'});
        },
    });
    const {button, error, location} = await load(api);

    await button.fire('click');

    assert.equal(error.hidden, false);
    assert.match(error.textContent, /No hemos podido verificar/);
    assert.equal(location.href, '/auth/login');
    assert.equal(button.disabled, false);
});

test('si el usuario cancela no se muestra ningun error', async () => {
    const api = passkeys({
        authenticate: async () => {
            throw Object.assign(new Error('cancel'), {code: 'cancelled'});
        },
    });
    const {button, error} = await load(api);

    await button.fire('click');

    assert.equal(error.hidden, true);
});
