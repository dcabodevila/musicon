$(document).ready(function(){
    let pickerFechaHasta = flatpickr("#idFechaHasta", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
    });

    let pickerFechaDesde = flatpickr("#idFechaDesde", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            pickerFechaHasta.set("minDate", dateStr);
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

    const municipioChoice = new Choices('#municipio-ocupacion');
    $('#municipio-ocupacion').data('choicesInstance', municipioChoice);

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


    $("#modalNuevaOcupacion").submit(function (event) {

        event.preventDefault();
        guardar_ocupacion();
        calendar.refetchEvents();
    });

    $("#btn-incrementos").click(function (event) {
        cargarListaIncrementos();

    });

    $("#btn-confirmar-ocupacion").click(function (event) {
        showConfirmationModal(function (confirmed) {
            if (confirmed) {
                confirmarOcupacion($('#id-ocupacion').val());
                calendar.refetchEvents();
            }
        });


    });

    $("#btn-anular-ocupacion").click(function (event) {
        showConfirmationModal(function (confirmed) {
            if (confirmed) {
                anularOcupacion($('#id-ocupacion').val());
                calendar.refetchEvents();
            }
        });
    });


    $('#ccaa-ocupacion').on('change', function() {
        cargarProvincias('#provincia-ocupacion', $(this).val(), null)
        .done(function() {
            $('#provincia-ocupacion').change();
        });
    });

    $("#btnModalNuevaOcupacion").click(function (event) {
        $('#divEstadoOcupacion').hide();
        pickerFechaOcupacion.setDate('');
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
        mostrarOcultarBotonesModalOcupacion();
    });



    $('#provincia-ocupacion').on('change', function() {
        cargarMunicipios('#municipio-ocupacion',$(this).val(), null);
    });

    $('#solo-matinal-ocupacion').on('change', function() {
        if ($(this).is(':checked')) {
            $('#matinal-ocupacion').prop('checked', true);
        }
    });


   // Manejar el submit del formulario de tarifa anual
    $('#modalTarifaAnual form').on('submit', function(e) {
        e.preventDefault();

        generarTarifaAnualAjax();
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

                // Verificar si la respuesta es realmente un PDF
                const contentType = xhr.getResponseHeader('Content-Type');
                if (!contentType || (!contentType.includes('application/pdf') && !contentType.includes('application/octet-stream'))) {
                    notif('error', 'Error: La respuesta del servidor no es válida');
                    return;
                }

                // Obtener el nombre del archivo
                const contentDisposition = xhr.getResponseHeader('Content-Disposition');
                let filename = 'tarifa_anual_' + new Date().getTime() + '.pdf';

                if (contentDisposition) {
                    const matches = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
                    if (matches != null && matches[1]) {
                        filename = matches[1].replace(/['"]/g, '');
                    }
                }
                // Crear y descargar el archivo
                const blob = new Blob([data], { type: 'application/pdf' });

                // Para navegadores modernos
                const url = window.URL.createObjectURL(blob);

                // 1. Descarga automática
                const downloadLink = document.createElement('a');
                downloadLink.style.display = 'none';
                downloadLink.href = url;
                downloadLink.download = filename;
                document.body.appendChild(downloadLink);
                downloadLink.click();

                // 2. Mostrar notificación con opción de abrir
                const notification = notifDuration('success',
                    `Tarifa anual descargada
                    <br><button onclick="window.open('${url}', '_blank')" class="btn btn-sm btn-outline-light mt-2">
                        <i class="fas fa-eye"></i> Abrir PDF
                    </button>`,15000
                );

                // Cleanup después de un tiempo
                setTimeout(() => {
                    document.body.removeChild(downloadLink);
                    window.URL.revokeObjectURL(url);
                }, 5000);
                // Cerrar el modal y mostrar notificación de éxito
                $('#modalTarifaAnual').modal('hide');
            },
            error: function(xhr, status, error) {
                // Rehabilitar el botón
                $btn.prop('disabled', false).text(textoOriginal);

                console.error('Error al generar tarifa anual:', error);

                let errorMessage = 'Error al generar la tarifa anual';

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
                        errorMessage = 'Datos del formulario inválidos. Verifica provincia, año y ocupación.';
                        break;
                    case 403:
                        errorMessage = 'No tienes permisos para generar esta tarifa anual';
                        break;
                    case 500:
                        errorMessage = 'Error interno del servidor al generar la tarifa';
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
function validar_guardar_ocupacion(){
    if (document.getElementById('idFechaOcupacion').value ==''){
        notif('error','Selecciona la fecha de la ocupación');
        return false;
    }
    if (document.getElementById('municipio-ocupacion').value ==''){
        notif('error','Selecciona el municipio de la ocupación');
        return false;
    }
    return true;

}
function guardar_ocupacion() {

    if (validar_guardar_ocupacion()){
        let ocupacionSaveDto = crearOcupacionSaveDto();

        $("#btn-guardar-ocupacion").prop("disabled", true);

        sendOcupacionPost(ocupacionSaveDto);

        $('#modalNuevaOcupacion').modal('toggle');

    }


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

function crearOcupacionSaveDto() {
    let ocupacionSaveDto = {}

    ocupacionSaveDto["id"] = $("#id-ocupacion").val();
    ocupacionSaveDto["idArtista"] = $("#id-artista-modal-ocupacion").val();
    ocupacionSaveDto["fecha"] = moment($("#idFechaOcupacion").val(), "DD-MM-YYYY").format("YYYY-MM-DDTHH:mm:ss");
    ocupacionSaveDto["idTipoOcupacion"] = $("#tipos-ocupacion").val();
    ocupacionSaveDto["idCcaa"] = $("#ccaa-ocupacion").val();
    ocupacionSaveDto["idProvincia"] = $("#provincia-ocupacion").val();
    ocupacionSaveDto["idProvincia"] = $("#provincia-ocupacion").val();
    ocupacionSaveDto["idMunicipio"] = $("#municipio-ocupacion").val();
    ocupacionSaveDto["localidad"] = $("#localidad-ocupacion").val();
    ocupacionSaveDto["lugar"] = $("#lugar-ocupacion").val();
    ocupacionSaveDto["importe"] = $("#importe-ocupacion").val();
    ocupacionSaveDto["porcentajeRepre"] = $("#porcentaje-repre-ocupacion").val();
    ocupacionSaveDto["iva"] = $("#iva-ocupacion").val();
    ocupacionSaveDto["matinal"] = $('#matinal-ocupacion').is(':checked');
    ocupacionSaveDto["soloMatinal"] = $('#solo-matinal-ocupacion').is(':checked');
    ocupacionSaveDto["observaciones"] = $("#observaciones-ocupacion").val();


    return ocupacionSaveDto;
}

function sendOcupacionPost(ocupacionSaveDto){
    $.ajax({
        type: "POST",
        contentType: "application/json; charset=utf-8",
        url: "/ocupacion/save",
        data: JSON.stringify(ocupacionSaveDto),
        dataType: 'json',
        cache: false,
        timeout: 600000,
        async: false,
        success: function (data) {
            $("#btn-guardar-ocupacion").prop("disabled", false);
            if (data.success){
                notif("success", data.message);
            }
            else {
                notif("error", data.message);
            }
        },
        error: function (e) {
            $("#btn-guardar-ocupacion").prop("disabled", false);
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

function obtenerOcupacionDto(idOcupacion){

    $.ajax({
        url: '/ocupacion/get/'+idOcupacion,
        method: 'GET',
        dataType: 'json',
        success: function(ocupacionDto) {

            $("#id-ocupacion").val(ocupacionDto.id);
            $("#id-artista-modal-ocupacion").val(ocupacionDto.idArtista);
            $("#tipos-ocupacion").val(ocupacionDto.idTipoOcupacion);
            $("#ccaa-ocupacion").val(ocupacionDto.idCcaa);

            cargarProvincias('#provincia-ocupacion', ocupacionDto.idCcaa, ocupacionDto.idProvincia);

            cargarMunicipios('#municipio-ocupacion',ocupacionDto.idProvincia, ocupacionDto.idMunicipio);

            $("#localidad-ocupacion").val(ocupacionDto.localidad);
            $("#lugar-ocupacion").val(ocupacionDto.lugar);
            $("#importe-ocupacion").val(ocupacionDto.importe);
            $("#porcentaje-repre-ocupacion").val(ocupacionDto.porcentajeRepre);
            $("#iva-ocupacion").val(ocupacionDto.iva);
            $('#matinal-ocupacion').prop('checked', ocupacionDto.matinal);
            $('#solo-matinal-ocupacion').prop('checked', ocupacionDto.soloMatinal);
            $("#observaciones-ocupacion").val(ocupacionDto.observaciones);

            actualizarBadgeEstado(ocupacionDto.estado);

            $('#divEstadoOcupacion').show();
            mostrarOcultarBotonesModalOcupacion(ocupacionDto.estado);
            $('#modalNuevaOcupacion').modal('toggle');
        },
        error: function(error) {
            console.error('Error al obtener la lista de objetos:', error);
        }
    });

}

function actualizarBadgeEstado(estado) {
    // Mapeo de estados a clases de fondo
    const estadoMap = {
        'Pendiente': 'bg-secondary',   // Azul
        'Ocupado': 'bg-success',     // Verde
        'Reservado': 'bg-warning',   // Amarillo
        'Anulado': 'bg-danger'       // Rojo
    };

    // Obtener la clase de fondo correspondiente al estado
    const bgClass = estadoMap[estado] || 'bg-secondary'; // Clase por defecto si el estado no está mapeado

    // Seleccionar el badge por su ID
    const $badge = $('#badge-estado-ocupacion');

    if ($badge.length === 0) {
        // Si el badge no existe, crearlo y añadirlo al contenedor
        const badgeHtml = `<a href="#" id="badge-estado-ocupacion" class="badge ${bgClass} me-1 my-1">${estado}</a>`;
        // Añadir el badge a un contenedor específico, por ejemplo, un div con id 'badge-container'
        $('#badge-container').append(badgeHtml);
    } else {
        // Si el badge existe, actualizar sus clases y texto

        // Remover todas las clases que empiezan con 'bg-'
        $badge.removeClass(function(index, className) {
            return (className.match(/(^|\s)bg-\S+/g) || []).join(' ');
        });

        // Añadir la nueva clase de fondo
        $badge.addClass(bgClass);

        // Actualizar el texto del badge
        $badge.text(estado);
    }



}

    // Al mostrar la modal
    function mostrarOcultarBotonesModalOcupacion(estado) {
        const $ocupacionInput = $('#id-ocupacion');
        const $btnAnular = $('#btn-anular-ocupacion');
        const $btnConfirmar = $('#btn-confirmar-ocupacion');

        // Verificar el valor del input al mostrar la modal
        if ($ocupacionInput.val().trim() !== "") {
            $btnAnular.show();
            $btnConfirmar.show();
        } else {
            $btnAnular.hide();
            $btnConfirmar.hide();
        }

        if (estado!=null && estado!='Ocupado'){
            $btnConfirmar.show();
        }
        else {
            $btnConfirmar.hide();
        }


    }
