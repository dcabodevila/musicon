let currentChartAccesos = null;
let chartInitializedAccesos = false;
let currentHeatmapChart = null;
let currentHeatmapRequest = null;
let artistHeatmapChoices = null;

const HEATMAP_COLOR_RANGES = [
    { from: 0, to: 0, name: '0', color: '#FFFFFF' },
    { from: 1, to: 1, name: '1', color: '#E8F5E9' },
    { from: 2, to: 3, name: '2-3', color: '#A5D6A7' },
    { from: 4, to: 7, name: '4-7', color: '#43A047' },
    { from: 8, to: 9999, name: '8+', color: '#1B5E20' }
];

const HEATMAP_MONTH_LABELS = {
    'Enero': 'Ene',
    'Febrero': 'Feb',
    'Marzo': 'Mar',
    'Abril': 'Abr',
    'Mayo': 'May',
    'Junio': 'Jun',
    'Julio': 'Jul',
    'Agosto': 'Ago',
    'Septiembre': 'Sept',
    'Octubre': 'Oct',
    'Noviembre': 'Nov',
    'Diciembre': 'Dic'
};

const HEATMAP_CHART_OPTIONS = {
    height: 420,
    strokeColor: '#d6e4da'
};

$(document).ready(function() {
    initializeAccessChartFilters();
    initializeHeatmap();
    initializeTables();
});

function initializeAccessChartFilters() {
    new Choices(document.querySelector("#usuarioSelectGrafico"));

    flatpickr("#fechaHastaGrafico", {
        disableMobile: true,
        locale: "es",
        altInput: true,
        altFormat: "j F, Y",
        dateFormat: "d-m-Y",
        allowInput: false,
        defaultDate: "today"
    });

    flatpickr("#fechaDesdeGrafico", {
        disableMobile: true,
        locale: "es",
        altInput: true,
        altFormat: "j F, Y",
        dateFormat: "d-m-Y",
        allowInput: false
    });

    $('#formFiltroAccesos').on('submit', function(e) {
        e.preventDefault();
        actualizarGraficoAccesos();
        return false;
    });
}

function initializeHeatmap() {
    const artistSelect = document.getElementById('artistHeatmapSelect');
    if (!artistSelect) {
        return;
    }

    artistHeatmapChoices = new Choices(artistSelect, {
        searchEnabled: true,
        shouldSort: false,
        itemSelectText: ''
    });

    artistSelect.addEventListener('change', function() {
        const artistId = artistSelect.value;

        if (!artistId) {
            clearHeatmapState();
            return;
        }

        loadHeatmap(artistId);
    });
}

function initializeTables() {
    $('#datatables-reponsive_usuarios').DataTable({
        responsive: true,
        searching: true,
        ordering: true,
        paging: true,
        pageLength: 10,
        language: {
            url: 'https://cdn.datatables.net/plug-ins/1.13.1/i18n/es-ES.json'
        },
        columnDefs: [{
            targets: 1,
            type: 'date-eu'
        }],
        order: [[1, 'desc']]
    });

    $('#datatables-reponsive_tarifas').DataTable({
        responsive: true,
        searching: true,
        ordering: true,
        paging: true,
        pageLength: 10,
        language: {
            url: 'https://cdn.datatables.net/plug-ins/1.13.1/i18n/es-ES.json'
        },
        order: [[2, 'desc']]
    });

    $('#datatables-reponsive_ocupaciones_actividad').DataTable({
        responsive: true,
        searching: true,
        ordering: true,
        paging: true,
        pageLength: 10,
        language: {
            url: 'https://cdn.datatables.net/plug-ins/1.13.1/i18n/es-ES.json'
        },
        order: [[2, 'desc']]
    });
}

function loadHeatmap(artistId) {
    const artistSelect = document.getElementById('artistHeatmapSelect');
    const heatmapUrl = artistSelect.dataset.heatmapUrl;

    if (currentHeatmapRequest) {
        currentHeatmapRequest.abort();
    }

    setHeatmapLoading(true);
    hideHeatmapError();
    updateHeatmapStatus('Cargando mapa de calor…');

    currentHeatmapRequest = $.ajax({
        url: heatmapUrl,
        type: 'GET',
        data: { artistId: artistId },
        success: function(response) {
            renderHeatmap(response);
            updateHeatmapStatus('Periodo: ' + response.from + ' → ' + response.to);
        },
        error: function(xhr, status) {
            if (status === 'abort') {
                return;
            }

            destroyHeatmap();

            if (xhr.status === 404) {
                showHeatmapError('El artista seleccionado ya no está disponible o ya no está activo.');
            } else {
                showHeatmapError('No se pudo cargar el mapa de calor. Inténtalo de nuevo.');
            }

            updateHeatmapStatus('Selecciona otro artista o vuelve a intentarlo.');
        },
        complete: function() {
            currentHeatmapRequest = null;
            setHeatmapLoading(false);
        }
    });
}

