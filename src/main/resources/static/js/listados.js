$(document).ready(function(){
    let pickerFechaHasta = flatpickr("#idFechaHastaListado", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
            ,
            onChange: function(selectedDates, dateStr, instance) {
//                pickerFechaDesde.set("maxDate", dateStr);
            }

    });
    let pickerFechaDesde = flatpickr("#idFechaDesdeListado", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            pickerFechaHasta.set("minDate", dateStr);
        },
        onMonthChange: function(selectedDates, dateStr, instance) {

            pickerFechaHasta.changeMonth(instance.currentMonth);
        }
    });



    let pickerFecha1 = flatpickr("#idFecha1", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
    });
    let pickerFecha2 = flatpickr("#idFecha2", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
    });
    let pickerFecha3 = flatpickr("#idFecha3", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
    });
    let pickerFecha4 = flatpickr("#idFecha4", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
    });
    let pickerFecha5 = flatpickr("#idFecha5", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
    });
    let pickerFecha6 = flatpickr("#idFecha6", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
    });
    let pickerFecha7 = flatpickr("#idFecha7", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
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



