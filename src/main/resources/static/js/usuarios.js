$(document).ready(function(){

    $('#datatables-reponsive_usuarios').DataTable( {
        responsive: true,
        searching: true,
        ordering:  true,
        paging: true,
        language: {
            url: "https://cdn.datatables.net/plug-ins/1.13.1/i18n/es-ES.json"
        },
        order: [
          [1, 'desc'], // Primero ordena por la segunda columna (índice 1)
          [2, 'asc']  // Luego ordena por la tercera columna (índice 2)
        ]


    } );

});

