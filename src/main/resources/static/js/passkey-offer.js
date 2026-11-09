/*
 * After a password login, offers to create a passkey on this device (fingerprint or Face ID).
 * Not offered again on this device after "Ahora no" or once a passkey exists here.
 * Requires passkeys.js.
 */
(function (window, document) {
    'use strict';

    const MESSAGES = {
        created: 'Listo. La próxima vez podrás entrar con huella o Face ID en este dispositivo.',
        alreadyRegistered: 'Este dispositivo ya puede entrar con huella o Face ID.',
        failed: 'No hemos podido activar el acceso con huella o Face ID. Puedes intentarlo más tarde desde Mi perfil.',
    };

    /** Also written by passkeys-perfil.js when a passkey is added from "Mi perfil". */
    function storageKey(username) {
        return 'festia.passkeyOffer.' + username;
    }

    function remember(username, value) {
        try {
            window.localStorage.setItem(storageKey(username), value);
        } catch (err) {
            // Private mode without storage: the offer may simply appear again.
        }
    }

    function alreadyAnswered(username) {
        try {
            return window.localStorage.getItem(storageKey(username)) !== null;
        } catch (err) {
            return false;
        }
    }

    async function init() {
        const passkeys = window.FestiaPasskeys;
        const offer = document.getElementById('passkey-offer');
        const label = document.getElementById('passkey-offer-label');
        const accept = document.getElementById('passkey-offer-accept');
        const dismiss = document.getElementById('passkey-offer-dismiss');
        const message = document.getElementById('passkey-offer-message');
        if (!passkeys || !offer || !accept || !dismiss || !passkeys.isSupported()) {
            return;
        }
        const username = offer.dataset.username;
        if (!username || alreadyAnswered(username) || !(await passkeys.isPlatformAuthenticatorAvailable())) {
            return;
        }

        if (label) {
            label.value = passkeys.suggestDeviceName();
        }
        offer.hidden = false;

        let created = false;

        function finish(text) {
            created = true;
            remember(username, 'created');
            message.textContent = text;
            accept.hidden = true;
            if (label) {
                label.disabled = true;
            }
            dismiss.textContent = 'Cerrar';
        }

        dismiss.addEventListener('click', (event) => {
            event.preventDefault();
            if (!created) {
                remember(username, 'dismissed');
            }
            offer.hidden = true;
        });

        accept.addEventListener('click', async (event) => {
            event.preventDefault();
            message.textContent = '';
            accept.disabled = true;
            try {
                await passkeys.register(label && label.value ? label.value : passkeys.suggestDeviceName());
                finish(MESSAGES.created);
            } catch (err) {
                if (err && err.code === 'already-registered') {
                    finish(MESSAGES.alreadyRegistered);
                } else if (!err || err.code !== 'cancelled') {
                    console.error(err);
                    message.textContent = MESSAGES.failed;
                }
            } finally {
                accept.disabled = false;
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})(window, document);
