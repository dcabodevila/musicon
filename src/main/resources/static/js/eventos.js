$(document).ready(function(){
    let fechaHastaPicker = flatpickr("#idFechaHasta", {
        disableMobile: true,
        locale: "es",
        altInput: true,
        altFormat: "j F, Y",
        dateFormat: "d-m-Y",
        allowInput: false
    });
    let fechaDesdeePicker = flatpickr("#idFechaDesde", {
        disableMobile: true,
        locale: "es",
        altInput: true,
        altFormat: "j F, Y",
        dateFormat: "d-m-Y",
        allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            fechaHastaPicker.jumpToDate(new Date(instance.currentYear, instance.currentMonth, 1));
        }
    });


});
