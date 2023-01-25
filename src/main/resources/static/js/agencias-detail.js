$(document).ready(function(){
    flatpickr.localize(flatpickr.l10ns.es);
    flatpickr("#date1", {"locale": "es",
        locale: {
            firstDayOfWeek: 1,
            weekdays: {
                      shorthand: ['Do', 'Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sa'],
                      longhand: ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'],
                    },
                    months: {
                      shorthand: ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Оct', 'Nov', 'Dic'],
                      longhand: ['Enero', 'Febrero', 'Мarzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'],
                    }
        },
        dateFormat: "d-m-Y"
        });

    new Choices(document.querySelector("#usuario"));
    new Choices(document.querySelector("#provincia"));

});
