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
        onMonthChange: function(selectedDates, dateStr, instance) {
            pickerFecha7.clear();
            pickerFecha7.changeMonth(instance.currentMonth);
        }
    });
    let pickerFecha5 = flatpickr("#idFecha5", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onMonthChange: function(selectedDates, dateStr, instance) {
            pickerFecha6.clear();
            pickerFecha6.changeMonth(instance.currentMonth);
        }
    });
    let pickerFecha4 = flatpickr("#idFecha4", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onMonthChange: function(selectedDates, dateStr, instance) {
            pickerFecha5.clear();
            pickerFecha5.changeMonth(instance.currentMonth);
        }
    });
    let pickerFecha3 = flatpickr("#idFecha3", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onMonthChange: function(selectedDates, dateStr, instance) {
            pickerFecha4.clear();
            pickerFecha4.changeMonth(instance.currentMonth);
        }
    });
    let pickerFecha2 = flatpickr("#idFecha2", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onMonthChange: function(selectedDates, dateStr, instance) {
            pickerFecha3.clear();
            pickerFecha3.changeMonth(instance.currentMonth);
        }
    });
    let pickerFecha1 = flatpickr("#idFecha1", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onMonthChange: function(selectedDates, dateStr, instance) {
            pickerFecha2.clear();
            pickerFecha2.changeMonth(instance.currentMonth);
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

    const form = document.getElementById('formGenerarListado');
    form.addEventListener('submit', (event) => {
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

        // Validar que haya al menos un rango de fechas o una fecha individual
        const hayRangoDeFechas = fechaDesde !== '' && fechaHasta !== '';
        const hayFechasIndividuales = fechasIndividuales.some(fecha => fecha !== '');

        if (!hayRangoDeFechas && !hayFechasIndividuales) {
            event.preventDefault(); // Evita que se envíe el formulario
            notif('error','Introduce las fecha inicial y final o alguna fecha individual');
            return;
        }
        if ((fechaDesde !== '' || fechaHasta !== '') && hayFechasIndividuales) {
            event.preventDefault(); // Evita que se envíe el formulario
            notif('error','Si defines una fecha inicial o final, no puedes incluir fechas individuales. Por favor, corrige los campos.');
            return;
        }

        // Validar que no haya fechas duplicadas
        const todasLasFechas = [fechaDesde, fechaHasta, ...fechasIndividuales].filter(fecha => fecha !== '');
        const fechasUnicas = new Set(todasLasFechas);

        if (fechasUnicas.size !== todasLasFechas.length) {
            event.preventDefault(); // Evita que se envíe el formulario
            notif('error','Las fechas no pueden ser iguales. Por favor, corrige los campos.');
        }
    });
    const artistasSelect = document.querySelector('#tiposArtista');

    new Choices(artistasSelect, {
            removeItemButton: true
    });

    const agenciasSelect = document.querySelector('#agencias');

    new Choices(agenciasSelect, {
            removeItemButton: true
    });

    const ccaaSelect = document.querySelector('#ccaa');

    new Choices(ccaaSelect, {
            removeItemButton: true
    });

});

$(document).ready(function() {
   // Remover el event listener duplicado que ya existe
    // Solo mantener este que maneja AJAX

    $('#formGenerarListado').off('submit').on('submit', function(e) {
        e.preventDefault();

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
            success: function(data, status, xhr) {
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

                // Crear blob y URL
                const blob = new Blob([data], { type: 'application/pdf' });
                const url = window.URL.createObjectURL(blob);

                // 1. Descarga automática
                const downloadLink = document.createElement('a');
                downloadLink.style.display = 'none';
                downloadLink.href = url;
                downloadLink.download = filename;
                document.body.appendChild(downloadLink);
                downloadLink.click();

                // 2. Notificación con opción de abrir (solo si el navegador soporta object URLs)
                if (window.URL && window.URL.createObjectURL) {
                    // Crear un ID único para este archivo
                    const fileId = 'pdf_' + Date.now();
                    
                    // Guardar referencia temporal
                    window[fileId] = {
                        url: url,
                        filename: filename
                    };
                    
                    notifDuration('success',
                        `Presupuesto descargado<br>
                        <button onclick="window.open(window.${fileId}.url, '_blank')" 
                                class="btn btn-sm btn-outline-light mt-2">
                            <i class="fas fa-eye"></i> Ver PDF
                        </button>`, 15000
                    );
                    
                    // Cleanup después de 30 segundos
                    setTimeout(() => {
                        document.body.removeChild(downloadLink);
                        window.URL.revokeObjectURL(url);
                        delete window[fileId];
                    }, 30000);
                } else {
                    // Fallback para navegadores antiguos
                    notif('success', 'Presupuesto descargado correctamente');
                    setTimeout(() => {
                        document.body.removeChild(downloadLink);
                        window.URL.revokeObjectURL(url);
                    }, 1000);
                }
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

});


function clearFlatpickrDate(inputId) {
    const input = document.getElementById(inputId);
    if (input && input._flatpickr) {
        input._flatpickr.clear(); // Limpia la fecha seleccionada
    }
}