$(document).ready(function(){
    let pickerFechaHasta = flatpickr("#idFechaHasta", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
            ,
            onChange: function(selectedDates, dateStr, instance) {
                const fechaDesde = $('#idFechaDesde').val();
                const fechaHasta = $('#idFechaHasta').val();

                if (fechaDesde && fechaHasta && fechaDesde === fechaHasta) {
                    const idArtista = $('#idArtista').val();
                    cargarTarifasCcaa(idArtista, fechaDesde);
                }
                else {
                    $('#tarifas-ccaa-container').empty();
                }
            }
    });

    let pickerFechaDesde = flatpickr("#idFechaDesde", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            pickerFechaHasta.set("minDate", dateStr);
            const fechaDesde = $('#idFechaDesde').val();
            const fechaHasta = $('#idFechaHasta').val();

            if (fechaDesde && fechaHasta && fechaDesde === fechaHasta) {
                const idArtista = $('#idArtista').val();
                cargarTarifasCcaa(idArtista, fechaDesde);
            }
            else {
                $('#tarifas-ccaa-container').empty();
            }

        },
        onMonthChange: function(selectedDates, dateStr, instance) {
            pickerFechaHasta.clear();
            pickerFechaHasta.changeMonth(instance.currentMonth);
        }
    });


    let pickerFechaOcupacion = flatpickr("#idFechaOcupacion", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
            ,
            onChange: function(selectedDates, dateStr, instance) {
//                pickerFechaDesde.set("maxDate", dateStr);
                let idArtista = $("#id-artista-modal-ocupacion").val();
                let fecha = $("#idFechaOcupacion").val();
                cargarTarifaFecha(idArtista, fecha);

            }

    });

    let calendarEl = document.getElementById('calendar');
    let idArtista = document.getElementById('idArtista').value;
    if (calendarEl){
        $("#modal-tarifa-eliminar").hide();

        var calendar = new FullCalendar.Calendar(calendarEl, {
          initialView: 'dayGridMonth',
          locale: 'es',
          timeZone: 'UTC',
          height:'auto',
          selectable: true,
          unselectAuto: true,
          headerToolbar: {
              start: 'title',
              center: '',
              end: 'prev,next'
          },
          firstDay : 1,
          events: {
              url: "/fecha/list/" + idArtista,
              method: 'GET',
              success: function(content, xhr) {
                if (!content || content.length === 0) {
                    notif("warning", "Es necesario añadir tarifas para que los artistas aparezcan en los listados. Si no quieres especificar importe, puedes crear tarifas con precio 0.");
                }
              },
              failure: function() {
                  alert('Hubo un error al cargar los eventos.');
              },

              eventDataTransform: function(event) {
                  // Asignar una clase personalizada basada en el tipo
                  let tipoFecha = event.tipoFecha || 'otro';
                  let tipoOcupacion = event.tipoOcupacion || 'otro';
                  let className = event.estado === 'Pendiente' ? 'estado-pendiente' : ((tipoFecha === 'Ocupacion') ? `tipo-${tipoOcupacion}` : `tipo-${tipoFecha}`);

                  return {
                      ...event,
                      classNames: [className]
                  };
              }
          },
          eventClick: function(info) {
            const tipoFecha = info.event.extendedProps.tipoFecha; // Accede al tipo
            const tipoOcupacion = info.event.extendedProps.tipoOcupacion;

            if (tipoFecha === 'Tarifa') {
                checkPermission( idArtista, 'ARTISTA', 'CREAR_TARIFAS')
                    .done(function(hasPermission) {
                        if (hasPermission) {
                            $("#id-tarifa").val(info.event.id);
                            $("#modal-tarifa-eliminar").show();
                            pickerFechaDesde.setDate(moment(info.event.start).format('DD-MM-YYYY'));
                            pickerFechaHasta.setDate(moment(info.event.start).format('DD-MM-YYYY'));
                            $("#importe").val(info.event.title);
                            $('#modalNuevaTarifa').modal('show');

                        }
                    })

            }
            else if (tipoFecha === 'Ocupacion') {

                  checkPermission( idArtista, 'ARTISTA', 'OCUPACIONES')
                      .done(function(hasPermission) {
                          if (hasPermission) {
                            const id = info.event.id;
                            pickerFechaOcupacion.setDate(moment(info.event.start).format('DD-MM-YYYY'));

                            let ocupacionDto = obtenerOcupacionDto(info.event.id);
                            $('#modalNuevaOcupacion').modal('toggle');

                          }
                      })

            }

          },
            eventDidMount: function(info) {

                const isTouchDevice = 'ontouchstart' in window || navigator.maxTouchPoints > 0;
                if (!isTouchDevice) {
                    // Inicializar el tooltip si no es un dispositivo táctil
                    const tipoFecha = info.event.extendedProps.tipoFecha || 'otro';
                    const tooltipEvent = info.event.extendedProps.tooltip || 'otro';

                    // Asignar el atributo 'title' para el tooltip
                    info.el.setAttribute('title', tooltipEvent);

                    // Inicializar el tooltip de Bootstrap
                    var tooltip = new bootstrap.Tooltip(info.el, {
                        container: 'body',
                        placement: 'top',
                        trigger: 'hover',
                        html: true
                    });
                }


            },
          dateClick: function(info) {

              checkPermission( idArtista, 'ARTISTA', 'CREAR_TARIFAS')
                  .done(function(hasPermission) {
                      if (hasPermission) {
                        $("#id-tarifa").val("");
                        $("#modal-tarifa-eliminar").hide();
                        pickerFechaDesde.setDate(moment(info.date).format('DD-MM-YYYY'));
                        pickerFechaHasta.setDate(moment(info.date).format('DD-MM-YYYY'));
                        $('#modalNuevaTarifa').modal('show');
                      }
                  })


          }
        });
        calendar.render();

    }



    $("#modalNuevaTarifa").submit(function (event) {

        event.preventDefault();
        guardar_tarifas();
        calendar.refetchEvents();

    });

    $("#modal-tarifa-eliminar").click(function (event) {

    showConfirmationModal(function (confirmed) {
        if (confirmed) {
        eliminar_tarifas();
        calendar.refetchEvents();
        }
    });



    });

    $("#modalIncrementos").submit(function (event) {

        event.preventDefault();
        guardar_incrementos();
        cargarListaIncrementos();
        calendar.refetchEvents();

    });


    $("#formNuevaOcupacion").submit(function (event) {

        event.preventDefault();
        guardar_ocupacion();
        calendar.refetchEvents();
    });

    $("#btn-incrementos").click(function (event) {
        cargarListaIncrementos();

    });

    $("#btn-confirmar-ocupacion").click(function (event) {

        $('#modalNuevaOcupacion').modal('hide');

        showConfirmationModal(function (confirmed) {
            if (confirmed) {
                confirmarOcupacion($('#id-ocupacion').val());
                calendar.refetchEvents();
            }
        });


    });

    $("#btn-anular-ocupacion").click(function (event) {

        $('#modalNuevaOcupacion').modal('hide');

        showConfirmationModal(function (confirmed) {

            if (confirmed) {
                anularOcupacion($('#id-ocupacion').val());
                calendar.refetchEvents();
            }

        });
    });

    $("#btnModalNuevaOcupacion").click(function (event) {
        $('#badge-estado-ocupacion').text('');
//        pickerFechaOcupacion.setDate('');
        $("#id-ocupacion").val('');
        $("#ccaa-ocupacion").change();
        $("#localidad-ocupacion").val('');
        $("#lugar-ocupacion").val('');
        $("#importe-ocupacion").val(0);
        $("#porcentaje-repre-ocupacion").val(0);
        $("#iva-ocupacion").val(0);
        $('#matinal-ocupacion').prop('checked', false);
        $('#solo-matinal-ocupacion').prop('checked', false);
        $("#observaciones-ocupacion").val('');
        $("#orquestasdegalicia-ocupacion").val('');
        $('#usuario-ocupacion').data('choicesInstance').setChoiceByValue(String($("#idUsuarioAutenticado").val()));

        mostrarOcultarBotonesModalOcupacion();
    });


   // Manejar el submit del formulario de tarifa anual
    $('#modalTarifaAnual form').on('submit', function(e) {
        e.preventDefault();

        generarTarifaAnualAjax();
    });



    $('#modalNuevaTarifa').on('shown.bs.modal', function () {
        $('#importe').focus().select();

        // Verificar si fechaDesde === fechaHasta
        const fechaDesde = $('#idFechaDesde').val();
        const fechaHasta = $('#idFechaHasta').val();

        if (fechaDesde && fechaHasta && fechaDesde === fechaHasta) {
            const idArtista = $('#idArtista').val();
            cargarTarifasCcaa(idArtista, fechaDesde);
        }
        else {
            $('#tarifas-ccaa-container').empty();
        }

    });

    function cargarTarifasCcaa(idArtista, fecha) {
        $.ajax({
            type: 'GET',
            url: '/tarifa/lista/' + idArtista + '/' + fecha,
            success: function(data) {
                if (data && data.length > 0) {
                    console.log('Tarifas de artistas de la misma CCAA:', data);
                    // Aquí puedes mostrar los datos en el modal o hacer lo que necesites
                    mostrarTarifasCcaa(data);
                }
            },
            error: function(xhr, status, error) {
                console.error('Error al cargar tarifas de CCAA:', error);
            }
        });
    }

    function mostrarTarifasCcaa(tarifas) {
        // Verificar si ya existe el contenedor, si no crearlo
        let contenedor = $('#tarifas-ccaa-container');
        if (contenedor.length === 0) {
            contenedor = $('<div id="tarifas-ccaa-container" class="mt-3"></div>');
            $('#modalNuevaTarifa .modal-body').append(contenedor);
        }

        // Limpiar el contenedor
        contenedor.empty();

        // Crear la tabla con las tarifas
        let html = '<div class="alert alert-info"><strong>Tarifas de otros artistas para esta fecha:</strong></div>';
          html += '<div class="table-responsive" style="max-height: 200px; overflow-y: auto;">';
        html += '<table class="table table-sm table-striped">';
        html += '<thead><tr><th>Artista</th><th class="text-end">Importe</th></tr></thead>';
        html += '<tbody>';

        tarifas.forEach(function(tarifa) {
            html += '<tr>';
            html += '<td>' + tarifa.nombreArtista + '</td>'; 
            html += '<td class="text-end">' + tarifa.importe.toFixed(0) + '</td>';
            html += '</tr>';
        });

        html += '</tbody></table></div>';
        contenedor.html(html);
    }

    $('#modalNuevaTarifa').on('hidden.bs.modal', function () {
        // Limpiar el contenedor de tarifas CCAA al cerrar el modal
        $('#tarifas-ccaa-container').remove();
    });

    function generarTarifaAnualAjax() {
        // Deshabilitar el botón
        const $btn = $('#btn-genarar-tarifa-anual');
        const textoOriginal = $btn.text();
        $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i> Generando...');

        const formData = $('#modalTarifaAnual form').serialize();
        const actionUrl = $('#modalTarifaAnual form').attr('action');

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

                const contentType = xhr.getResponseHeader('Content-Type');
                if (!contentType || (!contentType.includes('application/pdf') && !contentType.includes('application/octet-stream'))) {
                    notif('error', 'Error: La respuesta del servidor no es válida');
                    return;
                }

                // Obtener el nombre y crear Blob
                const contentDisposition = xhr.getResponseHeader('Content-Disposition');
                let filename = 'tarifa_anual_' + new Date().getTime() + '.pdf';
                if (contentDisposition) {
                    const matches = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
                    if (matches != null && matches[1]) {
                        filename = matches[1].replace(/['"]/g, '');
                    }
                }

                const blob = new Blob([data], { type: 'application/pdf' });
                const url = window.URL.createObjectURL(blob);

                // Generar un ID único para el archivo
                const fileId = 'pdf_' + Date.now();
                window[fileId] = { blob: blob, url: url, filename: filename };

                // Notificación con botones para abrir y compartir
                notifDuration(
                    'success',
                    `<div class="d-flex gap-2">
                        <button onclick="window.open(window.${fileId}.url, '_blank')" class="btn btn-sm btn-outline-light w-50">
                            <i class="fas fa-eye"></i> Ver PDF
                        </button>
                        <button id="btn-share-pdf" class="btn btn-sm btn-outline-light w-50" data-file-id="${fileId}">
                            <i class="fas fa-share"></i> Compartir PDF
                        </button>
                    </div>`,
                    60000
                );

                // Configuración para compartir el archivo
                $(document).on('click', '#btn-share-pdf', function () {
                    const $btn = $(this);
                    const fileToShare = window[$btn.data('file-id')];

                    if ($btn.prop('disabled')) {
                        return;
                    }

                    if (navigator.share && navigator.canShare) {
                        const file = new File([fileToShare.blob], fileToShare.filename, { type: 'application/pdf' });

                        if (navigator.canShare({ files: [file] })) {
                            $btn.prop('disabled', true);

                            navigator
                                .share({
                                    files: [file],
                                    title: 'Tarifa Anual Generada',
                                    text: 'Aquí tienes el PDF generado.',
                                })
                                .then(() => console.log('Archivo compartido exitosamente.'))
                                .catch(error => {
                                    console.error('Error al compartir:', error);
                                    notif('error', 'No se pudo compartir el archivo.');
                                })
                                .finally(() => $btn.prop('disabled', false));
                        } else {
                            notif('error', 'Tu dispositivo no admite compartir este archivo.');
                        }
                    } else {
                        notif('error', 'Compartir archivos no está disponible en tu navegador.');
                    }
                });

                // Limpieza del recurso Blob
                setTimeout(() => {
                    delete window[fileId];
                    window.URL.revokeObjectURL(url);
                }, 60000);

                // Ocultar el modal
                $('#modalTarifaAnual').modal('hide');
            },
            error: function(xhr, status, error) {
                $btn.prop('disabled', false).text(textoOriginal);

                let errorMessage = 'Error al generar la tarifa anual';
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
                            } catch (e) {}
                            notif('error', errorMessage);
                        };
                        reader.readAsText(new Blob([xhr.response]));
                        return;
                    } catch (e) {}
                }

                switch (xhr.status) {
                    case 400:
                        errorMessage = 'Formulario inválido. Verifica los datos.';
                        break;
                    case 403:
                        errorMessage = 'No tienes permisos para esta operación.';
                        break;
                    case 500:
                        errorMessage = 'Error interno del servidor.';
                        break;
                    default:
                        errorMessage = 'Error desconocido.';
                        break;
                }

                notif('error', errorMessage);
            }
        });
    }


});


