$(document).ready(function(){

        // Verificar si ya existe una instancia antes de crear una nueva
        if (!$('#municipio-ocupacion').data('choicesInstance')) {
            const municipioChoice = new Choices('#municipio-ocupacion');
            $('#municipio-ocupacion').data('choicesInstance', municipioChoice);
        }

        if (!$('#usuario-ocupacion').data('choicesInstance')) {
            const usuarioChoice = new Choices('#usuario-ocupacion');
            $('#usuario-ocupacion').data('choicesInstance', usuarioChoice);
        }


        $('#ccaa-ocupacion').on('change', function() {
            cargarProvincias('#provincia-ocupacion', $(this).val(), null)
                .done(function() {
                    $('#provincia-ocupacion').change();
                });
        });


        $('#provincia-ocupacion').on('change', function() {
            cargarMunicipios('#municipio-ocupacion', $(this).val(), null);
        });

        $('#municipio-ocupacion').on('change', function() {
            cargarLocalidades('#localidad-ocupacion', $(this).val(), null);
            $('#localidad-ocupacion').val('');
        });

        $('#solo-matinal-ocupacion').on('change', function() {
            if ($(this).is(':checked')) {
                $('#matinal-ocupacion').prop('checked', true);
            }
        });

        $('#matinal-ocupacion').on('change', function() {
            if (!$(this).is(':checked')) {
                $('#solo-matinal-ocupacion').prop('checked', false);
            }
        });

        $('#provisional-ocupacion').change(function() {
            if ($(this).is(':checked')) {
                // Seleccionar la CCAA con valor 20 y disparar el evento change
                $('#ccaa-ocupacion').val(20).trigger('change');
                $('#localidad-ocupacion').val('Provisional');
                // Esperar a que se carguen los municipios
                setTimeout(function() {
                    // Actualizar el Choice de municipios
                    $('#municipio-ocupacion').data('choicesInstance').setChoiceByValue(8117);
                }, 500);
            }
        });

        $(document).on('click', '#btn-publicar-orquestas', function(e) {
            e.preventDefault();
            publicarEnOrquestasDeGalicia();
        });
        $(document).on('click', '#btn-actualizar-orquestas', function(e) {
            e.preventDefault();
            actualizarEnOrquestasDeGalicia();
        });

        $(document).on('click', '#btn-eliminar-orquestas', function(e) {
            e.preventDefault();
            eliminarDeOrquestasDeGalicia();
        });

});


function guardar_ocupacion() {

    if (validar_guardar_ocupacion()){
        let ocupacionSaveDto = crearOcupacionSaveDto();

        $("#btn-guardar-ocupacion").prop("disabled", true);

        let response = sendOcupacionPost(ocupacionSaveDto);

        $('#modalNuevaOcupacion').modal('toggle');

        return response;

    }
    else {
        return $.Deferred().reject("Error en validación").promise();
    }

}

