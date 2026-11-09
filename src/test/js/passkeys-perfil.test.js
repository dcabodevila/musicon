const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const SOURCE = fs.readFileSync(
    path.join(__dirname, '../../main/resources/static/js/passkeys-perfil.js'), 'utf8');

/** Minimal DOM node: enough for the profile card to render and react to clicks. */
function node(tag = 'div', extra = {}) {
    const listeners = {};
    return Object.assign({
        tagName: tag.toUpperCase(),
        hidden: false,
        disabled: false,
        value: '',
        className: '',
        dataset: {},
        children: [],
        _text: '',
        get textContent() {
            return this._text + this.children.map((c) => c.textContent).join(' ');
        },
        set textContent(value) {
            this._text = value;
            this.children = [];
        },
        appendChild(child) {
            this.children.push(child);
            return child;
        },
        setAttribute(name, value) {
            this[name] = value;
        },
        addEventListener(type, handler) {
            listeners[type] = handler;
        },
        async fire(type) {
            await listeners[type]({preventDefault() {}});
        },
        buttons() {
            const found = [];
            const walk = (n) => {
                if (n.tagName === 'BUTTON') found.push(n);
                n.children.forEach(walk);
            };
            this.children.forEach(walk);
            return found;
        },
    }, extra);
}

async function settle() {
    for (let i = 0; i < 5; i++) {
        await new Promise((resolve) => setImmediate(resolve));
    }
}

async function load({passkeys, confirm = () => true}) {
    const page = {
        'passkeys-perfil': node('section'),
        'passkeys-perfil-list': node('ul'),
        'passkeys-perfil-empty': node('p', {hidden: true}),
        'passkeys-perfil-label': node('input'),
        'passkeys-perfil-add': node('button', {hidden: true}),
        'passkeys-perfil-message': node('p'),
    };
    const document = {
        readyState: 'complete',
        getElementById: (id) => page[id] || null,
        createElement: (tag) => node(tag),
        addEventListener() {},
    };
    const window = {FestiaPasskeys: passkeys, confirm};
    const context = {window, document, console: {error() {}}, Promise, Error, Date};
    vm.createContext(context);
    vm.runInContext(SOURCE, context);
    await settle();
    return page;
}

function passkeys(lista, overrides = {}) {
    const calls = {registered: [], removed: []};
    return Object.assign({
        calls,
        isSupported: () => true,
        suggestDeviceName: () => 'iPhone',
        list: async () => lista.slice(),
        register: async (label) => {
            calls.registered.push(label);
            lista.push({id: 'nueva', etiqueta: label, fechaCreacion: '2026-11-10T09:00:00Z', fechaUltimoUso: null});
        },
        remove: async (id) => {
            calls.removed.push(id);
            lista.splice(lista.findIndex((p) => p.id === id), 1);
        },
    }, overrides);
}

const IPHONE = {id: 'cred-1', etiqueta: 'iPhone de Juan', fechaCreacion: '2026-11-09T10:00:00Z', fechaUltimoUso: '2026-11-10T08:00:00Z'};

test('lista las passkeys con su nombre y fechas', async () => {
    const page = await load({passkeys: passkeys([IPHONE])});

    const list = page['passkeys-perfil-list'];
    assert.equal(list.children.length, 1);
    assert.match(list.textContent, /iPhone de Juan/);
    assert.match(list.textContent, /09\/11\/2026/);
    assert.match(list.textContent, /10\/11\/2026/);
    assert.equal(page['passkeys-perfil-empty'].hidden, true);
});

test('sin passkeys muestra el mensaje de lista vacia', async () => {
    const page = await load({passkeys: passkeys([])});

    assert.equal(page['passkeys-perfil-list'].children.length, 0);
    assert.equal(page['passkeys-perfil-empty'].hidden, false);
});

test('en un dispositivo compatible permite anadirlo con un nombre sugerido', async () => {
    const page = await load({passkeys: passkeys([])});

    assert.equal(page['passkeys-perfil-add'].hidden, false);
    assert.equal(page['passkeys-perfil-label'].value, 'iPhone');
});

test('en un dispositivo sin passkeys solo se puede ver y eliminar', async () => {
    const page = await load({passkeys: passkeys([IPHONE], {isSupported: () => false})});

    assert.equal(page['passkeys-perfil-add'].hidden, true);
    assert.equal(page['passkeys-perfil-list'].children.length, 1);
});

test('anadir este dispositivo registra la passkey y refresca la lista', async () => {
    const api = passkeys([]);
    const page = await load({passkeys: api});
    page['passkeys-perfil-label'].value = 'Portatil de Juan';

    await page['passkeys-perfil-add'].fire('click');
    await settle();

    assert.deepEqual(api.calls.registered, ['Portatil de Juan']);
    assert.match(page['passkeys-perfil-list'].textContent, /Portatil de Juan/);
    assert.match(page['passkeys-perfil-message'].textContent, /añadido/);
});

test('eliminar pide confirmacion, borra la passkey y refresca la lista', async () => {
    const api = passkeys([IPHONE]);
    const page = await load({passkeys: api});

    await page['passkeys-perfil-list'].buttons()[0].fire('click');
    await settle();

    assert.deepEqual(api.calls.removed, ['cred-1']);
    assert.equal(page['passkeys-perfil-list'].children.length, 0);
    assert.equal(page['passkeys-perfil-empty'].hidden, false);
});

test('si no se confirma no se elimina nada', async () => {
    const api = passkeys([IPHONE]);
    const page = await load({passkeys: api, confirm: () => false});

    await page['passkeys-perfil-list'].buttons()[0].fire('click');
    await settle();

    assert.deepEqual(api.calls.removed, []);
    assert.equal(page['passkeys-perfil-list'].children.length, 1);
});

test('si falla la carga muestra un mensaje de error', async () => {
    const page = await load({passkeys: passkeys([], {list: async () => { throw Object.assign(new Error('500'), {code: 'rejected'}); }})});

    assert.match(page['passkeys-perfil-message'].textContent, /No hemos podido cargar/);
});