function cargarListaIncrementos(){

    let idArtista = $("#id-artista-modal").val();

    $.ajax({
        url: '/incremento/list/'+idArtista,
        method: 'GET',
        dataType: 'json',
        success: function(data) {

            $('#tablaIncrementos tbody').empty();
            for (var i = 0; i < data.length; i++) {

                $('#tablaIncrementos tbody').append(
                    '<tr>' +
                    '<td>' + data[i].descripcionProvincia + '</td>' +
                    '<td>' + data[i].decripcionTipoIncremento + '</td>' +
                    '<td class="text-end">' + data[i].incremento + '</td>' +
                    '</tr>'
                );
            }

            // Agregar eventos click a los botones de editar y eliminar
            $('.editar').click(function() {
                var id = $(this).data('id');
            });

            $('.eliminar').click(function() {
                var id = $(this).data('id');
            });
        },
        error: function(error) {
            console.error('Error al obtener la lista de objetos:', error);
        }
    });




}

function guardar_tarifas() {

    let tarifaSaveDto = crearTarifaSaveDto();

    $("#btn-guardar-tarifa").prop("disabled", true);

    sendTarifaPost(tarifaSaveDto);
    $('#modalNuevaTarifa').modal('toggle');

}

function guardar_incrementos() {

    let incrementoDto = crearIncrementoDto();

    $("#btn-guardar-incremento").prop("disabled", true);

    sendIncrementoPost(incrementoDto);




}

