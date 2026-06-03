

$(document).ready(function(){
    let pickerFechaHasta = flatpickr("#idFechaHastaListado", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false,
        onChange: function() {
            actualizarModoFechas();
        }
    });
    let pickerFechaDesde = flatpickr("#idFechaDesdeListado", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            pickerFechaHasta.set("minDate", dateStr);
            actualizarModoFechas();
        },
        onMonthChange: function(selectedDates, dateStr, instance) {
            pickerFechaHasta.clear();
            pickerFechaHasta.changeMonth(instance.currentMonth);
        }
    });

    let pickerFecha7 = flatpickr("#idFecha7", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false,
        onChange: function() {
            actualizarModoFechas();
        }
    });
    let pickerFecha6 = flatpickr("#idFecha6", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            pickerFecha7.jumpToDate(new Date(instance.currentYear, instance.currentMonth, 1));
            actualizarModoFechas();
        }
    });
    let pickerFecha5 = flatpickr("#idFecha5", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            pickerFecha6.jumpToDate(new Date(instance.currentYear, instance.currentMonth, 1));
            actualizarModoFechas();
        }
    });
    let pickerFecha4 = flatpickr("#idFecha4", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            pickerFecha5.jumpToDate(new Date(instance.currentYear, instance.currentMonth, 1));
            actualizarModoFechas();
        }
    });
    let pickerFecha3 = flatpickr("#idFecha3", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            pickerFecha4.jumpToDate(new Date(instance.currentYear, instance.currentMonth, 1));
            actualizarModoFechas();
        }

    });
    let pickerFecha2 = flatpickr("#idFecha2", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            pickerFecha3.jumpToDate(new Date(instance.currentYear, instance.currentMonth, 1));
            actualizarModoFechas();
        }
    });
    let pickerFecha1 = flatpickr("#idFecha1", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            pickerFecha2.jumpToDate(new Date(instance.currentYear, instance.currentMonth, 1));
            actualizarModoFechas();
        }
    });

    const camposRango = ['#idFechaDesdeListado', '#idFechaHastaListado'];
    const camposIndividuales = ['#idFecha1', '#idFecha2', '#idFecha3', '#idFecha4', '#idFecha5', '#idFecha6', '#idFecha7'];
    const $alertaFiltrosRequeridos = $('#listado-filtros-requeridos-alert');

    function tieneValor(selector) {
        return $(selector).val() !== '';
    }

    function setCamposFechasDisabled(selectores, disabled) {
        selectores.forEach(function(selector) {
            const input = document.querySelector(selector);
            if (!input || !input._flatpickr) {
                return;
            }

            input._flatpickr.set('clickOpens', !disabled);
            $(input._flatpickr.altInput).prop('disabled', disabled);
            $(input._flatpickr.altInput).toggleClass('bg-light', disabled);
        });
    }

    function actualizarModoFechas() {
        const hayRango = camposRango.some(tieneValor);
        const hayFechasIndividuales = camposIndividuales.some(tieneValor);

        setCamposFechasDisabled(camposIndividuales, hayRango);
        setCamposFechasDisabled(camposRango, hayFechasIndividuales);
    }

    window.actualizarModoFechasListado = actualizarModoFechas;
    actualizarModoFechas();

    function obtenerErroresFiltrosRequeridos() {
        const errores = [];

        if (!($('#agencias').val() || []).length) {
            errores.push('Selecciona al menos una agencia.');
        }

        if (!($('#tiposArtista').val() || []).length) {
            errores.push('Selecciona al menos un tipo de artista.');
        }

        if (!($('#ccaa').val() || []).length) {
            errores.push('Selecciona al menos una comunidad del artista.');
        }

        return errores;
    }

    function mostrarErroresFiltrosRequeridos(errores) {
        if (!errores.length) {
            $alertaFiltrosRequeridos.addClass('d-none').text('');
            return;
        }

        $alertaFiltrosRequeridos.removeClass('d-none').html(errores.join('<br>'));
    }


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

    $('#agencias, #tiposArtista, #ccaa').on('change', function() {
        mostrarErroresFiltrosRequeridos(obtenerErroresFiltrosRequeridos());
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

    $('#btn-guardar-configuracion-listado').on('click', function() {
        guardarConfiguracionListado();
    });

    // Función para rellenar los campos con los datos del ajuste
    function rellenarCamposAjuste(ajuste) {

        // Rellenar selects múltiples (Agencias)
        const agenciasIds = ajuste.idsAgencias || [];
        const agenciasSelect = document.getElementById('agencias');
        $(agenciasSelect).val(agenciasIds);

        // Actualizar instancia de Choices si existe
        if (agenciaSelectChoice) {
            agenciaSelectChoice.removeActiveItems();
            agenciasIds.forEach((id) => {
                agenciaSelectChoice.setChoiceByValue(String(id));
            });
        }

        // Rellenar Tipos de Artista
        const tipoArtistaIds = ajuste.idsTipoArtista || [];
        const artistasSelect = document.getElementById('tiposArtista');
        $(artistasSelect).val(tipoArtistaIds);

        if (tipoArtistaSelectChoice) {
            tipoArtistaSelectChoice.removeActiveItems();
            tipoArtistaIds.forEach((id) => {
                tipoArtistaSelectChoice.setChoiceByValue(String(id));
            });
        }

        // Rellenar Comunidades
        const comunidadesIds = ajuste.idsComunidades || [];
        const ccaaSelect = document.getElementById('ccaa');
        $(ccaaSelect).val(comunidadesIds);

        if (ccaaaSelectChoice) {
            ccaaaSelectChoice.removeActiveItems();
            comunidadesIds.forEach((id) => {
                ccaaaSelectChoice.setChoiceByValue(String(id));
            });
        }

        notif('success', 'Ajuste cargado correctamente');
    }

    function guardarConfiguracionListado() {
        const ajusteId = $('#selectAjustes').val();

        if (!ajusteId) {
            notif('error', 'Selecciona una configuración para guardar');
            return;
        }

        const $btn = $('#btn-guardar-configuracion-listado');
        const contenidoOriginal = $btn.html();
        const datos = $('#formGenerarListado input[type="hidden"]').serializeArray();

        ($('#agencias').val() || []).forEach((id) => datos.push({name: 'idsAgencias', value: id}));
        ($('#tiposArtista').val() || []).forEach((id) => datos.push({name: 'idsTipoArtista', value: id}));
        ($('#ccaa').val() || []).forEach((id) => datos.push({name: 'idsComunidades', value: id}));

        $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i> Guardando...');

        $.ajax({
            type: 'POST',
            url: '/ajustes/' + ajusteId + '/opciones-listado',
            data: $.param(datos),
            success: function(response) {
                if (response && response.success) {
                    notif('success', response.message || 'Configuración guardada correctamente');
                    return;
                }

                notif('error', response && response.message ? response.message : 'Error al guardar la configuración');
            },
            error: function() {
                notif('error', 'Error al guardar la configuración');
            },
            complete: function() {
                $btn.prop('disabled', false).html(contenidoOriginal);
            }
        });
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

            if ((fechaDesde !== '' || fechaHasta !== '') && !hayRangoDeFechas) {
                notif('error','Completa fecha inicial y fecha final, o borra el rango y usa fechas sueltas.');
                return;
            }

            if (!hayRangoDeFechas && !hayFechasIndividuales) {
                notif('error','Introduce fecha inicial y final, o al menos una fecha suelta.');
                return;
            }

            if ((fechaDesde !== '' || fechaHasta !== '') && hayFechasIndividuales) {
                notif('error','No mezcles rango de fechas con fechas sueltas. Borra uno de los dos bloques.');
                return;
            }

            const todasLasFechas = [fechaDesde, fechaHasta, ...fechasIndividuales].filter(fecha => fecha !== '');
            const fechasUnicas = new Set(todasLasFechas);

            if (fechasUnicas.size !== todasLasFechas.length) {
                notif('error','Las fechas no pueden ser iguales. Por favor, corrige los campos.');
                return;
            }

            const erroresFiltrosRequeridos = obtenerErroresFiltrosRequeridos();
            if (erroresFiltrosRequeridos.length) {
                mostrarErroresFiltrosRequeridos(erroresFiltrosRequeridos);
                notif('error', erroresFiltrosRequeridos.join(' '));
                return;
            }

            mostrarErroresFiltrosRequeridos([]);

            // Si pasa las validaciones, proceder con AJAX
            generarPresupuestoAjax();
        });
    // Remover el event listener duplicado que ya existe
    // Solo mantener este que maneja AJAX

});
    function generarPresupuestoAjax() {
        // Deshabilitar el botón
        const $btn = $('#btn-generar-listado');
        const contenidoOriginal = $btn.html();
        $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i> Generando...');

        const formData = $('#formGenerarListado').serialize();
        const actionUrl = $('#formGenerarListado').attr('action');
        let nativeXhr = null;

        $.ajax({
            type: 'POST',
            url: actionUrl,
            data: formData,
            xhr: function() {
                nativeXhr = new window.XMLHttpRequest();
                return nativeXhr;
            },
            xhrFields: {
                responseType: 'blob'
            },
            success: function (data, status, xhr) {
                // Rehabilitar el botón
                $btn.prop('disabled', false).html(contenidoOriginal);

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
                $btn.prop('disabled', false).html(contenidoOriginal);

                console.error('Error al generar presupuesto:', error);

                let errorMessage = 'Error al generar el listado';

                const notifyError = function(message) {
                    notif('error', message || errorMessage);
                };

                const notifyResponseError = function(responseText) {
                    if (responseText) {
                        try {
                            const errorData = JSON.parse(responseText);
                            if (errorData.message) {
                                notifyError(errorData.message);
                                return;
                            }
                        } catch (e) {
                            // No es JSON, usar mensaje por código de estado
                        }
                    }

                    notifyError(getErrorMessageByStatus(xhr.status, errorMessage));
                };

                if (xhr.responseJSON && xhr.responseJSON.message) {
                    notifyError(xhr.responseJSON.message);
                    return;
                }

                const response = xhr.response || (nativeXhr ? nativeXhr.response : null);

                // Con responseType='blob', los errores JSON llegan en la respuesta nativa, no en xhr.responseText.
                if (response) {
                    const responseBlob = response instanceof Blob ? response : new Blob([response]);
                    const reader = new FileReader();
                    reader.onload = function() {
                        notifyResponseError(reader.result);
                    };
                    reader.onerror = function() {
                        notifyError(getErrorMessageByStatus(xhr.status, errorMessage));
                    };
                    reader.readAsText(responseBlob);
                    return;
                }

                // Si la respuesta es texto/JSON, intentar leerla.
                if (xhr.responseText) {
                    notifyResponseError(xhr.responseText);
                    return;
                }

                notifyError(getErrorMessageByStatus(xhr.status, errorMessage));
            }
        });
    }

    function getErrorMessageByStatus(status, defaultMessage) {
        // Mensajes específicos por código de estado
        switch (status) {
            case 400:
                return 'Revisa los filtros obligatorios y los datos del formulario';
            case 500:
                return 'Error interno del servidor';
            case 0:
                return 'Error de conexión con el servidor';
            default:
                return defaultMessage;
        }
    }



    function clearFlatpickrDate(inputId) {
        const input = document.getElementById(inputId);
        if (input && input._flatpickr) {
            input._flatpickr.clear(); // Limpia la fecha seleccionada
        }
        if (typeof window.actualizarModoFechasListado === 'function') {
            window.actualizarModoFechasListado();
        }
    }