function sendOcupacionPost(ocupacionSaveDto){
    return $.ajax({
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
                obtenerOcupacionDto(data.idEntidad);
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

function validar_guardar_ocupacion(){
    if (document.getElementById('idFechaOcupacion').value ==''){
        notif('error','Selecciona la fecha de la ocupación');
        return false;
    }

    return true;

}

function crearOcupacionSaveDto() {
    let ocupacionSaveDto = {}

    ocupacionSaveDto["id"] = $("#id-ocupacion").val();
    ocupacionSaveDto["idArtista"] = $("#id-artista-modal-ocupacion").val();
    ocupacionSaveDto["fecha"] = moment($("#idFechaOcupacion").val(), "DD-MM-YYYY").format("YYYY-MM-DDTHH:mm:ss");
    ocupacionSaveDto["idTipoOcupacion"] = $("#tipos-ocupacion").val();
    ocupacionSaveDto["idCcaa"] = $("#ccaa-ocupacion").val();
    ocupacionSaveDto["idProvincia"] = $("#provincia-ocupacion").val();
    ocupacionSaveDto["idMunicipio"] = $("#municipio-ocupacion").val();
    ocupacionSaveDto["localidad"] = $("#localidad-ocupacion").val();
    ocupacionSaveDto["lugar"] = $("#lugar-ocupacion").val();
    ocupacionSaveDto["importe"] = $("#importe-ocupacion").val() || "0";
    ocupacionSaveDto["porcentajeRepre"] = $("#porcentaje-repre-ocupacion").val() || "0";
    ocupacionSaveDto["iva"] = $("#iva-ocupacion").val() || "0";
    ocupacionSaveDto["matinal"] = $('#matinal-ocupacion').is(':checked');
    ocupacionSaveDto["soloMatinal"] = $('#solo-matinal-ocupacion').is(':checked');
    ocupacionSaveDto["observaciones"] = $("#observaciones-ocupacion").val();
    ocupacionSaveDto["provisional"] = $("#provisional-ocupacion").val();
    ocupacionSaveDto["textoOrquestasDeGalicia"] = $("#orquestasdegalicia-ocupacion").val();
    ocupacionSaveDto["idUsuario"] = $("#usuario-ocupacion").data('choicesInstance').getValue(true);

    return ocupacionSaveDto;
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

function obtenerOcupacionDto(idOcupacion) {

    if (idOcupacion){

        $.ajax({
            url: '/ocupacion/get/' + idOcupacion,
            method: 'GET',
            dataType: 'json',
            success: function(ocupacionDto) {
                // Establecer valores básicos
                $("#id-ocupacion").val(ocupacionDto.id);
                $("#id-artista-modal-ocupacion").val(ocupacionDto.idArtista);
                $("#tipos-ocupacion").val(ocupacionDto.idTipoOcupacion);

                // Establecer fecha
                if (ocupacionDto.start) {
                    $("#idFechaOcupacion").val(moment(ocupacionDto.start).format('DD-MM-YYYY'));
                }
                $("#provisional-ocupacion").val(ocupacionDto.provisional);

                // Gestionar ubicación
                $("#ccaa-ocupacion").val(ocupacionDto.idCcaa);
                cargarProvincias('#provincia-ocupacion', ocupacionDto.idCcaa, ocupacionDto.idProvincia)
                    .done(function() {
                        cargarMunicipios('#municipio-ocupacion', ocupacionDto.idProvincia, ocupacionDto.idMunicipio).done(function() {
                            cargarLocalidades('#localidad-ocupacion', ocupacionDto.idCcaa, ocupacionDto.localidad);
                        });

                    });

                // Datos de localización
                $("#localidad-ocupacion").val(ocupacionDto.localidad);
                $("#lugar-ocupacion").val(ocupacionDto.lugar);

                // Datos económicos
                $("#importe-ocupacion").val(ocupacionDto.importe || 0);
                $("#porcentaje-repre-ocupacion").val(ocupacionDto.porcentajeRepre || 0);
                $("#iva-ocupacion").val(ocupacionDto.iva || 0);

                // Checkboxes
                $('#matinal-ocupacion').prop('checked', Boolean(ocupacionDto.matinal));
                $('#solo-matinal-ocupacion').prop('checked', Boolean(ocupacionDto.soloMatinal));

                // Datos adicionales
                $("#observaciones-ocupacion").val(ocupacionDto.observaciones);
                $("#orquestasdegalicia-ocupacion").val(ocupacionDto.textoOrquestasDeGalicia);

                $('#usuario-ocupacion').data('choicesInstance').setChoiceByValue(String(ocupacionDto.idUsuario));


                // Gestionar estado y UI
                if (ocupacionDto.estado) {
                    actualizarBadgeEstado(ocupacionDto.estado);
                    $('#divEstadoOcupacion').show();
                    mostrarOcultarBotonesModalOcupacion(ocupacionDto.estado);
                }

                if (ocupacionDto.isPublicadoOdg){
                    $('#btn-publicar-orquestas').hide();
                    $('#btn-actualizar-orquestas').show();
                    $('#btn-eliminar-orquestas').show();

                }
                else{
                    $('#btn-publicar-orquestas').show();
                    $('#btn-actualizar-orquestas').hide();
                    $('#btn-eliminar-orquestas').hide();
                }

            },
            error: function(xhr, status, error) {
                console.error('Error al obtener la ocupación:', error);
                notif('error', 'Error al cargar los datos de la ocupación');
            }
        });
    } else {
        mostrarOcultarBotonesModalOcupacion(null);
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

function publicarEnOrquestasDeGalicia() {
    // Obtener el ID de la ocupación
    const idOcupacion = $('#id-ocupacion').val();

    // Validar que exista el ID
    if (!idOcupacion) {

        notif('error', 'La ocupación debe ser guardada primero');
        return;
    }
    let promesa = guardar_ocupacion();
    if (promesa) {
        promesa.done(function(response) {
            if (response && response.success) {
                // Si el guardado fue exitoso, enviar la petición de publicación
                enviarPeticionPublicacion(idOcupacion);
            } else {
                notif('error', response ? response.message : 'Error al guardar el formulario');
            }
        }).fail(function(error) {
            notif('error', 'Error al guardar el formulario');
            console.error('Error:', error);
        });
    }
}

function actualizarEnOrquestasDeGalicia() {
    // Obtener el ID de la ocupación
    const idOcupacion = $('#id-ocupacion').val();

    // Validar que exista el ID
    if (!idOcupacion) {

        notif('error', 'La ocupación debe ser guardada primero');
        return;
    }
    let promesa = guardar_ocupacion();
    if (promesa) {
        promesa.done(function(response) {
            if (response && response.success) {
                // Si el guardado fue exitoso, enviar la petición de publicación
                enviarPeticionActualizarEnOrquestasDeGalicia(idOcupacion);
            } else {
                notif('error', response ? response.message : 'Error al guardar el formulario');
            }
        }).fail(function(error) {
            notif('error', 'Error al guardar el formulario');
            console.error('Error:', error);
        });
    }
}

function enviarPeticionPublicacion(idOcupacion) {

    $.ajax({
        url: '/ocupacion/publicar-odg/' + idOcupacion,
        type: 'POST',
        contentType: 'application/json',
        dataType: 'json',
        success: function(data) {
            if (data && data.success) {
                notif(data.messageType, data.message || 'Ocupación publicada correctamente en OrquestasDeGalicia');
            } else {
                notif(data.messageType, data ? data.message : 'Error al publicar la ocupación');
            }
        },
        error: function(xhr, status, error) {
            notif('error',  'Error al publicar la ocupación: ' + error);
        }
    });
}

function enviarPeticionActualizarEnOrquestasDeGalicia() {
    const idOcupacion = $("#id-ocupacion").val();

    if (!idOcupacion) {
        notif('error', 'Debe guardar la ocupación primero');
        return;
    }

    $.ajax({
        type: "POST",
        contentType: "application/json; charset=utf-8",
        url: "/ocupacion/actualizar-odg/" + idOcupacion,
        dataType: 'json',
        cache: false,
        timeout: 600000,
        success: function(data) {
            if (data.success) {
                notif("success", data.message);
            } else {
                notif("error", data.message);
            }
        },
        error: function(e) {
            console.log(e);
            notif('error', 'Error al actualizar en OrquestasDeGalicia');
        }
    });

}

function eliminarDeOrquestasDeGalicia() {
    const idOcupacion = $("#id-ocupacion").val();

    if (!idOcupacion) {
        notif('error', 'Debe guardar la ocupación primero');
        return;
    }


    $.ajax({
        type: "POST",
        contentType: "application/json; charset=utf-8",
        url: "/ocupacion/eliminar-odg/" + idOcupacion,
        dataType: 'json',
        cache: false,
        timeout: 600000,
        success: function(data) {
            if (data.success) {
                notif("success", data.message);
            } else {
                notif("error", data.message);
            }
            $('#modalNuevaOcupacion').modal('toggle');

        },
        error: function(e) {
            console.log(e);
            notif('error', 'Error al eliminar de OrquestasDeGalicia');
        }
    });

}



