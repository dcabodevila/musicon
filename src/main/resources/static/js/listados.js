

$(document).ready(function(){
    let pickerFechaHasta = flatpickr("#idFechaHastaListado", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
    });
    let pickerFechaDesde = flatpickr("#idFechaDesdeListado", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            pickerFechaHasta.set("minDate", dateStr);
        },
        onMonthChange: function(selectedDates, dateStr, instance) {
            pickerFechaHasta.clear();
            pickerFechaHasta.changeMonth(instance.currentMonth);
        }
    });

    let pickerFecha7 = flatpickr("#idFecha7", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
    });
    let pickerFecha6 = flatpickr("#idFecha6", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            pickerFecha7.jumpToDate(new Date(instance.currentYear, instance.currentMonth, 1));
        }
    });
    let pickerFecha5 = flatpickr("#idFecha5", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            pickerFecha6.jumpToDate(new Date(instance.currentYear, instance.currentMonth, 1));
        }
    });
    let pickerFecha4 = flatpickr("#idFecha4", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            pickerFecha5.jumpToDate(new Date(instance.currentYear, instance.currentMonth, 1));
        }
    });
    let pickerFecha3 = flatpickr("#idFecha3", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            pickerFecha4.jumpToDate(new Date(instance.currentYear, instance.currentMonth, 1));
        }

    });
    let pickerFecha2 = flatpickr("#idFecha2", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            pickerFecha3.jumpToDate(new Date(instance.currentYear, instance.currentMonth, 1));
        }
    });
    let pickerFecha1 = flatpickr("#idFecha1", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            pickerFecha2.jumpToDate(new Date(instance.currentYear, instance.currentMonth, 1));
        }
    });


    const municipioChoice = new Choices('#municipio-listado');
    $('#municipio-listado').data('choicesInstance', municipioChoice);


    $('#ccaa-listado').on('change', function() {
        cargarProvincias('#provincia-listado', $(this).val(), null)
        .done(function() {
            $('#provincia-listado').change();
        });
    });



    $('#provincia-listado').on('change', function() {
        cargarMunicipios('#municipio-listado',$(this).val(), null);
    });

    const artistasSelect = document.querySelector('#tiposArtista');

    let tipoArtistaSelectChoice = new Choices(artistasSelect, {
            removeItemButton: true
    });

    const agenciasSelect = document.querySelector('#agencias');

    let agenciaSelectChoice = new Choices(agenciasSelect, {
            removeItemButton: true
    });

    const ccaaSelect = document.querySelector('#ccaa');

    let ccaaaSelectChoice = new Choices(ccaaSelect, {
            removeItemButton: true
    });


    const selectAjustes = document.getElementById('selectAjustes');
    let selectAjustesChoice = new Choices(selectAjustes, {
         removeItemButton: false
    });



    // Manejador para cambios en selectAjustes

    selectAjustes.addEventListener('change', function() {
        const ajusteId = this.value;

        if (!ajusteId) {
            return; // No hacer nada si no hay selección
        }

        // Llamada AJAX para obtener el ajuste
        $.ajax({
            type: 'GET',
            url: '/ajustes/' + ajusteId + '/json',
            success: function(data) {
                // Rellenar los campos del formulario con los datos del ajuste
                rellenarCamposAjuste(data);
            },
            error: function(xhr, status, error) {
                console.error('Error al obtener ajuste:', error);
                notif('error', 'Error al cargar el ajuste seleccionado');
            }
        });
    });

    // Función para rellenar los campos con los datos del ajuste
    function rellenarCamposAjuste(ajuste) {

        // Rellenar selects múltiples (Agencias)
        if (ajuste.idsAgencias && ajuste.idsAgencias.length > 0) {
            const agenciasSelect = document.getElementById('agencias');
            $(agenciasSelect).val(ajuste.idsAgencias);

            // Actualizar instancia de Choices si existe
            if (agenciaSelectChoice) {
                agenciaSelectChoice.removeActiveItems();
                ajuste.idsAgencias.forEach((id) => {
                    agenciaSelectChoice.setChoiceByValue(String(id));
                });
            }
        }

        // Rellenar Tipos de Artista
        if (ajuste.idsTipoArtista && ajuste.idsTipoArtista.length > 0) {
            const artistasSelect = document.getElementById('tiposArtista');
            $(artistasSelect).val(ajuste.idsTipoArtista);

            if (tipoArtistaSelectChoice) {
                tipoArtistaSelectChoice.removeActiveItems();
                ajuste.idsTipoArtista.forEach((id) => {
                    tipoArtistaSelectChoice.setChoiceByValue(String(id));
                });
            }
        }

        // Rellenar Comunidades
        if (ajuste.idsComunidades && ajuste.idsComunidades.length > 0) {
            const ccaaSelect = document.getElementById('ccaa');
            $(ccaaSelect).val(ajuste.idsComunidades);

            if (ccaaaSelectChoice) {
                ccaaaSelectChoice.removeActiveItems();
                ajuste.idsComunidades.forEach((id) => {
                    ccaaaSelectChoice.setChoiceByValue(String(id));
                });
            }
        }

        notif('success', 'Ajuste cargado correctamente');
    }
        $('#formGenerarListado').off('submit').on('submit', function(e) {
            e.preventDefault();


            const municipio = document.getElementById('municipio-listado').value;
            // Ejecutar primero las validaciones existentes
            const fechaDesde = document.getElementById('idFechaDesdeListado').value;
            const fechaHasta = document.getElementById('idFechaHastaListado').value;
            const fechasIndividuales = [
                document.getElementById('idFecha1').value,
                document.getElementById('idFecha2').value,
                document.getElementById('idFecha3').value,
                document.getElementById('idFecha4').value,
                document.getElementById('idFecha5').value,
                document.getElementById('idFecha6').value,
                document.getElementById('idFecha7').value
            ];

            // Validaciones existentes
            const hayRangoDeFechas = fechaDesde !== '' && fechaHasta !== '';
            const hayFechasIndividuales = fechasIndividuales.some(fecha => fecha !== '');

            if (!hayRangoDeFechas && !hayFechasIndividuales) {
                notif('error','Introduce las fecha inicial y final o alguna fecha individual');
                return;
            }

            if ((fechaDesde !== '' || fechaHasta !== '') && hayFechasIndividuales) {
                notif('error','Si defines una fecha inicial o final, no puedes incluir fechas individuales. Por favor, corrige los campos.');
                return;
            }

            const todasLasFechas = [fechaDesde, fechaHasta, ...fechasIndividuales].filter(fecha => fecha !== '');
            const fechasUnicas = new Set(todasLasFechas);

            if (fechasUnicas.size !== todasLasFechas.length) {
                notif('error','Las fechas no pueden ser iguales. Por favor, corrige los campos.');
                return;
            }

            // Si pasa las validaciones, proceder con AJAX
            generarPresupuestoAjax();
        });
    // Remover el event listener duplicado que ya existe
    // Solo mantener este que maneja AJAX

});
    function generarPresupuestoAjax() {
        // Deshabilitar el botón
        const $btn = $('#btn-generar-listado');
        const textoOriginal = $btn.text();
        $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i> Generando...');

        const formData = $('#formGenerarListado').serialize();
        const actionUrl = $('#formGenerarListado').attr('action');

        $.ajax({
            type: 'POST',
            url: actionUrl,
            data: formData,
            xhrFields: {
                responseType: 'blob'
            },
            success: function (data, status, xhr) {
                // Rehabilitar el botón
                $btn.prop('disabled', false).text(textoOriginal);

                // Verificar si la respuesta es realmente un PDF
                const contentType = xhr.getResponseHeader('Content-Type');
                if (!contentType || !contentType.includes('application/pdf')) {
                    notif('error', 'Error: La respuesta del servidor no es válida');
                    return;
                }

                // Obtener el nombre del archivo
                const contentDisposition = xhr.getResponseHeader('Content-Disposition');
                let filename = 'presupuesto_' + new Date().getTime() + '.pdf';

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
                const fileId = 'pdf_' + Date.now();
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
                        <button id="btn-share-pdf" class="btn btn-sm btn-outline-light w-50" data-file-id="${fileId}">
                            <i class="fas fa-share"></i> Compartir PDF
                        </button>
                    </div>`,
                    60000
                );

                // Evento para compartir (debe ser dinámico porque el botón se genera en la notificación)
                $(document).on('click', '#btn-share-pdf', function () {
                    const $btn = $(this); // Guardamos una referencia al botón
                    const fileIdToShare = $btn.data('file-id');
                    const fileMetadata = window[fileIdToShare];

                    // Prevenir múltiples solicitudes de compartir
                    if ($btn.prop('disabled')) {
                        return;
                    }

                    if (navigator.share && navigator.canShare) {
                        const file = new File([fileMetadata.blob], fileMetadata.filename, { type: 'application/pdf' });

                        // Verificar si el navegador y dispositivo soportan la funcionalidad de compartir
                        if (navigator.canShare({ files: [file] })) {
                            // Deshabilitar el botón para evitar interacciones múltiples
                            $btn.prop('disabled', true);

                            navigator
                                .share({
                                    files: [file],
                                    title: 'Presupuesto Generado',
                                    text: 'Aquí tienes el PDF generado.',
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
                                    $btn.prop('disabled', false);
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
                    delete window[fileId]; // Elimina la referencia temporal
                    window.URL.revokeObjectURL(url);
                }, 60000);
            },
            error: function(xhr, status, error) {
                // Rehabilitar el botón
                $btn.prop('disabled', false).text(textoOriginal);

                console.error('Error al generar presupuesto:', error);

                let errorMessage = 'Error al generar el presupuesto';

                // Si la respuesta es texto/JSON, intentar leerla
                if (xhr.responseText) {
                    try {
                        const reader = new FileReader();
                        reader.onload = function() {
                            try {
                                const errorData = JSON.parse(reader.result);
                                if (errorData.message) {
                                    notif('error', errorData.message);
                                    return;
                                }
                            } catch (e) {
                                // No es JSON, usar mensaje genérico
                            }
                            notif('error', errorMessage);
                        };
                        reader.readAsText(new Blob([xhr.response]));
                        return;
                    } catch (e) {
                        // Error al leer la respuesta
                    }
                }

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



    function clearFlatpickrDate(inputId) {
        const input = document.getElementById(inputId);
        if (input && input._flatpickr) {
            input._flatpickr.clear(); // Limpia la fecha seleccionada
        }
    }