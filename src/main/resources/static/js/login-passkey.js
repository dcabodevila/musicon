/*
 * Login page: sign in with a passkey (button) and offer saved passkeys in the username field (autofill).
 * Requires passkeys.js. The password form keeps working on its own.
 */
(function (window, document) {
    'use strict';

    const MESSAGES = {
        rejected: 'No hemos podido verificar la passkey. Inténtalo de nuevo o entra con tu contraseña.',
        unavailable: 'Este dispositivo no permite entrar con huella o Face ID. Entra con tu contraseña.',
    };

    function init() {
        const passkeys = window.FestiaPasskeys;
        const button = document.getElementById('passkey-login');
        const error = document.getElementById('passkey-login-error');
        if (!passkeys || !button || !passkeys.isSupported() || !passkeys.isMobileDevice()) {
            return;
        }

        function showError(err) {
            if (!error || !err || err.code === 'cancelled') {
                return;
            }
            error.textContent = MESSAGES[err.code] || MESSAGES.rejected;
            error.hidden = false;
        }

        async function signIn(conditional) {
            const redirectUrl = await passkeys.authenticate({conditional});
            window.location.href = redirectUrl;
        }

        button.hidden = false;
        button.addEventListener('click', async (event) => {
            event.preventDefault();
            if (error) {
                error.hidden = true;
            }
            button.disabled = true;
            try {
                await signIn(false);
            } catch (err) {
                console.error(err);
                showError(err);
                button.disabled = false;
            }
        });

        passkeys.isConditionalAvailable()
            .then((available) => available ? signIn(true) : null)
            .catch((err) => {
                // Autofill was replaced by the button or the user dismissed it: nothing to report.
                if (err && err.code !== 'cancelled') {
                    console.error(err);
                    showError(err);
                }
            });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})(window, document);
