/*
 * "Mi perfil": list, add and remove the user's passkeys (fingerprint, Face ID, Windows Hello).
 * Requires passkeys.js.
 */
(function (window, document) {
    'use strict';

    const MESSAGES = {
        added: 'Dispositivo añadido. Ya puedes entrar con huella o Face ID desde él.',
        alreadyRegistered: 'Este dispositivo ya tiene acceso con huella o Face ID.',
        addFailed: 'No hemos podido añadir este dispositivo. Inténtalo de nuevo.',
        removed: 'Dispositivo eliminado.',
        removeFailed: 'No hemos podido eliminar el dispositivo. Inténtalo de nuevo.',
        loadFailed: 'No hemos podido cargar tus dispositivos. Recarga la página para intentarlo de nuevo.',
    };

    /** Same per-device flag as passkey-offer.js: a device with a passkey is not offered one again. */
    function markDeviceRegistered(username) {
        if (!username) {
            return;
        }
        try {
            window.localStorage.setItem('festia.passkeyOffer.' + username, 'created');
        } catch (err) {
            // Private mode without storage: the offer may simply appear again.
        }
    }

    function formatDate(value) {
        if (!value) {
            return null;
        }
        return new Date(value).toLocaleDateString('es-ES', {day: '2-digit', month: '2-digit', year: 'numeric'});
    }

    function init() {
        const passkeys = window.FestiaPasskeys;
        const list = document.getElementById('passkeys-perfil-list');
        const empty = document.getElementById('passkeys-perfil-empty');
        const label = document.getElementById('passkeys-perfil-label');
        const add = document.getElementById('passkeys-perfil-add');
        const message = document.getElementById('passkeys-perfil-message');
        const card = document.getElementById('passkeys-perfil');
        const username = card && card.dataset ? card.dataset.username : null;
        if (!passkeys || !list || !message) {
            return;
        }

        function say(text) {
            message.textContent = text;
        }

        function item(passkey) {
            const li = document.createElement('li');
            li.className = 'list-group-item d-flex justify-content-between align-items-center gap-2';

            const info = document.createElement('div');
            const name = document.createElement('strong');
            name.textContent = passkey.etiqueta;
            const dates = document.createElement('div');
            dates.className = 'small text-muted';
            const lastUse = formatDate(passkey.fechaUltimoUso);
            dates.textContent = 'Añadido el ' + formatDate(passkey.fechaCreacion)
                + (lastUse ? ' · Último uso el ' + lastUse : ' · Sin usar todavía');
            info.appendChild(name);
            info.appendChild(dates);

            const remove = document.createElement('button');
            remove.className = 'btn btn-outline-danger btn-sm';
            remove.setAttribute('type', 'button');
            remove.setAttribute('aria-label', 'Eliminar ' + passkey.etiqueta);
            remove.textContent = 'Eliminar';
            remove.addEventListener('click', async (event) => {
                event.preventDefault();
                if (!window.confirm('¿Eliminar el acceso con huella o Face ID de «' + passkey.etiqueta + '»?')) {
                    return;
                }
                remove.disabled = true;
                try {
                    await passkeys.remove(passkey.id);
                    say(MESSAGES.removed);
                    await refresh();
                } catch (err) {
                    console.error(err);
                    say(MESSAGES.removeFailed);
                    remove.disabled = false;
                }
            });

            li.appendChild(info);
            li.appendChild(remove);
            return li;
        }

        async function refresh() {
            let items;
            try {
                items = await passkeys.list();
            } catch (err) {
                console.error(err);
                say(MESSAGES.loadFailed);
                return;
            }
            list.textContent = '';
            items.forEach((passkey) => list.appendChild(item(passkey)));
            if (empty) {
                empty.hidden = items.length > 0;
            }
        }

        if (add && passkeys.isSupported()) {
            if (label) {
                label.value = passkeys.suggestDeviceName();
            }
            add.hidden = false;
            add.addEventListener('click', async (event) => {
                event.preventDefault();
                say('');
                add.disabled = true;
                try {
                    await passkeys.register(label && label.value ? label.value : passkeys.suggestDeviceName());
                    markDeviceRegistered(username);
                    say(MESSAGES.added);
                    await refresh();
                } catch (err) {
                    if (err && err.code === 'already-registered') {
                        markDeviceRegistered(username);
                        say(MESSAGES.alreadyRegistered);
                    } else if (!err || err.code !== 'cancelled') {
                        console.error(err);
                        say(MESSAGES.addFailed);
                    }
                } finally {
                    add.disabled = false;
                }
            });
        }

        refresh();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})(window, document);
