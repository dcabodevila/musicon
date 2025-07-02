$(document).ready(function() {
    // Manejar clic en botones de descarga
    $('.btn-descargar').on('click', function(e) {
        e.preventDefault();
        const documentoId = $(this).data('id');
        const nombreDocumento = $(this).data('nombre');
        descargarDocumento(documentoId, nombreDocumento, $(this));
    });

    function descargarDocumento(documentoId, nombreDocumento, $btn) {
        // Deshabilitar el botón y mostrar spinner
        const textoOriginal = $btn.html();
        $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i>');

        const url = `/documentos/descargar/${documentoId}`;

        $.ajax({
            type: 'GET',
            url: url,
            xhrFields: {
                responseType: 'blob'
            },
            success: function(data, status, xhr) {
                // Rehabilitar el botón
                $btn.prop('disabled', false).html(textoOriginal);

                // Verificar si la respuesta es válida
                const contentType = xhr.getResponseHeader('Content-Type');
                if (!data || data.size === 0) {
                    notif('error', 'Error: El archivo está vacío o no se pudo descargar');
                    return;
                }

                // Obtener el nombre del archivo desde el header
                let filename = nombreDocumento;
                const contentDisposition = xhr.getResponseHeader('Content-Disposition');

                if (contentDisposition) {
                    const matches = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
                    if (matches != null && matches[1]) {
                        filename = matches[1].replace(/['"]/g, '');
                    }
                }

                // Si no hay nombre, usar uno por defecto
                if (!filename || filename.trim() === '') {
                    filename = `documento_${documentoId}_${Date.now()}`;
                }

                // **CORRECCIÓN PRINCIPAL**: Crear blob con el tipo MIME correcto
                const blob = new Blob([data], { type: contentType || data.type });
                const downloadUrl = window.URL.createObjectURL(blob);

                // 1. Descarga automática
                const downloadLink = document.createElement('a');
                downloadLink.style.display = 'none';
                downloadLink.href = downloadUrl;
                downloadLink.download = filename;
                document.body.appendChild(downloadLink);
                downloadLink.click();

                // 2. Notificación de éxito con opción de abrir
                if (window.URL && window.URL.createObjectURL) {
                    // Crear un ID único para este archivo
                    const fileId = 'doc_' + Date.now();

                    // **CORRECCIÓN**: Crear una URL separada para visualización
                    const viewBlob = new Blob([data], { type: contentType || data.type });
                    const viewUrl = window.URL.createObjectURL(viewBlob);

                    // Guardar referencia temporal
                    window[fileId] = {
                        url: viewUrl,
                        filename: filename,
                        blob: viewBlob
                    };

                    // Determinar el tipo de archivo para el botón de vista
                    const extension = filename.split('.').pop().toLowerCase();
                    const esPDF = extension === 'pdf' || contentType === 'application/pdf';
                    const iconoVista = esPDF ? 'fa-eye' : 'fa-external-link-alt';
                    const textoVista = esPDF ? 'Ver documento' : 'Abrir documento';

                    notifDuration('success',
                        `Documento descargado: ${filename}<br>
                        <button onclick="abrirDocumento('${fileId}')"
                                class="btn btn-sm btn-outline-light mt-2">
                            <i class="fas ${iconoVista}"></i> ${textoVista}
                        </button>`, 10000
                    );

                    // Cleanup después de 60 segundos (aumentado el tiempo)
                    setTimeout(() => {
                        if (document.body.contains(downloadLink)) {
                            document.body.removeChild(downloadLink);
                        }
                        window.URL.revokeObjectURL(downloadUrl);
                        if (window[fileId]) {
                            window.URL.revokeObjectURL(window[fileId].url);
                            delete window[fileId];
                        }
                    }, 60000);
                } else {
                    // Fallback para navegadores antiguos
                    notif('success', `Documento descargado: ${filename}`);
                    setTimeout(() => {
                        if (document.body.contains(downloadLink)) {
                            document.body.removeChild(downloadLink);
                        }
                        window.URL.revokeObjectURL(downloadUrl);
                    }, 1000);
                }
            },
            error: function(xhr, status, error) {
                // Rehabilitar el botón
                $btn.prop('disabled', false).html(textoOriginal);

                console.error('Error al descargar documento:', error);

                let errorMessage = 'Error al descargar el documento';

                // Manejar diferentes tipos de errores
                switch (xhr.status) {
                    case 404:
                        errorMessage = 'Documento no encontrado';
                        break;
                    case 403:
                        errorMessage = 'No tienes permisos para descargar este documento';
                        break;
                    case 500:
                        errorMessage = 'Error interno del servidor';
                        break;
                    case 0:
                        errorMessage = 'Error de conexión con el servidor';
                        break;
                    default:
                        if (xhr.responseText) {
                            try {
                                const errorData = JSON.parse(xhr.responseText);
                                if (errorData.message) {
                                    errorMessage = errorData.message;
                                }
                            } catch (e) {
                                // No es JSON, usar mensaje genérico
                            }
                        }
                }

                notif('error', errorMessage);
            }
        });
    }
});

// **NUEVA FUNCIÓN**: Función global para abrir documentos
function abrirDocumento(fileId) {
    if (window[fileId] && window[fileId].url) {
        const ventana = window.open(window[fileId].url, '_blank');
        
        // Verificar si se pudo abrir la ventana
        if (!ventana) {
            notif('warning', 'Por favor, permite las ventanas emergentes para ver el documento');
        }
    } else {
        notif('error', 'El documento ya no está disponible. Intenta descargarlo nuevamente.');
    }
}