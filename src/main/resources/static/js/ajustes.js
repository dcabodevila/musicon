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

    const selectAjustes = document.getElementById('selectAjustes');
    const btnNuevoAjuste = document.getElementById('btnNuevoAjuste');
    const formAjustes = document.getElementById('formAjustes');

    // Preseleccionar el ajuste actual si existe
    const idActual = formAjustes.querySelector('input[name="id"]').value;
    if (idActual) {
        selectAjustes.value = idActual;
    }

    // Cargar ajuste al cambiar el select
    selectAjustes.addEventListener('change', function() {
        if (this.value) {
            window.location.href = '/ajustes/' + this.value;
        }
    });

    // Crear nuevo ajuste
    btnNuevoAjuste.addEventListener('click', function() {
        window.location.href = '/ajustes/nuevo';
    });

});

