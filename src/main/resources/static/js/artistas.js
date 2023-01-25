$(document).ready(function(){

    function convertirTarifasParaFullCalendar(tarifas) {
    	let _tarifas = [],
    		formato = "YYYY-MM-DDTHH:mm:ss";

    	for(let tarifa of tarifas){

    		let nuevaTarifa = {
    			  id: tarifa.id,
    		      title  : tarifa.importe,
    		      start  : moment(tarifa.fechaInicial).format(formato),
    		      end    : moment(tarifa.fechaFinal).format(formato),
    		}
    		_tarifas.push(nuevaTarifa)
    	}
    	return _tarifas;
    }

    var CONTEXT_ROOT = '[[@{/}]]';
    var calendarEl = document.getElementById('calendar');
    var calendar = new FullCalendar.Calendar(calendarEl, {
      initialView: 'dayGridMonth',
      locale: 'es',
      headerToolbar: {
          start: 'title',
          center: '',
          end: 'prev,next'
      },
      firstDay : 1,
      events: "/tarifa/list/6",
//      events: (start, end, timezone, callback) => {
//        var csrfHeader = $("meta[name='_csrf_header']").attr("content");
//        var csrfToken = $("meta[name='_csrf']").attr("content");
//        $.ajaxSetup({
//            headers:
//                { 'X-CSRF-TOKEN': csrfToken }
//        });
//        $.ajax({
//            type: 'GET',
//            url: "/tarifa/list/6",
////            data: {
////                idEdificio: $('#idEdificio').val(),
////                fechaReservaIni: start.format("DD-MM-YYYY"),
////                fechaReservaFin: end.format("DD-MM-YYYY"),
////                tipoReserva: $('#tipoReserva').val(),
////                tipoSalaId : tipoSalaId,
////                salaId : salaId,
////                agrupacionMedio: agrupacionMedio,
////                medio : medio,
////                agrupacionTipoMedio : agrupacionTipoMedio,
////                isCalendarioDashboard: isCalendarioDashboard
////            },
//            datatype: "json",
//            success: (listaTarifas) => {
//                var tarifas = convertirTarifasParaFullCalendar(listaTarifas);
//                callback(tarifas);
//            }
//        })
//
//    },
//      eventClick: (reserva, allDay, jsEvent, view) => {
//
//          alert("eventClick!");
//
//    },
//      dayClick: (date, jsEvent, view) => {
//        alert("dayClick!");
//    }
    });
    calendar.render();


});