function eliminar_tarifas() {

    let tarifaSaveDto = crearTarifaSaveDto();

    $("#modal-tarifa-eliminar").prop("disabled", true);

    sendTarifaEliminarPost(tarifaSaveDto);
    $('#modalNuevaTarifa').modal('toggle');

}


function crearTarifaSaveDto() {
    let tarifaSaveDto = {}

    if ($("#id-tarifa").val()!=""){
        tarifaSaveDto["id"] = $("#id-tarifa").val();
    }
    tarifaSaveDto["idArtista"] = $("#id-artista-modal").val();
    tarifaSaveDto["fechaDesde"] = moment($("#idFechaDesde").val(), "DD-MM-YYYY").format("YYYY-MM-DDTHH:mm:ss");

    if ($("#idFechaHasta").val()!=""){
        tarifaSaveDto["fechaHasta"] = moment($("#idFechaHasta").val(), "DD-MM-YYYY").format("YYYY-MM-DDTHH:mm:ss");
    }

    tarifaSaveDto["importe"] = $("#importe").val();

    return tarifaSaveDto;
}

function sendTarifaPost(tarifaSaveDto){
    $.ajax({
        type: "POST",
        contentType: "application/json; charset=utf-8",
        url: "/tarifa/save",
        data: JSON.stringify(tarifaSaveDto),
        dataType: 'json',
        cache: false,
        timeout: 600000,
        async: false,
        success: function (data) {
            $("#btn-guardar-tarifa").prop("disabled", false);
            if (data.success){
                notif("success", data.message);
            }
            else {
                notif("error", data.message);
            }
        },
        error: function (e) {
            $("#btn-guardar-tarifa").prop("disabled", false);
            console.log(e);
            notyf.error(e);
        }
    });
}



