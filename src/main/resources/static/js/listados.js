$(document).ready(function(){
    let pickerFechaHasta = flatpickr("#idFechaHastaListado", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
    });
    let pickerFechaDesde = flatpickr("#idFechaDesdeListado", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onChange: function(selectedDates, dateStr, instance) {
            pickerFechaHasta.set("minDate", dateStr);
        },
        onMonthChange: function(selectedDates, dateStr, instance) {
            pickerFechaHasta.clear();
            pickerFechaHasta.changeMonth(instance.currentMonth);
        }
    });

    let pickerFecha7 = flatpickr("#idFecha7", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
    });
    let pickerFecha6 = flatpickr("#idFecha6", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onMonthChange: function(selectedDates, dateStr, instance) {
            pickerFecha7.clear();
            pickerFecha7.changeMonth(instance.currentMonth);
        }
    });
    let pickerFecha5 = flatpickr("#idFecha5", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onMonthChange: function(selectedDates, dateStr, instance) {
            pickerFecha6.clear();
            pickerFecha6.changeMonth(instance.currentMonth);
        }
    });
    let pickerFecha4 = flatpickr("#idFecha4", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onMonthChange: function(selectedDates, dateStr, instance) {
            pickerFecha5.clear();
            pickerFecha5.changeMonth(instance.currentMonth);
        }
    });
    let pickerFecha3 = flatpickr("#idFecha3", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onMonthChange: function(selectedDates, dateStr, instance) {
            pickerFecha4.clear();
            pickerFecha4.changeMonth(instance.currentMonth);
        }
    });
    let pickerFecha2 = flatpickr("#idFecha2", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onMonthChange: function(selectedDates, dateStr, instance) {
            pickerFecha3.clear();
            pickerFecha3.changeMonth(instance.currentMonth);
        }
    });
    let pickerFecha1 = flatpickr("#idFecha1", {disableMobile: true, "locale": "es", altInput: true, altFormat: "j F, Y",dateFormat: "d-m-Y",  allowInput: false
        ,
        onMonthChange: function(selectedDates, dateStr, instance) {
            pickerFecha2.clear();
            pickerFecha2.changeMonth(instance.currentMonth);
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

    const form = document.getElementById('formGenerarListado');
    form.addEventListener('submit', (event) => {
        const fechaDesde = document.getElementById('idFechaDesdeListado').value;
        const fechaHasta = document.getElementById('idFechaHastaListado').value;

        const fechasIndividuales = [
            document.getElementById('idFecha1').value,
            document.getElementById('idFecha2').value,
            document.getElementById('idFecha3').value,
            document.getElementById('idFecha4').value,
            document.getElementById('idFecha5').value,
            document.getElementById('idFecha6').value,
            document.getElementById('idFecha7').value
        ];

        // Validar que haya al menos un rango de fechas o una fecha individual
        const hayRangoDeFechas = fechaDesde !== '' && fechaHasta !== '';
        const hayFechasIndividuales = fechasIndividuales.some(fecha => fecha !== '');

        if (!hayRangoDeFechas && !hayFechasIndividuales) {
            event.preventDefault(); // Evita que se envíe el formulario
            notif('error','Introduce las fecha inicial y final o alguna fecha individual');
            return;
        }
        if ((fechaDesde !== '' || fechaHasta !== '') && hayFechasIndividuales) {
            event.preventDefault(); // Evita que se envíe el formulario
            notif('error','Si defines una fecha inicial o final, no puedes incluir fechas individuales. Por favor, corrige los campos.');
            return;
        }

        // Validar que no haya fechas duplicadas
        const todasLasFechas = [fechaDesde, fechaHasta, ...fechasIndividuales].filter(fecha => fecha !== '');
        const fechasUnicas = new Set(todasLasFechas);

        if (fechasUnicas.size !== todasLasFechas.length) {
            event.preventDefault(); // Evita que se envíe el formulario
            notif('error','Las fechas no pueden ser iguales. Por favor, corrige los campos.');
        }
    });
    const artistasSelect = document.querySelector('#tiposArtista');

    new Choices(artistasSelect, {
            removeItemButton: true
    });

    const agenciasSelect = document.querySelector('#agencias');

    new Choices(agenciasSelect, {
            removeItemButton: true
    });

    const ccaaSelect = document.querySelector('#ccaa');

    new Choices(ccaaSelect, {
            removeItemButton: true
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

function clearFlatpickrDate(inputId) {
    const input = document.getElementById(inputId);
    if (input && input._flatpickr) {
        input._flatpickr.clear(); // Limpia la fecha seleccionada
    }
}