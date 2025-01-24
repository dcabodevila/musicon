$(document).ready(function(){
    $(".confirmar-ocupacion").click(function (event) {
        let idOcupacion = $(this).data("idocupacion");
        showConfirmationModal(function (confirmed) {
            if (confirmed) {
                confirmarOcupacion(idOcupacion);
                window.location.href = "/";
            }
        });


    });

    $(".anular-ocupacion").click(function (event) {
        let idOcupacion = $(this).data("idocupacion");
        showConfirmationModal(function (confirmed) {
            if (confirmed) {
                anularOcupacion(idOcupacion);
                window.location.href = "/";

            }
        });
    });

});


