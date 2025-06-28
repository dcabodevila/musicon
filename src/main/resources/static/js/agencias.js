$(document).ready(function(){

    $('#datatables-reponsive').DataTable( {
        responsive: true,
        searching: false,
        ordering:  true,
        paging: false,
        language: {
            url: "https://cdn.datatables.net/plug-ins/1.13.1/i18n/es-ES.json"
        }
    } );

});
