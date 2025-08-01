// Variable global para mantener referencia al gráfico
let currentChart = null;
let chartInitialized = false;

$(document).ready(function () {

    // Inicializar DataTable
    $('#datatables-reponsive_listados-generados').DataTable({
        responsive: true,
        searching: true,
        ordering: true,
        paging: true,
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
        initializeChart();
    }, 500);
});

// Función separada para el gráfico
function initializeChart() {
    // Evitar múltiples inicializaciones
    if (chartInitialized) {
        console.log('Gráfico ya inicializado, saltando...');
        return;
    }

    console.log('Iniciando inicialización del gráfico...');

    // Verificar que Chart.js esté disponible
    if (typeof Chart === 'undefined') {
        console.error('Chart.js no está disponible');
        setTimeout(initializeChart, 1000); // Reintentar en 1 segundo
        return;
    }

    // Destruir gráfico existente si existe
    if (currentChart) {
        console.log('Destruyendo gráfico existente...');
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
        console.log('No hay datos para mostrar gráfico');
        let canvas = document.getElementById('listadosChart');
        if (canvas) {
            canvas.style.display = 'none';
        }
        chartInitialized = true;
        return;
    }

    console.log('Datos finales para gráfico:', chartData);

    // Verificar que el canvas existe
    let canvas = document.getElementById('listadosChart');
    if (!canvas) {
        console.error('Canvas listadosChart no encontrado');
        return;
    }

    // Mostrar el gráfico
    canvas.style.display = 'block';

    // Preparar datos para Chart.js estilo AdminKit
    var labels = chartData.map(item => item.mes);
    var data = chartData.map(item => item.cantidad);

    console.log('Labels:', labels);
    console.log('Data:', data);

    try {
        // Crear gráfico con estilo AdminKit.io
        currentChart = new Chart(canvas, {
            type: "line",
            data: {
                labels: labels,
                datasets: [{
                    label: "Número de impresiones",
                    backgroundColor: "transparent",
                    borderColor: window.theme.primary,
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
                            display: false
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
                            color: "transparent"
                        }
                    }
                }
            }
        });

        chartInitialized = true;

    } catch (error) {
        console.error('Error creando el gráfico:', error);
    }
}

// Función para reinicializar el gráfico (útil después de actualizaciones AJAX)
function reinitializeChart() {
    chartInitialized = false;
    initializeChart();
}