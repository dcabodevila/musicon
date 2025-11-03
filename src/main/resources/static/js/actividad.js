$(document).ready(function(){
    new Choices(document.querySelector("#usuarioSelectGrafico"));

});

        // Variable global para mantener referencia al gráfico
        let currentChartAccesos = null;
        let chartInitializedAccesos = false;

        $(document).ready(function() {
            // Configurar flatpickr para las fechas del gráfico
            let pickerFechaHastaGrafico = flatpickr("#fechaHastaGrafico", {
                disableMobile: true,
                "locale": "es",
                altInput: true,
                altFormat: "j F, Y",
                dateFormat: "d-m-Y",
                allowInput: false,
                defaultDate: "today"
            });

            let pickerFechaDesdeGrafico = flatpickr("#fechaDesdeGrafico", {
                disableMobile: true,
                "locale": "es",
                altInput: true,
                altFormat: "j F, Y",
                dateFormat: "d-m-Y",
                allowInput: false
            });

            // Interceptar el envío del formulario
            $('#formFiltroAccesos').on('submit', function(e) {
                e.preventDefault();
                actualizarGraficoAccesos();
                return false;
            });
        });

        // Función para actualizar el gráfico con filtros
        function actualizarGraficoAccesos() {
            const usuarioId = $('#usuarioSelectGrafico').val();
            const fechaDesde = $('#fechaDesdeGrafico').val();
            const fechaHasta = $('#fechaHastaGrafico').val();

            // Validar que las fechas estén completas (usuarioId es opcional)
            if (!fechaDesde || !fechaHasta) {
                alert('Por favor completa los campos de fecha');
                return;
            }

            // Convertir fechas a formato ISO
            let [dDesde, mDesde, aDesde] = fechaDesde.split('-');
            let [dHasta, mHasta, aHasta] = fechaHasta.split('-');
            let fechaDesdeISO = aDesde + '-' + mDesde + '-' + dDesde + 'T00:00:00';
            let fechaHastaISO = aHasta + '-' + mHasta + '-' + dHasta + 'T23:59:59';

            // Construir parámetros de la URL
            let params = new URLSearchParams({
                fechaInicio: fechaDesdeISO,
                fechaFin: fechaHastaISO
            });

            // Agregar usuarioId solo si está seleccionado
            if (usuarioId) {
                params.append('usuarioId', usuarioId);
            }

            // Realizar petición AJAX para obtener datos del gráfico
            $.ajax({
                url: '/registro-login/chart-data?' + params.toString(),
                type: 'GET',
                success: function(response) {
                    if (response.success && response.chartData && response.chartData.length > 0) {
                        // Actualizar el elemento hidden con los nuevos datos
                        $('#chartDataAccesos').val(JSON.stringify(response.chartData));

                        // Reinicializar el gráfico con los nuevos datos
                        reinitializeChartAccesos();
                    } else {
                        // Sin datos
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

        // Función separada para el gráfico
        function initializeChartAccesos() {
            // Evitar múltiples inicializaciones
            if (chartInitializedAccesos) {
                return;
            }

            // Verificar que Chart.js esté disponible
            if (typeof Chart === 'undefined') {
                console.error('Chart.js no está disponible');
                setTimeout(initializeChartAccesos, 1000);
                return;
            }

            // Destruir gráfico existente si existe
            if (currentChartAccesos) {
                currentChartAccesos.destroy();
                currentChartAccesos = null;
            }

            // Obtener datos del servidor
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

            // Verificar si hay datos válidos
            if (!chartData || chartData.length === 0) {
                chartData = [{
                    dia: "Sin datos",
                    cantidad: 0
                }];
            }

            // Verificar que el canvas existe
            let canvas = document.getElementById('accesosPorDiaChart');
            if (!canvas) {
                console.error('Canvas accesosPorDiaChart no encontrado');
                return;
            }

            // Mostrar el gráfico
            toggleChartVisibilityAccesos(true);

            // Preparar datos para Chart.js
            var labels = chartData.map(item => item.dia);
            var data = chartData.map(item => item.cantidad);

            try {
                // Crear gráfico con estilo AdminKit.io
                currentChartAccesos = new Chart(canvas, {
                    type: "bar",
                    data: {
                        labels: labels,
                        datasets: [{
                            label: "Accesos del usuario",
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
                                            return 'No hay accesos en el período seleccionado';
                                        }
                                        return context.dataset.label + ': ' + context.parsed.y + ' acceso' + (context.parsed.y !== 1 ? 's' : '');
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

                chartInitializedAccesos = true;

            } catch (error) {
                console.error('Error creando el gráfico:', error);
                toggleChartVisibilityAccesos(false);
            }
        }

        // Función para reinicializar el gráfico
        function reinitializeChartAccesos() {
            chartInitializedAccesos = false;

            if (currentChartAccesos) {
                currentChartAccesos.destroy();
                currentChartAccesos = null;
            }

            initializeChartAccesos();
        }

        // Función para mostrar/ocultar el gráfico
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
