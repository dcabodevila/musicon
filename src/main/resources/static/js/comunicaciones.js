$(document).ready(function(){

    const MAX_SELECCION = 100;

    // Inicializar DataTable para la tabla de comunicaciones
    var table = $('#datatables-comunicaciones').DataTable({
        responsive: true,
        searching: true,
        ordering: true,
        paging: true,
        language: {
            url: "https://cdn.datatables.net/plug-ins/1.13.1/i18n/es-ES.json"
        },
        columnDefs: [{
            orderable: false,
            targets: 0 // Deshabilitar ordenamiento en la columna de checkboxes
        }],
        order: [
            [1, 'asc'] // Ordenar por nombre por defecto
        ]
    });

    // === CCAA → Provincia AJAX ===
    $('#ccaaId').change(function() {
        var idCcaa = $(this).val();
        var $provinciaSelect = $('#provinciaId');
        
        if (idCcaa) {
            // Limpiar y mostrar mensaje de carga
            $provinciaSelect.empty();
            $provinciaSelect.append('<option value="">Cargando provincias...</option>');
            
            // Obtener el token CSRF
            var token = $('meta[name="_csrf"]').attr('content');
            var header = $('meta[name="_csrf_header"]').attr('content');
            
            // Llamada AJAX para obtener provincias
            $.ajax({
                url: '/localizacion/provincias/' + idCcaa,
                type: 'GET',
                beforeSend: function(xhr) {
                    if (token && header) {
                        xhr.setRequestHeader(header, token);
                    }
                },
                success: function(provincias) {
                    $provinciaSelect.empty();
                    $provinciaSelect.append('<option value="">Todas las provincias</option>');
                    
                    if (provincias && provincias.length > 0) {
                        $.each(provincias, function(index, provincia) {
                            $provinciaSelect.append(
                                $('<option></option>')
                                    .attr('value', provincia.id)
                                    .text(provincia.nombre)
                            );
                        });
                    }
                },
                error: function(xhr, status, error) {
                    console.error('Error al cargar provincias:', error);
                    $provinciaSelect.empty();
                    $provinciaSelect.append('<option value="">Error al cargar provincias</option>');
                }
            });
        } else {
            // Si no hay CCAA seleccionada, resetear provincias
            $provinciaSelect.empty();
            $provinciaSelect.append('<option value="">Seleccione CCAA primero</option>');
        }
    });

    // === Select All Checkbox ===
    $('#selectAll').change(function() {
        var isChecked = $(this).is(':checked');
        var $checkboxes = $('.user-checkbox');

        if (isChecked) {
            var total = $checkboxes.length;
            if (total > MAX_SELECCION) {
                alert('Solo puedes seleccionar un máximo de ' + MAX_SELECCION + ' usuarios. Se marcarán los primeros ' + MAX_SELECCION + '.');
                $checkboxes.prop('checked', false);
                $checkboxes.slice(0, MAX_SELECCION).prop('checked', true);
            } else {
                $checkboxes.prop('checked', true);
            }
        } else {
            $checkboxes.prop('checked', false);
        }
        updateRedactarButton();
    });

    // === Individual checkbox change ===
    $(document).on('change', '.user-checkbox', function() {
        var selectedCount = $('.user-checkbox:checked').length;

        if ($(this).is(':checked') && selectedCount > MAX_SELECCION) {
            $(this).prop('checked', false);
            alert('Solo puedes seleccionar un máximo de ' + MAX_SELECCION + ' usuarios.');
            return false;
        }

        updateRedactarButton();

        // Actualizar estado de "select all" si algún checkbox individual cambia
        var totalCheckboxes = $('.user-checkbox').length;
        var checkedCheckboxes = $('.user-checkbox:checked').length;

        if (checkedCheckboxes === 0) {
            $('#selectAll').prop('checked', false).prop('indeterminate', false);
        } else if (checkedCheckboxes === totalCheckboxes) {
            $('#selectAll').prop('checked', true).prop('indeterminate', false);
        } else {
            $('#selectAll').prop('checked', false).prop('indeterminate', true);
        }
    });

    // === Actualizar estado del botón "Redactar mensaje" ===
    function updateRedactarButton() {
        var selectedCount = $('.user-checkbox:checked').length;
        $('#btnRedactar').prop('disabled', selectedCount === 0);

        // Actualizar texto del botón con conteo
        if (selectedCount > 0) {
            $('#btnRedactar').html('<i class="fas fa-envelope me-2"></i>Redactar mensaje (' + selectedCount + '/' + MAX_SELECCION + ')');
        } else {
            $('#btnRedactar').html('<i class="fas fa-envelope me-2"></i>Redactar mensaje');
        }
    }

    // === Validación del formulario antes de enviar a redactar ===
    $('#usuariosForm').submit(function(e) {
        var selectedCount = $('.user-checkbox:checked').length;

        if (selectedCount === 0) {
            e.preventDefault();
            alert('Por favor, selecciona al menos un usuario para continuar.');
            return false;
        }

        if (selectedCount > MAX_SELECCION) {
            e.preventDefault();
            alert('No puedes enviar a más de ' + MAX_SELECCION + ' usuarios a la vez. Seleccionados: ' + selectedCount);
            return false;
        }

        return true;
    });

    // Inicializar estado del botón al cargar la página
    updateRedactarButton();

});