function renderHeatmap(response) {
    if (typeof ApexCharts === 'undefined') {
        showHeatmapError('ApexCharts no está disponible en esta página.');
        updateHeatmapStatus('No se pudo inicializar el mapa de calor.');
        return;
    }

    const container = document.getElementById('actividadHeatmapContainer');
    const chartElement = document.getElementById('actividadHeatmapChart');
    const series = response.series.map(function(row) {
        return {
            name: HEATMAP_MONTH_LABELS[row.label] || row.label,
            data: row.data.map(function(cell) {
                return {
                    x: String(cell.day),
                    y: cell.count
                };
            })
        };
    });

    destroyHeatmap();
    container.classList.remove('d-none');

    currentHeatmapChart = new ApexCharts(chartElement, {
        chart: {
            type: 'heatmap',
            height: HEATMAP_CHART_OPTIONS.height,
            toolbar: {
                show: false
            }
        },
        series: series,
        dataLabels: {
            enabled: false
        },
        stroke: {
            width: 1,
            colors: [HEATMAP_CHART_OPTIONS.strokeColor]
        },
        plotOptions: {
            heatmap: {
                radius: 0,
                shadeIntensity: 0,
                colorScale: {
                    ranges: HEATMAP_COLOR_RANGES
                }
            }
        },
        xaxis: {
            type: 'category',
            categories: response.days.map(function(day) {
                return String(day);
            })
        },
        yaxis: {
            labels: {
                show: true
            }
        },
        legend: {
            show: true,
            position: 'top'
        },
        tooltip: {
            y: {
                formatter: function(value) {
                    return value + ' ocupación' + (value === 1 ? '' : 'es');
                }
            }
        }
    });

    currentHeatmapChart.render();
}

function clearHeatmapState() {
    if (currentHeatmapRequest) {
        currentHeatmapRequest.abort();
        currentHeatmapRequest = null;
    }

    setHeatmapLoading(false);
    hideHeatmapError();
    destroyHeatmap();
    updateHeatmapStatus('Selecciona un artista para cargar el mapa de calor.');
}

function destroyHeatmap() {
    const container = document.getElementById('actividadHeatmapContainer');
    if (currentHeatmapChart) {
        currentHeatmapChart.destroy();
        currentHeatmapChart = null;
    }
    if (container) {
        container.classList.add('d-none');
    }
}

function setHeatmapLoading(isLoading) {
    const artistSelect = document.getElementById('artistHeatmapSelect');

    if (artistHeatmapChoices) {
        if (isLoading) {
            artistHeatmapChoices.disable();
        } else {
            artistHeatmapChoices.enable();
        }
    }

    if (artistSelect) {
        artistSelect.disabled = isLoading;
    }
}

function updateHeatmapStatus(message) {
    const statusElement = document.getElementById('actividadHeatmapStatus');
    if (statusElement) {
        statusElement.textContent = message;
    }
}

function showHeatmapError(message) {
    const errorElement = document.getElementById('actividadHeatmapError');
    if (!errorElement) {
        return;
    }

    errorElement.textContent = message;
    errorElement.classList.remove('d-none');
}

function hideHeatmapError() {
    const errorElement = document.getElementById('actividadHeatmapError');
    if (!errorElement) {
        return;
    }

    errorElement.textContent = '';
    errorElement.classList.add('d-none');
}

