const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const SOURCE = fs.readFileSync(
    path.join(__dirname, '../../main/resources/static/js/passkey-offer.js'), 'utf8');

function element(extra = {}) {
    const listeners = {};
    return Object.assign({
        hidden: true,
        disabled: false,
        textContent: '',
        value: '',
        dataset: {},
        addEventListener(type, handler) {
            listeners[type] = handler;
        },
        async fire(type) {
            await listeners[type]({preventDefault() {}});
        },
    }, extra);
}

function storage(initial = {}) {
    const data = Object.assign({}, initial);
    return {
        data,
        getItem: (k) => (k in data ? data[k] : null),
        setItem: (k, v) => {
            data[k] = String(v);
        },
    };
}

async function load({passkeys, localStorage = storage()} = {}) {
    const page = {
        'passkey-offer': element({dataset: {username: 'jperez'}}),
        'passkey-offer-label': element(),
        'passkey-offer-accept': element(),
        'passkey-offer-dismiss': element(),
        'passkey-offer-message': element(),
    };
    const document = {
        readyState: 'complete',
        getElementById: (id) => page[id] || null,
        addEventListener() {},
    };
    const window = {FestiaPasskeys: passkeys, localStorage};
    const context = {window, document, console: {error() {}}, Promise, Error};
    vm.createContext(context);
    vm.runInContext(SOURCE, context);
    await new Promise((resolve) => setImmediate(resolve));
    return {page, localStorage};
}

function passkeys(overrides = {}) {
    const registered = [];
    return Object.assign({
        registered,
        isSupported: () => true,
        isMobileDevice: () => true,
        isPlatformAuthenticatorAvailable: async () => true,
        suggestDeviceName: () => 'iPhone',
        register: async (label) => {
            registered.push(label);
        },
    }, overrides);
}

const KEY = 'festia.passkeyOffer.jperez';

test('dispositivo con huella o Face ID: muestra la oferta con el nombre del dispositivo sugerido', async () => {
    const {page} = await load({passkeys: passkeys()});

    assert.equal(page['passkey-offer'].hidden, false);
    assert.equal(page['passkey-offer-label'].value, 'iPhone');
});

test('sin autenticador biometrico en el dispositivo no se ofrece', async () => {
    const {page} = await load({passkeys: passkeys({isPlatformAuthenticatorAvailable: async () => false})});

    assert.equal(page['passkey-offer'].hidden, true);
});

test('sin soporte de passkeys no se ofrece', async () => {
    const {page} = await load({passkeys: passkeys({isSupported: () => false})});

    assert.equal(page['passkey-offer'].hidden, true);
});

test('si en este dispositivo ya se dijo "Ahora no" no se vuelve a ofrecer', async () => {
    const {page} = await load({passkeys: passkeys(), localStorage: storage({[KEY]: 'dismissed'})});

    assert.equal(page['passkey-offer'].hidden, true);
});

test('"Ahora no" oculta la oferta y lo recuerda en este dispositivo', async () => {
    const api = passkeys();
    const {page, localStorage} = await load({passkeys: api});

    await page['passkey-offer-dismiss'].fire('click');

    assert.equal(page['passkey-offer'].hidden, true);
    assert.equal(localStorage.data[KEY], 'dismissed');
    assert.equal(api.registered.length, 0);
});

test('"Activar" crea la passkey con el nombre elegido y deja de ofrecerla', async () => {
    const api = passkeys();
    const {page, localStorage} = await load({passkeys: api});
    page['passkey-offer-label'].value = 'iPhone de Juan';

    await page['passkey-offer-accept'].fire('click');

    assert.deepEqual(api.registered, ['iPhone de Juan']);
    assert.equal(localStorage.data[KEY], 'created');
    assert.match(page['passkey-offer-message'].textContent, /Listo/);
    assert.equal(page['passkey-offer-accept'].hidden, true);
});

test('si el usuario cancela en el dispositivo la oferta sigue disponible sin error', async () => {
    const api = passkeys({
        register: async () => {
            throw Object.assign(new Error('cancel'), {code: 'cancelled'});
        },
    });
    const {page, localStorage} = await load({passkeys: api});

    await page['passkey-offer-accept'].fire('click');

    assert.equal(page['passkey-offer'].hidden, false);
    assert.equal(page['passkey-offer-message'].textContent, '');
    assert.equal(page['passkey-offer-accept'].disabled, false);
    assert.equal(localStorage.data[KEY], undefined);
});

test('si este dispositivo ya tenia passkey lo indica y deja de ofrecerla', async () => {
    const api = passkeys({
        register: async () => {
            throw Object.assign(new Error('dup'), {code: 'already-registered'});
        },
    });
    const {page, localStorage} = await load({passkeys: api});

    await page['passkey-offer-accept'].fire('click');

    assert.match(page['passkey-offer-message'].textContent, /ya puede entrar/);
    assert.equal(localStorage.data[KEY], 'created');
});

test('error del servidor muestra un mensaje y permite reintentar', async () => {
    const api = passkeys({
        register: async () => {
            throw Object.assign(new Error('500'), {code: 'rejected'});
        },
    });
    const {page} = await load({passkeys: api});

    await page['passkey-offer-accept'].fire('click');

    assert.match(page['passkey-offer-message'].textContent, /No hemos podido/);
    assert.equal(page['passkey-offer-accept'].disabled, false);
});

test('en escritorio no se ofrece la passkey tras el login', async () => {
    const {page} = await load({passkeys: passkeys({isMobileDevice: () => false})});

    assert.equal(page['passkey-offer'].hidden, true);
});
