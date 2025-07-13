

$(document).ready(function(){


    $('#datatables-reponsive_ocupaciones').DataTable( {
        responsive: true,
        searching: true,
        ordering:  true,
        paging: true,
        language: {
            url: "https://cdn.datatables.net/plug-ins/1.13.1/i18n/es-ES.json"
        },
            columnDefs: [
                {
                    targets: 1,
                    type: 'date-eu'
                }
            ],
            order: [
              [1, 'desc']  // Luego ordena por la tercera columna (índice 2)
            ]


    } );

        let pickerFechaDesde = flatpickr("#idFechaDesde", {
            disableMobile: true,
            "locale": "es",
            altInput: true,
            altFormat: "j F, Y",
            dateFormat: "d-m-Y",
            allowInput: false
        });



});




