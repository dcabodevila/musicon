// Variable global para mantener referencia al gráfico
let currentChart = null;
let chartInitialized = false;

$(document).ready(function () {

    // Inicializar DataTable con AJAX
    $('#datatables-reponsive_listados-generados').DataTable({
        responsive: true,
        searching: false,
        ordering: false,
        paging: true,
        processing: true,
        serverSide: true,
        ajax: {
            url: '/listado/audiencias/data',
            type: 'POST',
            data: function(d) {
                // Añadir parámetros del formulario de filtros
                d.idAgencia = $('#agencia').val();
                d.fechaDesde = $('#idFechaDesde').val();
                d.fechaHasta = $('#idFechaHasta').val();
                return d;
            },
            error: function(xhr, error, code) {
                console.error('Error en la carga de datos:', error);
            }
        },
        columns: [
            {
                data: 'fechaCreacion',
                render: function(data, type, row) {
                    if (data && type === 'display') {
                        return new Date(data).toLocaleDateString('es-ES');
                    }
                    return data || '-';
                }
            },
            { data: 'nombreRepresentante' },
            { data: 'solicitadoPara' },
            { data: 'municipio' },
            { data: 'localidad' },
            {
                data: null,
                render: function(data, type, row) {
                    if (row.fechaInicio && row.fechaFin) {
                        return new Date(row.fechaInicio).toLocaleDateString('es-ES') + ' a ' +
                               new Date(row.fechaFin).toLocaleDateString('es-ES');
                    } else {
                        let fechas = [];
                        for (let i = 1; i <= 7; i++) {
                            let fechaProp = row['fechaPropuesta' + i];
                            if (fechaProp) {
                                fechas.push(new Date(fechaProp).toLocaleDateString('es-ES'));
                            }
                        }
                        return fechas.join(' - ');
                    }
                }
            }
        ],
        language: {
            url: "https://cdn.datatables.net/plug-ins/1.13.1/i18n/es-ES.json"
        },
        columnDefs: [
            {
                targets: 0,
                type: 'date-eu'
            }
        ],
        order: [
          [0, 'desc']
        ]
    });

// Configurar flatpickr
    let pickerFechaHasta = flatpickr("#idFechaHasta", {
        disableMobile: true, 
        "locale": "es", 
        altInput: true, 
        altFormat: "j F, Y",
        dateFormat: "d-m-Y",  
        allowInput: false
    });

    let pickerFechaDesde = flatpickr("#idFechaDesde", {
        disableMobile: true, 
        "locale": "es", 
        altInput: true, 
        altFormat: "j F, Y",
        dateFormat: "d-m-Y",  
        allowInput: false
    });

    // Esperar a que AdminKit esté completamente cargado
    setTimeout(function() {
        $('#formListadoAudiencias').trigger('submit');
    }, 500);

    // Interceptar el envío del formulario para recargar la tabla
    $('#formListadoAudiencias').on('submit', function(e) {
        e.preventDefault();
        
        // Recargar la tabla con los nuevos filtros
        $('#datatables-reponsive_listados-generados').DataTable().ajax.reload();
        
        // Actualizar el gráfico con los nuevos filtros
        updateChartWithFilters();
        
        return false;
    });
});

    // Función para actualizar el gráfico con filtros
    function updateChartWithFilters() {
        // Obtener valores de los filtros
        const idAgencia = $('#agencia').val();
        const fechaDesde = $('#idFechaDesde').val();
        const fechaHasta = $('#idFechaHasta').val();
        
        // Realizar petición AJAX para obtener nuevos datos del gráfico
        $.ajax({
            url: '/listado/audiencias/chart-data',
            type: 'POST',
            data: {
                idAgencia: idAgencia,
                fechaDesde: fechaDesde,
                fechaHasta: fechaHasta
            },
            success: function(response) {
                if (response.success && response.chartData) {
                    // Actualizar el elemento hidden con los nuevos datos
                    $('#chartData').val(JSON.stringify(response.chartData));
                    
                    // Reinicializar el gráfico con los nuevos datos
                    reinitializeChart();
                } else {
                    console.error('Error en la respuesta del gráfico:', response.error);
                }
            },
            error: function(xhr, status, error) {
                console.error('Error actualizando datos del gráfico:', error);
            }
        });
    }

