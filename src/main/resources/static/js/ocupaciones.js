$(document).ready(function(){
    inicializarForm();

});


function inicializarForm(){
    let estado = $('#badge-estado-ocupacion').text();

    actualizarBadgeEstado(estado);

    flatpickr("#idFechaOcupacion", {
        disableMobile: true,
        locale: "es",
        altInput: true,
        altFormat: "j F, Y",
        dateFormat: "d-m-Y",
        allowInput: false
    });



    $("#formNuevaOcupacion").submit(function (event) {
        event.preventDefault();
        guardar_ocupacion();
    });

    $("#btn-confirmar-ocupacion").click(function (event) {
        showConfirmationModal(function (confirmed) {
            if (confirmed) {
                confirmarOcupacion($('#id-ocupacion').val());
                location.reload();
            }
        });


    });

    $("#btn-anular-ocupacion").click(function (event) {
        showConfirmationModal(function (confirmed) {
            if (confirmed) {
                anularOcupacion($('#id-ocupacion').val());
                location.reload();
            }
        });
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
    obtenerOcupacionDto($('#id-ocupacion').val());

}