function actualizarGraficoAccesos() {
    const usuarioId = $('#usuarioSelectGrafico').val();
    const fechaDesde = $('#fechaDesdeGrafico').val();
    const fechaHasta = $('#fechaHastaGrafico').val();

    if (!fechaDesde || !fechaHasta) {
        alert('Por favor completa los campos de fecha');
        return;
    }

    let [dDesde, mDesde, aDesde] = fechaDesde.split('-');
    let [dHasta, mHasta, aHasta] = fechaHasta.split('-');
    let fechaDesdeISO = aDesde + '-' + mDesde + '-' + dDesde + 'T00:00:00';
    let fechaHastaISO = aHasta + '-' + mHasta + '-' + dHasta + 'T23:59:59';

    let params = new URLSearchParams({
        fechaInicio: fechaDesdeISO,
        fechaFin: fechaHastaISO
    });

    if (usuarioId) {
        params.append('usuarioId', usuarioId);
    }

    $.ajax({
        url: '/registro-login/chart-data?' + params.toString(),
        type: 'GET',
        success: function(response) {
            if (response.success && response.chartData && response.chartData.length > 0) {
                $('#chartDataAccesos').val(JSON.stringify(response.chartData));
                reinitializeChartAccesos();
            } else {
                $('#accesosPorDiaContainer').hide();
                $('#sinDatosMsg').show();
                if (currentChartAccesos) {
                    currentChartAccesos.destroy();
                    currentChartAccesos = null;
                }
            }
        },
        error: function(xhr, status, error) {
            console.error('Error actualizando datos del gráfico:', error);
            alert('Error al cargar los datos del gráfico');
            $('#accesosPorDiaContainer').hide();
            $('#sinDatosMsg').show();
        }
    });
}

function initializeChartAccesos() {
    if (chartInitializedAccesos) {
        return;
    }

    if (typeof Chart === 'undefined') {
        console.error('Chart.js no está disponible');
        setTimeout(initializeChartAccesos, 1000);
        return;
    }

    if (currentChartAccesos) {
        currentChartAccesos.destroy();
        currentChartAccesos = null;
    }

    let chartDataElement = document.getElementById('chartDataAccesos');
    let chartData = [];

    try {
        if (chartDataElement && chartDataElement.value) {
            let rawData = chartDataElement.value;
            chartData = JSON.parse(rawData);

            if (!Array.isArray(chartData)) {
                chartData = [chartData];
            }
        }
    } catch (e) {
        console.error('Error parsing chart data:', e);
        chartData = [];
    }

    if (!chartData || chartData.length === 0) {
        chartData = [{
            dia: 'Sin datos',
            cantidad: 0
        }];
    }

    let canvas = document.getElementById('accesosPorDiaChart');
    if (!canvas) {
        console.error('Canvas accesosPorDiaChart no encontrado');
        return;
    }

    toggleChartVisibilityAccesos(true);

    var labels = chartData.map(item => item.dia);
    var data = chartData.map(item => item.cantidad);

    try {
        currentChartAccesos = new Chart(canvas, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Accesos del usuario',
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
                legend: {
                    display: true,
                    position: 'bottom'
                },
                tooltips: {
                    callbacks: {
                        label: function(tooltipItem, data) {
                            if (tooltipItem.yLabel === 0 && data.labels[tooltipItem.index] === 'Sin datos') {
                                return 'No hay accesos en el período seleccionado';
                            }
                            return data.datasets[tooltipItem.datasetIndex].label + ': ' + tooltipItem.yLabel + ' acceso' + (tooltipItem.yLabel !== 1 ? 's' : '');
                        }
                    }
                },
                scales: {
                    yAxes: [{
                        ticks: {
                            beginAtZero: true,
                            min: 0,
                            callback: function(value) {
                                return Number.isInteger(value) ? value : '';
                            }
                        },
                        gridLines: {
                            display: true
                        }
                    }],
                    xAxes: [{
                        gridLines: {
                            display: false
                        }
                    }]
                }
            }
        });

        chartInitializedAccesos = true;
    } catch (error) {
        console.error('Error creando el gráfico:', error);
        toggleChartVisibilityAccesos(false);
    }
}

function reinitializeChartAccesos() {
    chartInitializedAccesos = false;

    if (currentChartAccesos) {
        currentChartAccesos.destroy();
        currentChartAccesos = null;
    }

    initializeChartAccesos();
}

function toggleChartVisibilityAccesos(hasData) {
    const container = document.getElementById('accesosPorDiaContainer');
    const sinDatos = document.getElementById('sinDatosMsg');

    if (container && sinDatos) {
        if (hasData) {
            container.style.display = 'block';
            sinDatos.style.display = 'none';
        } else {
            container.style.display = 'none';
            sinDatos.style.display = 'block';
        }
    }
}
