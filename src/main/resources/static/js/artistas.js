$(document).ready(function(){

    let calendarEl = document.getElementById('calendar');
    let idArtista = document.getElementById('idArtista').value;
    let calendar = new FullCalendar.Calendar(calendarEl, {
      initialView: 'dayGridMonth',
      locale: 'es',
      timeZone: 'UTC',
      height:'auto',
      headerToolbar: {
          start: 'title',
          center: '',
          end: 'prev,next'
      },
      firstDay : 1,
      events: "/tarifa/list/"+idArtista,
//      eventDataTransform: function(event) {
//        event.title = event.title.toLocaleString('es-ES');
//        return event;
//      }
//      ,
//      eventClick: function(info) {
//        alert('Event: ' + info.event.id);
//        alert('Event: ' + info.event.title);
//
//        // change the border color just for fun
//        info.el.style.borderColor = 'red';
//      },
//      dateClick: function(info) {
//        alert('Clicked on: ' + info.dateStr);
//        // change the day's background color just for fun
//        info.dayEl.style.backgroundColor = 'red';
//      }
    });
    calendar.render();


});