// Función separada para el gráfico
function initializeChart() {
    // Evitar múltiples inicializaciones
    if (chartInitialized) {
        return;
    }

    // Verificar que Chart.js esté disponible
    if (typeof Chart === 'undefined') {
        console.error('Chart.js no está disponible');
        setTimeout(initializeChart, 1000); // Reintentar en 1 segundo
        return;
    }

    // Destruir gráfico existente si existe
    if (currentChart) {

        currentChart.destroy();
        currentChart = null;
    }

    // Obtener datos del servidor
    let chartDataElement = document.getElementById('chartData');
    let chartData = [];
    
    try {
        if (chartDataElement && chartDataElement.value) {
            let rawData = chartDataElement.value;

            // Parsear los datos
            chartData = JSON.parse(rawData);

            // Verificar si es un array, si no, convertirlo
            if (!Array.isArray(chartData)) {
                chartData = [chartData];
            }
        }
    } catch (e) {
        console.error('Error parsing chart data:', e);
        chartData = [];
    }
    
    // Verificar si hay datos válidos
    if (!chartData || chartData.length === 0) {

        chartData = [{
            mes: "Sin datos",
            cantidad: 0
        }];
    }



    // Verificar que el canvas existe
    let canvas = document.getElementById('listadosChart');
    if (!canvas) {
        console.error('Canvas listadosChart no encontrado');
        return;
    }

    // Siempre mostrar el gráfico (ya sea con datos reales o vacíos)
    toggleChartVisibility(true);

    // Preparar datos para Chart.js estilo AdminKit
    var labels = chartData.map(item => item.mes);
    var data = chartData.map(item => item.cantidad);


    try {
        // Crear gráfico con estilo AdminKit.io
        currentChart = new Chart(canvas, {
            type: "bar",
            data: {
                labels: labels,
                datasets: [{
                    label: "Número de listados",
                    backgroundColor: window.theme.primary,
                    borderColor: window.theme.primary,
                    borderWidth: 1,
                    maxBarThickness: 50,
                    data: data
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: true,
                        position: 'bottom'
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                if (context.parsed.y === 0 && context.label === 'Sin datos') {
                                    return 'No hay listados en el período seleccionado';
                                }
                                return context.dataset.label + ': ' + context.parsed.y + ' listados';
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        min: 0,
                        grid: {
                            display: true
                        },
                        ticks: {
                            stepSize: 1,
                            callback: function(value) {
                                return Number.isInteger(value) ? value : '';
                            }
                        }
                    },
                    x: {
                        grid: {
                            display: false
                        }
                    }
                }
            }
        });

        chartInitialized = true;

    } catch (error) {
        console.error('Error creando el gráfico:', error);
        toggleChartVisibility(false);
    }

}

// Función para reinicializar el gráfico (útil después de actualizaciones AJAX)
function reinitializeChart() {

        
        // Marcar como no inicializado para forzar recreación
        chartInitialized = false;
        
        // Destruir gráfico existente si existe
        if (currentChart) {
            currentChart.destroy();
            currentChart = null;
        }
        
        // Inicializar con los nuevos datos
        initializeChart();
    }

    // Función para mostrar/ocultar el gráfico según haya datos
    function toggleChartVisibility(hasData) {
        const canvas = document.getElementById('listadosChart');
        const container = canvas ? canvas.closest('.chart-container') : null;
        
        if (container) {
            if (hasData) {
                container.style.display = 'block';
            } else {
                container.style.display = 'none';
            }
        }
    }