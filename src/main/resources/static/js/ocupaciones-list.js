$(document).ready(function(){


    $('#datatables-reponsive_ocupaciones').DataTable( {
        responsive: true,
        searching: true,
        ordering:  true,
        paging: true,
        language: {
            url: "https://cdn.datatables.net/plug-ins/1.13.1/i18n/es-ES.json"
        },
            columnDefs: [
                {
                    targets: 1,
                    type: 'date-eu'
                },
                {
                    targets: 2,
                    type: 'date-eu'
                }
            ],
            order: [
              [1, 'desc']  // Luego ordena por la tercera columna (índice 2)
            ]


    } );

        let pickerFechaDesde = flatpickr("#idFechaDesde", {
            disableMobile: true,
            "locale": "es", 
            altInput: true,
            altFormat: "j F, Y",
            dateFormat: "d-m-Y",
            allowInput: false,
            onChange: function(selectedDates, dateStr, instance) {
                document.getElementById("idFechaDesde").required = true; // Refuerza el atributo required.
            }

        });
        let pickerFechaHasta = flatpickr("#idFechaHasta", {
            disableMobile: true,
            "locale": "es",
            altInput: true,
            altFormat: "j F, Y",
            dateFormat: "d-m-Y",
            allowInput: false
        });

        document.getElementById("formListadoOcupaciones").addEventListener("submit", function(event) {
            const fechaDesde = document.getElementById("idFechaDesde").value;
            if (document.getElementById('idFechaDesde').value ==''){
                notif('error','Selecciona la fecha ocupación desde');
                event.preventDefault();
            }
        });

       $("#agencia").on("change", function () {
            const idAgencia = $(this).val(); // Obtener el valor seleccionado

            // Limpiar y reiniciar las opciones del select de artistas
            const $selectArtista = $("#artista");
            $selectArtista.empty();
            $selectArtista.append('<option value="">Todos los artistas de la agencia</option>');

            // Verificar si se seleccionó una agencia válida
            if (idAgencia) {
                // Realizar la solicitud AJAX para obtener los artistas
                $.ajax({
                    url: `/artista/artistas/${idAgencia}`, // Endpoint del controlador
                    type: "GET",
                    contentType: "application/json",
                    success: function (data) {
                        // Agregar las opciones obtenidas en la lista de artistas
                        $.each(data, function (index, artista) {
                            $selectArtista.append(
                                `<option value="${artista.id}">${artista.nombre}</option>`
                            );
                        });
                    },
                    error: function (xhr, status, error) {
                        console.error("Error al cargar los artistas:", error);
                        alert("Error al cargar los artistas. Intente de nuevo.");
                    }
                });
            }
        });


});

function crearNuevaOcupacion() {
    const selectedArtistId = $('#selectArtistasNuevaOcupacion').val();
    window.location.href = `/ocupacion/nueva/${selectedArtistId}`;
}

// Función para descargar ocupaciones en PDF
function descargarOcupacionesPDF() {
    // Deshabilitar el botón
    const $btn = $('#btn-descargar-pdf-ocupaciones');
    const textoOriginal = $btn.html();
    $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i> Generando...');

    const formData = $('#formListadoOcupaciones').serialize();

    $.ajax({
        type: 'POST',
        url: '/ocupacion/ocupaciones-pdf',
        data: formData,
        xhrFields: {
            responseType: 'blob'
        },
        success: function (data, status, xhr) {
            // Rehabilitar el botón
            $btn.prop('disabled', false).html(textoOriginal);

            // Verificar si la respuesta es realmente un PDF
            const contentType = xhr.getResponseHeader('Content-Type');
            if (!contentType || !contentType.includes('application/pdf')) {
                notif('error', 'Error: La respuesta del servidor no es válida');
                return;
            }

            // Obtener el nombre del archivo
            const contentDisposition = xhr.getResponseHeader('Content-Disposition');
            let filename = 'Ocupaciones_' + new Date().getTime() + '.pdf';

            if (contentDisposition) {
                const matches = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
                if (matches != null && matches[1]) {
                    filename = matches[1].replace(/['"]/g, '');
                }
            }

            // Crear Blob y URL
            const blob = new Blob([data], { type: 'application/pdf' });
            const url = window.URL.createObjectURL(blob);

            // Crear ID único para el archivo
            const fileId = 'pdf_ocupaciones_' + Date.now();
            window[fileId] = {
                blob: blob,
                url: url,
                filename: filename
            };

            // Mostrar notificación con botones
            notifDuration(
                'success',
                `<div class="d-flex gap-2">
                    <button onclick="window.open(window.${fileId}.url, '_blank')"
                    class="btn btn-sm btn-outline-light w-50">
                        <i class="fas fa-eye"></i> Ver PDF
                    </button>
                    <button id="btn-share-pdf-ocupaciones" class="btn btn-sm btn-outline-light w-50" data-file-id="${fileId}">
                        <i class="fas fa-share"></i> Compartir PDF
                    </button>
                </div>`,
                60000
            );

            // Evento para compartir (debe ser dinámico porque el botón se genera en la notificación)
            $(document).on('click', '#btn-share-pdf-ocupaciones', function () {
                const $shareBtn = $(this);
                const fileIdToShare = $shareBtn.data('file-id');
                const fileMetadata = window[fileIdToShare];

                // Prevenir múltiples solicitudes de compartir
                if ($shareBtn.prop('disabled')) {
                    return;
                }

                if (navigator.share && navigator.canShare) {
                    const file = new File([fileMetadata.blob], fileMetadata.filename, { type: 'application/pdf' });

                    // Verificar si el navegador y dispositivo soportan la funcionalidad de compartir
                    if (navigator.canShare({ files: [file] })) {
                        // Deshabilitar el botón para evitar interacciones múltiples
                        $shareBtn.prop('disabled', true);

                        navigator
                            .share({
                                files: [file],
                                title: 'Listado de Ocupaciones',
                                text: 'Aquí tienes el PDF de ocupaciones generado.',
                            })
                            .then(() => {
                                console.log('Archivo compartido exitosamente.');
                            })
                            .catch((error) => {
                                console.error('Error al compartir:', error);
                                notif('error', 'No se pudo compartir el archivo.');
                            })
                            .finally(() => {
                                // Vuelve a habilitar el botón después de completar o fallar el proceso
                                $shareBtn.prop('disabled', false);
                            });
                    } else {
                        notif('error', 'Tu dispositivo no admite la funcionalidad de compartir archivos.');
                    }
                } else {
                    notif('error', 'Compartir archivos no está disponible en este navegador.');
                }
            });

            // Limpieza del Blob después de 60 segundos
            setTimeout(() => {
                delete window[fileId];
                window.URL.revokeObjectURL(url);
            }, 60000);
        },
        error: function(xhr, status, error) {
            // Rehabilitar el botón
            $btn.prop('disabled', false).html(textoOriginal);

            console.error('Error al generar PDF:', error);

            let errorMessage = 'Error al generar el PDF de ocupaciones';

            // Mensajes específicos por código de estado
            switch (xhr.status) {
                case 400:
                    errorMessage = 'Datos del formulario inválidos';
                    break;
                case 500:
                    errorMessage = 'Error interno del servidor';
                    break;
                case 0:
                    errorMessage = 'Error de conexión con el servidor';
                    break;
            }

            notif('error', errorMessage);
        }
    });
}


