

$(document).ready(function () {

    const usuariosSelect = document.querySelector('#usuarios-accesos');

    const choicesUsuarios = new Choices(usuariosSelect, {
            removeItemButton: true
    });

    // Evento cuando se muestra la modal
    $('#modalAccesos').on('shown.bs.modal', function () {
        let idRol = $('#rol-accesos').val(); // Obtiene el ID del rol seleccionado
        if (idRol) {
            cargarPermisos(idRol);
        }
    });

    // Evento cuando se cambia el rol seleccionado
    $('#rol-accesos').change(function () {
        let idRol = $(this).val();
        cargarPermisos(idRol);
    });

    $('.btnModalAcceso').click(function () {
        let idUsuario = $(this).data('idusuario'); // Obtiene el idUsuario del botón
        let idRol = $(this).data('idrol'); // Obtiene el idRol del botón
        let idAcceso = $(this).data('idacceso'); // Obtiene el idacceso del botón
        $('#id-acceso').val(idAcceso).trigger('change'); // Actualiza el select de rol
        // Asigna los valores a los selects en la modal
        choicesUsuarios.setChoiceByValue(idUsuario.toString());
        $('#rol-accesos').val(idRol).trigger('change'); // Actualiza el select de rol
        // Si existe un ID, mostrar botón de eliminar
        if (idAcceso) {
            $('#btn-eliminar-acceso').removeClass('d-none').attr('data-id', idAcceso);
        } else {
            $('#btn-eliminar-acceso').addClass('d-none');
        }


    });
    $('#btnNuevoAcceso').click(function () {
        $('#id-acceso').val(''); // Limpia el valor del campo hidden id-acceso
        $('#btn-eliminar-acceso').addClass('d-none');
    });

        // Manejar eliminación de acceso usando GET
    $('#btn-eliminar-acceso').click(function () {
        let idAcceso = $(this).attr('data-id');

        showConfirmationModal(function (confirmed) {
            if (confirmed) {
                eliminarAcceso(idAcceso);
            }
        });




    });

});

    function eliminarAcceso(idAcceso){

        $.ajax({
            url: '/accesos/eliminar/' + idAcceso,
            type: 'GET',
            success: function (response) {
                notif("success", response);
                $('#modalAccesos').modal('hide'); // Cierra la modal
                location.reload(); // Recargar la página para reflejar cambios
            },
            error: function () {
                notif("error", 'Error al eliminar el acceso');
            }
        });
    }

    // Función para cargar los permisos
    function cargarPermisos(idRol) {
        $.ajax({
            url: '/permisos/rol/' + idRol, // Llamada al método REST
            type: 'GET',
            success: function (data) {
                actualizarListaPermisos(data);
            },
            error: function () {
                console.error('Error al cargar los permisos.');
            }
        });
    }

    // Función para actualizar la lista de permisos en la UI
    function actualizarListaPermisos(permisos) {
        let permisosContainer = $('#lista-permisos');
        permisosContainer.empty(); // Limpia la lista antes de agregar nuevos permisos

        if (permisos.length > 0) {
            permisos.forEach(function (permiso) {
                permisosContainer.append(
                    `<li class="list-group-item">${permiso.descripcion}</li>`
                );
            });
        } else {
            permisosContainer.append('<li class="list-group-item text-muted">No hay permisos disponibles</li>');
        }
    }

function notif(type, message){
    let duration = "5000";
    let ripple = true;
    let dismissible = false;
    window.notyf.open({
        type,
        message,
        duration,
        ripple,
        dismissible,
        position: {
            x: "center",
            y: "top"
        }
    });
}

