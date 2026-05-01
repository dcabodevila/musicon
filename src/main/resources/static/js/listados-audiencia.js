// Variable global para mantener referencia al gráfico
let currentChart = null;
let chartInitialized = false;

// Variables para el mapa
let mapaProvincias = null;
let geoJsonLayer = null;
let markersLayer = null;
let mapaInicializado = false;

// Mapa de normalización de nombres de provincia (BBDD -> GeoJSON)
// El GeoJSON usa nombres bilingües oficiales; la BBDD puede usar nombres simplificados
const PROVINCIA_NOMBRE_MAP = {
    "Álava": "Araba/Álava",
    "Alicante": "Alacant/Alicante",
    "Baleares": "Illes Balears",
    "Castellón": "Castelló/Castellón",
    "Gipuzkoa": "Gipuzkoa/Guipúzcoa",
    "Bizkaia": "Bizkaia/Vizcaya",
    "Tenerife": "Santa Cruz De Tenerife",
    "Valencia": "València/Valencia"
};

// Normaliza un nombre de provincia al formato usado en el GeoJSON
function normalizarNombreProvincia(nombre) {
    if (!nombre) return nombre;
    // Mapeo directo de nombres de BBDD a GeoJSON
    if (PROVINCIA_NOMBRE_MAP[nombre]) {
        return PROVINCIA_NOMBRE_MAP[nombre];
    }
    return nombre;
}

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
            {
                data: 'provincia',
                render: function(data, type, row) {
                    return data || '-';
                }
            },
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

    // Inicializar el mapa
    initMapa();

    // Cargar chart y datos del mapa con UNA sola petición.
    // El mapa se renderizará cuando el GeoJSON llegue, usando los datos guardados.
    updateAggregatedData();

    // Interceptar el envío del formulario para recargar la tabla
    $('#formListadoAudiencias').on('submit', function(e) {
        e.preventDefault();
        
        // Recargar la tabla con los nuevos filtros
        $('#datatables-reponsive_listados-generados').DataTable().ajax.reload();

        // Actualizar gráfico y mapa con UNA sola petición
        updateAggregatedData();
        
        return false;
    });
});

    // Función unificada que actualiza gráfico y mapa con UNA sola petición a BD
    function updateAggregatedData() {
        const idAgencia = $('#agencia').val();
        const fechaDesde = $('#idFechaDesde').val();
        const fechaHasta = $('#idFechaHasta').val();
        const porDia = $('#modoAgrupacion').val() === 'true';

        $.ajax({
            url: '/listado/audiencias/aggregated-data',
            type: 'POST',
            data: {
                idAgencia: idAgencia,
                fechaDesde: fechaDesde,
                fechaHasta: fechaHasta,
                porDia: porDia
            },
            success: function(response) {
                if (response.success) {
                    // Actualizar gráfico
                    if (response.chartData) {
                        $('#chartData').val(JSON.stringify(response.chartData));
                        reinitializeChart();
                    }
                    // Guardar datos del mapa para renderizar cuando el GeoJSON esté listo
                    if (response.mapData) {
                        window.lastMapData = response.mapData;
                        // Renderizar solo si el GeoJSON ya cargó
                        if (window.geoJsonData) {
                            renderizarMapa(response.mapData);
                        }
                    }
                } else {
                    console.error('Error en la respuesta agregada:', response.error);
                }
            },
            error: function(xhr, status, error) {
                console.error('Error actualizando datos agregados:', error);
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
                legend: {
                    display: true,
                    position: 'bottom'
                },
                tooltips: {
                    callbacks: {
                        label: function(tooltipItem, data) {
                            if (tooltipItem.yLabel === 0 && data.labels[tooltipItem.index] === 'Sin datos') {
                                return 'No hay listados en el período seleccionado';
                            }
                            return data.datasets[tooltipItem.datasetIndex].label + ': ' + tooltipItem.yLabel + ' listados';
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

// ============================================
// FUNCIONES DEL MAPA DE PROVINCIAS
// ============================================

// Función para inicializar el mapa
function initMapa() {
    // Evitar múltiples inicializaciones
    if (mapaInicializado) {
        return;
    }

    const mapContainer = document.getElementById('mapaProvincias');
    if (!mapContainer) {
        console.error('Contenedor del mapa no encontrado');
        return;
    }

    try {
        // Inicializar Leaflet centrado en el norte de España con más zoom
        mapaProvincias = L.map('mapaProvincias').setView([42.5, -3.5], 6);

        // Agregar capa de tiles de OpenStreetMap
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
            maxZoom: 18
        }).addTo(mapaProvincias);

        // Cargar GeoJSON de provincias
        cargarGeoJSON();

        mapaInicializado = true;

    } catch (error) {
        console.error('Error inicializando el mapa:', error);
    }
}

// Función para cargar el GeoJSON
function cargarGeoJSON() {
    $.getJSON('/geojson/espana-provincias.json')
        .done(function(data) {
            // Guardar referencia a los datos para uso posterior
            window.geoJsonData = data;
            // Si ya tenemos datos del mapa cargados, renderizar directamente.
            // Si no, la petición de updateAggregatedData() ya se habrá hecho
            // o se hará cuando el usuario cambie filtros.
            if (window.lastMapData) {
                renderizarMapa(window.lastMapData);
            }
        })
        .fail(function(jqXHR, textStatus, errorThrown) {
            console.error('Error cargando GeoJSON:', textStatus, errorThrown);
        });
}

// Función para actualizar el mapa con los datos actuales
function updateMapa() {
    // Si el mapa no está listo o no hay datos GeoJSON, salir
    if (!mapaProvincias || !window.geoJsonData) {
        return;
    }

    // Obtener valores de los filtros
    const idAgencia = $('#agencia').val();
    const fechaDesde = $('#idFechaDesde').val();
    const fechaHasta = $('#idFechaHasta').val();

    // Realizar petición AJAX para obtener datos del mapa
    $.ajax({
        url: '/listado/audiencias/map-data',
        type: 'POST',
        data: {
            idAgencia: idAgencia,
            fechaDesde: fechaDesde,
            fechaHasta: fechaHasta
        },
        success: function(response) {
            if (response.success && response.mapData) {
                renderizarMapa(response.mapData);
            } else {
                console.error('Error en la respuesta del mapa:', response.error);
                renderizarMapa([]);
            }
        },
        error: function(xhr, status, error) {
            console.error('Error actualizando datos del mapa:', error);
            renderizarMapa([]);
        }
    });
}

// Función para renderizar el mapa con los datos
function renderizarMapa(mapData) {
    // Crear mapa de provincia -> cantidad para búsqueda rápida
    const cantidadPorProvincia = {};
    mapData.forEach(function(item) {
        const nombreNormalizado = normalizarNombreProvincia(item.provincia);
        cantidadPorProvincia[nombreNormalizado] = item.cantidad;
    });

    // Eliminar capas anteriores si existen
    if (geoJsonLayer) {
        mapaProvincias.removeLayer(geoJsonLayer);
    }
    if (markersLayer) {
        mapaProvincias.removeLayer(markersLayer);
    }

    // Crear nueva capa de markers
    markersLayer = L.layerGroup().addTo(mapaProvincias);

    // Color primario del tema (fallback a azul si no existe)
    const primaryColor = (window.theme && window.theme.primary) ? window.theme.primary : '#3b7ddd';

    // Agregar capa GeoJSON
    geoJsonLayer = L.geoJSON(window.geoJsonData, {
        style: function(feature) {
            const nombreProvincia = normalizarNombreProvincia(feature.properties.name);
            const tieneDatos = cantidadPorProvincia[nombreProvincia] > 0;

            return {
                fillColor: tieneDatos ? primaryColor : '#e0e0e0',
                weight: 1,
                opacity: 1,
                color: '#666',
                dashArray: '',
                fillOpacity: tieneDatos ? 0.6 : 0.3
            };
        },
        onEachFeature: function(feature, layer) {
            const nombreProvincia = normalizarNombreProvincia(feature.properties.name);
            const cantidad = cantidadPorProvincia[nombreProvincia] || 0;

            // Popup con información
            const popupContent = '<strong>' + nombreProvincia + '</strong><br>' +
                                cantidad + ' listado' + (cantidad !== 1 ? 's' : '');
            layer.bindPopup(popupContent);

            // Tooltip en hover
            layer.bindTooltip(nombreProvincia + ': ' + cantidad + ' listado' + (cantidad !== 1 ? 's' : ''), {
                permanent: false,
                direction: 'top',
                className: 'provincia-tooltip'
            });

            // Agregar marcador con número en el centro de la provincia
            if (cantidad > 0) {
                const centro = obtenerCentroPoligono(feature);
                if (centro) {
                    const iconoNumero = L.divIcon({
                        className: 'marker-numero',
                        html: '<div style="' +
                              'background-color: ' + primaryColor + ';' +
                              'color: white;' +
                              'width: 28px;' +
                              'height: 28px;' +
                              'border-radius: 50%;' +
                              'display: flex;' +
                              'align-items: center;' +
                              'justify-content: center;' +
                              'font-size: 11px;' +
                              'font-weight: bold;' +
                              'border: 2px solid white;' +
                              'box-shadow: 0 2px 4px rgba(0,0,0,0.3);' +
                              '">' + cantidad + '</div>',
                        iconSize: [28, 28],
                        iconAnchor: [14, 14]
                    });

                    const marker = L.marker(centro, { icon: iconoNumero });
                    marker.bindPopup(popupContent);
                    marker.bindTooltip(nombreProvincia + ': ' + cantidad + ' listado' + (cantidad !== 1 ? 's' : ''), {
                        permanent: false,
                        direction: 'top'
                    });
                    markersLayer.addLayer(marker);
                }
            }
        }
    }).addTo(mapaProvincias);

    // Ajustar vista solo a provincias con datos (si las hay)
    // Primero recolectar features con datos
    const provinciasConDatos = [];
    window.geoJsonData.features.forEach(function(feature) {
        const nombre = normalizarNombreProvincia(feature.properties.name);
        if (cantidadPorProvincia[nombre] > 0) {
            provinciasConDatos.push(feature);
        }
    });

    if (provinciasConDatos.length > 0) {
        const bounds = L.geoJSON({type: "FeatureCollection", features: provinciasConDatos}).getBounds();
        if (bounds.isValid()) {
            mapaProvincias.fitBounds(bounds, {
                padding: [40, 40],
                maxZoom: 7
            });
        }
    }
}

// Función para obtener el centro de un polígono (centroide aproximado)
function obtenerCentroPoligono(feature) {
    try {
        const geometry = feature.geometry;
        let latlngs = [];

        if (geometry.type === 'Polygon') {
            // Para Polygon, usar el primer anillo
            latlngs = geometry.coordinates[0].map(function(coord) {
                return [coord[1], coord[0]]; // Leaflet usa [lat, lng], GeoJSON usa [lng, lat]
            });
        } else if (geometry.type === 'MultiPolygon') {
            // Para MultiPolygon, usar el primer polígono
            latlngs = geometry.coordinates[0][0].map(function(coord) {
                return [coord[1], coord[0]];
            });
        }

        if (latlngs.length === 0) {
            return null;
        }

        // Calcular centro usando bounds
        const bounds = L.latLngBounds(latlngs);
        return bounds.getCenter();

    } catch (error) {
        console.error('Error calculando centro del polígono:', error);
        return null;
    }
}
