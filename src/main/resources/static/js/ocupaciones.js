$(document).ready(function(){

    let pickerFechaOcupacion = flatpickr("#idFechaOcupacion", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: true
            ,
            onChange: function(selectedDates, dateStr, instance) {
//                pickerFechaDesde.set("maxDate", dateStr);
            }

    });


    $("#modalNuevaOcupacion").submit(function (event) {

        event.preventDefault();
        guardar_ocupacion();
        calendar.refetchEvents();

    });


});



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
function guardar_ocupacion() {

    let tarifaSaveDto = crearOcupacionSaveDto();

    $("#btn-guardar-ocupacion").prop("disabled", true);

    sendOcupacionPost(tarifaSaveDto);
    $('#modalNuevaOcupacion').modal('toggle');

}


function crearOcupacionSaveDto() {
    let ocupacionSaveDto = {}

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




