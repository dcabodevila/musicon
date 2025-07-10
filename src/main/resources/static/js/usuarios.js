$(document).ready(function(){

    $('#datatables-reponsive_usuarios').DataTable( {
        responsive: true,
        searching: true,
        ordering:  true,
        paging: true,
        language: {
            url: "https://cdn.datatables.net/plug-ins/1.13.1/i18n/es-ES.json"
        }
    } );

});

