$(document).ready(function(){
    let pickerFechaDesde = flatpickr("#idFechaDesde", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: true
        ,
        onChange: function(selectedDates, dateStr, instance) {
//            pickerFechaHasta.set("minDate", dateStr);
        },
        onMonthChange: function(selectedDates, dateStr, instance) {

//            pickerFechaHasta.changeMonth(instance.currentMonth);
        }
    });

    let pickerFechaHasta = flatpickr("#idFechaHasta", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: true
            ,
            onChange: function(selectedDates, dateStr, instance) {
//                pickerFechaDesde.set("maxDate", dateStr);
            }

    });

    let calendarEl = document.getElementById('calendar');
    let idArtista = document.getElementById('idArtista').value;
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
      events: "/tarifa/list/"+idArtista,
      eventClick: function(info) {
        $("#id-tarifa").val(info.event.id);
        $("#modal-tarifa-eliminar").show();
        pickerFechaDesde.setDate(moment(info.event.start).format('DD-MM-YYYY'));
        pickerFechaHasta.setDate(moment(info.event.start).format('DD-MM-YYYY'));
        $("#importe").val(info.event.title);
        $('#modalNuevaTarifa').modal('show');
      },
      dateClick: function(info) {
        $("#id-tarifa").val("");
        $("#modal-tarifa-eliminar").hide();
        pickerFechaDesde.setDate(moment(info.date).format('DD-MM-YYYY'));
        pickerFechaHasta.setDate(moment(info.date).format('DD-MM-YYYY'));
        $('#modalNuevaTarifa').modal('show');
      }
    });
    calendar.render();

    $("#modalNuevaTarifa").submit(function (event) {

        event.preventDefault();
        guardar_tarifas();
        calendar.refetchEvents();

    });

    $("#modal-tarifa-eliminar").click(function (event) {

        eliminar_tarifas();
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
function guardar_tarifas() {

    let tarifaSaveDto = crearTarifaSaveDto();

    $("#btn-guardar-tarifa").prop("disabled", true);

    sendTarifaPost(tarifaSaveDto);
    $('#modalNuevaTarifa').modal('toggle');

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


