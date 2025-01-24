$(document).ready(function(){
    $("#btn-confirmar-ocupacion").click(function (event) {
        let idOcupacion = $(this).data("idocupacion");
        showConfirmationModal(function (confirmed) {
            if (confirmed) {
                confirmarOcupacion(idOcupacion);
                window.location.href = "/";
            }
        });


    });

    $("#btn-anular-ocupacion").click(function (event) {
        let idOcupacion = $(this).data("idocupacion");
        showConfirmationModal(function (confirmed) {
            if (confirmed) {
                anularOcupacion(idOcupacion);
                window.location.href = "/";

            }
        });
    });

});


