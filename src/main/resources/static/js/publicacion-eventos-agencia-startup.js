(function() {
    'use strict';

    function shouldBootstrapModalByServerFlag() {
        const flag = document.getElementById('flagMostrarModalPublicacionEventosAgencia');
        return flag && flag.value === 'true';
    }

    function openModalIfNeeded() {
        if (!shouldBootstrapModalByServerFlag()) {
            return;
        }

        fetch('/api/agencia/publicacion-eventos/check')
            .then(response => response.json())
            .then(data => {
                if (data.shouldShow) {
                    showModal(data.agencias || []);
                }
            })
            .catch(error => console.error('Error verificando modal de publicación de eventos:', error));
    }

    function showModal(agencias) {
        const modalElement = document.getElementById('publicacionEventosAgenciaModal');
        if (!modalElement) {
            return;
        }

        renderAgencias(agencias);

        const modal = new bootstrap.Modal(modalElement);
        document.getElementById('btnActivarPublicacionEventosAgencia').onclick = function() {
            submitDecision('/api/agencia/publicacion-eventos/activar', modal);
        };
        document.getElementById('btnRechazarPublicacionEventosAgencia').onclick = function() {
            submitDecision('/api/agencia/publicacion-eventos/rechazar', modal);
        };

        modal.show();
    }

    function renderAgencias(agencias) {
        const list = document.getElementById('listaAgenciasPublicacionEventos');
        if (!list) {
            return;
        }
        list.innerHTML = '';

        agencias.forEach(agencia => {
            const li = document.createElement('li');
            li.textContent = agencia.nombreAgencia;
            list.appendChild(li);
        });
    }

    function submitDecision(url, modal) {
        fetch(url, { method: 'POST' })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    modal.hide();
                }
            })
            .catch(error => console.error('Error guardando decisión de publicación de eventos:', error));
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', openModalIfNeeded);
    } else {
        openModalIfNeeded();
    }
})();
