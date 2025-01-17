$(document).ready(function(){

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

