$(document).ready(function(){
    let pickerFechaDesde = flatpickr("#idFechaDesdeListado", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            pickerFechaHasta.set("minDate", dateStr);
        },
        onMonthChange: function(selectedDates, dateStr, instance) {

            pickerFechaHasta.changeMonth(instance.currentMonth);
        }
    });

    let pickerFechaHasta = flatpickr("#idFechaHastaListado", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
            ,
            onChange: function(selectedDates, dateStr, instance) {
//                pickerFechaDesde.set("maxDate", dateStr);
            }

    });



    const municipioChoice = new Choices('#municipio-listado');
    $('#municipio-listado').data('choicesInstance', municipioChoice);


    $('#ccaa-listado').on('change', function() {
        cargarProvincias('#provincia-listado', $(this).val(), null)
        .done(function() {
            $('#provincia-listado').change();
        });
    });



    $('#provincia-listado').on('change', function() {
        cargarMunicipios('#municipio-listado',$(this).val(), null);
    });
});