function crearIncrementoDto() {
    let incrementoDto = {}

    incrementoDto["idArtista"] = $("#id-artista-modal").val();
    incrementoDto["idProvincia"] = $("#provincia").val();
    incrementoDto["idTipoIncremento"] = $("#tipoIncremento").val();
    incrementoDto["incremento"] = $("#incremento").val();


    return incrementoDto;
}


function sendIncrementoPost(incrementoDto){
    $.ajax({
        type: "POST",
        contentType: "application/json; charset=utf-8",
        url: "/incremento/save",
        data: JSON.stringify(incrementoDto),
        dataType: 'json',
        cache: false,
        timeout: 600000,
        async: false,
        success: function (data) {
            $("#btn-guardar-incremento").prop("disabled", false);
            if (data.success){
                notif("success", data.message);
                cargarListaIncrementos();
            }
            else {
                notif("error", data.message);
            }
        },
        error: function (e) {
            $("#btn-guardar-incremento").prop("disabled", false);
            console.log(e);
            notyf.error(e);
        }
    });
}

function sendTarifaEliminarPost(tarifaSaveDto){
    $.ajax({
        type: "POST",
        contentType: "application/json; charset=utf-8",
        url: "/tarifa/eliminar",
        data: JSON.stringify(tarifaSaveDto),
        dataType: 'json',
        cache: false,
        timeout: 600000,
        async: false,
        success: function (data) {
            $("#modal-tarifa-eliminar").prop("disabled", false);
            if (data.success){
                notif("success", data.message);
            }
            else {
                notif("error", data.message);
            }
        },
        error: function (e) {
            $("#modal-tarifa-eliminar").prop("disabled", false);
            console.log(e);
            notyf.error(e);
        }
    });
}


function cargarTarifaFecha(idArtista, fecha){
    $.ajax({
        url: '/tarifa/'+idArtista+'/'+fecha,
        method: 'GET',
        dataType: 'json',
        success: function(data) {
            $("#importe-ocupacion").val(data!=null ? data.title : 0);
        },
        error: function(error) {
            console.error('Error al obtener la lista de objetos:', error);
        }
    });
}