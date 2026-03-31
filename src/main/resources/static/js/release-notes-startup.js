/**
 * Script para mostrar automáticamente las release notes al iniciar sesión
 * Solo se muestra si el usuario no las ha leído y está en resolución desktop
 */

(function() {
    'use strict';

    // Verificar si estamos en resolución mayor que móvil (> 768px)
    function isDesktopResolution() {
        return window.innerWidth > 768;
    }

    // Verificar si las release notes deben mostrarse
    function checkAndShowReleaseNotes() {
        // Solo ejecutar en resolución desktop
        if (!isDesktopResolution()) {
            console.log('Release notes: Resolución móvil detectada, no se mostrarán las release notes');
            return;
        }

        fetch('/release-notes/api/check')
            .then(response => response.json())
            .then(data => {
                if (data.shouldShow) {
                    showReleaseNotesModal(data.version, data.content);
                }
            })
            .catch(error => {
                console.error('Error verificando release notes:', error);
            });
    }

    // Mostrar el modal con las release notes
    function showReleaseNotesModal(version, content) {
        const modal = document.getElementById('releaseNotesModal');
        if (!modal) {
            console.error('Modal de release notes no encontrado');
            return;
        }

        // Establecer versión y contenido
        document.getElementById('releaseNotesVersion').textContent = version;
        document.getElementById('releaseNotesContent').innerHTML = content;

        // Inicializar el modal de Bootstrap
        const bsModal = new bootstrap.Modal(modal);
        
        // Configurar el botón "Entendido"
        const btnMarkAsRead = document.getElementById('btnMarkAsRead');
        btnMarkAsRead.onclick = function() {
            markAsRead(version, bsModal);
        };

        // Mostrar el modal
        bsModal.show();
    }

    // Marcar las release notes como leídas
    function markAsRead(version, modalInstance) {
        const btnMarkAsRead = document.getElementById('btnMarkAsRead');
        const originalText = btnMarkAsRead.innerHTML;
        
        // Deshabilitar botón y mostrar spinner
        btnMarkAsRead.disabled = true;
        btnMarkAsRead.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Guardando...';

        fetch('/release-notes/api/mark-read?version=' + encodeURIComponent(version), {
            method: 'POST'
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                // Cerrar el modal
                modalInstance.hide();
            } else {
                console.error('Error marcando release notes como leídas:', data.message);
                alert('Error al guardar. Por favor, inténtalo de nuevo.');
                btnMarkAsRead.disabled = false;
                btnMarkAsRead.innerHTML = originalText;
            }
        })
        .catch(error => {
            console.error('Error en la petición:', error);
            alert('Error al guardar. Por favor, inténtalo de nuevo.');
            btnMarkAsRead.disabled = false;
            btnMarkAsRead.innerHTML = originalText;
        });
    }

    // Ejecutar cuando el DOM esté listo
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', checkAndShowReleaseNotes);
    } else {
        checkAndShowReleaseNotes();
    }
})();
