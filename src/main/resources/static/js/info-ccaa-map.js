(function () {
    const mapElement = document.getElementById('info-ccaa-map');
    if (!mapElement || typeof L === 'undefined') {
        return;
    }

    const metricasPorNombre = new Map();
    let geoJsonLayer = null;
    let selectedLayer = null;

    const NOMBRES_CCAA_POR_CODIGO = {
        '01': 'Andalucía',
        '02': 'Aragón',
        '03': 'Balears, Illes',
        '04': 'Canarias',
        '05': 'Cantabria',
        '06': 'Castilla - La Mancha',
        '07': 'Castilla y León',
        '08': 'Catalunya',
        '09': 'Ceuta',
        '10': 'Extremadura',
        '11': 'Galicia',
        '12': 'Rioja, La',
        '13': 'Madrid, Comunidad de',
        '14': 'Melilla',
        '15': 'Murcia, Región de',
        '16': 'Navarra, Comunidad Foral de',
        '17': 'País Vasco',
        '18': 'Asturias, Principado de',
        '19': 'Comunitat Valenciana'
    };

    const panelNombre = document.getElementById('info-ccaa-name');
    const panelAyuda = document.getElementById('info-ccaa-help');
    const panelUsuarios = document.getElementById('info-ccaa-usuarios');
    const panelPresupuestos = document.getElementById('info-ccaa-presupuestos');
    const panelContenedor = document.getElementById('info-ccaa-panel');

    const mapa = L.map('info-ccaa-map', {
        zoomControl: true,
        zoomSnap: 0.1,
        scrollWheelZoom: false,
        attributionControl: false
    });

    const estiloBase = {
        color: '#ffffff',
        weight: 1.2,
        fillColor: '#4bb9ec',
        fillOpacity: 0.7
    };

    const estiloHover = {
        color: '#f9fbff',
        weight: 2,
        fillColor: '#1b86c6',
        fillOpacity: 0.86
    };

    const estiloSeleccionado = {
        color: '#f7b733',
        weight: 2.2,
        fillColor: '#145f96',
        fillOpacity: 0.95
    };

    function normalizarNombre(nombre) {
        return (nombre || '')
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .replace(/\//g, ' ')
            .replace(/\s+/g, ' ')
            .trim()
            .toLowerCase();
    }

    function canonicalizarNombre(nombre) {
        const normalizado = normalizarNombre(nombre);
        const alias = {
            'principado de asturias': 'asturias principado de',
            'asturias principado de': 'asturias principado de',
            'asturias': 'asturias principado de',
            'islas baleares': 'balears illes',
            'illes balears': 'balears illes',
            'balears illes': 'balears illes',
            'canarias': 'canarias',
            'cantabria': 'cantabria',
            'castilla la mancha': 'castilla - la mancha',
            'castilla y leon': 'castilla y leon',
            'cataluna': 'catalunya',
            'cataluña': 'catalunya',
            'ceuta': 'ceuta',
            'extremadura': 'extremadura',
            'galicia': 'galicia',
            'comunidad de madrid': 'madrid comunidad de',
            'madrid comunidad de': 'madrid comunidad de',
            'region de murcia': 'murcia region de',
            'murcia region de': 'murcia region de',
            'comunidad valenciana': 'comunitat valenciana',
            'comunitat valenciana': 'comunitat valenciana',
            'navarra comunidad foral de': 'navarra comunidad foral de',
            'pais vasco euskadi': 'pais vasco',
            'pais vasco': 'pais vasco',
            'rioja la': 'rioja la',
            'la rioja': 'rioja la',
            'melilla': 'melilla'
        };
        return alias[normalizado] || normalizado;
    }

    function pintarPanel(nombreCcaa, metricas) {
        panelNombre.textContent = nombreCcaa;
        panelAyuda.textContent = 'Datos actualizados para esta comunidad autónoma.';
        panelUsuarios.textContent = new Intl.NumberFormat('es-ES').format(metricas.usuariosActivos || 0);
        panelPresupuestos.textContent = new Intl.NumberFormat('es-ES').format(metricas.presupuestosUltimos30Dias || 0);
    }

    function pintarTotales(metricas) {
        const totales = metricas.reduce((acc, item) => {
            acc.usuariosActivos += Number(item.usuariosActivos || 0);
            acc.presupuestosUltimos30Dias += Number(item.presupuestosUltimos30Dias || 0);
            return acc;
        }, { usuariosActivos: 0, presupuestosUltimos30Dias: 0 });

        const totalPresupuestosGlobal = Number(panelContenedor?.dataset?.totalPresupuestosGlobal);
        if (Number.isFinite(totalPresupuestosGlobal) && totalPresupuestosGlobal >= 0) {
            totales.presupuestosUltimos30Dias = totalPresupuestosGlobal;
        }

        panelNombre.textContent = 'Total nacional';
        panelAyuda.textContent = 'Resumen global de actividad. Haz clic en una comunidad para ver su detalle.';
        panelUsuarios.textContent = new Intl.NumberFormat('es-ES').format(totales.usuariosActivos);
        panelPresupuestos.textContent = new Intl.NumberFormat('es-ES').format(totales.presupuestosUltimos30Dias);
    }

    function obtenerMetrica(nombreCcaa) {
        return metricasPorNombre.get(canonicalizarNombre(nombreCcaa)) || {
            usuariosActivos: 0,
            presupuestosUltimos30Dias: 0
        };
    }

    function onEachFeature(feature, layer) {
        const nombreCcaa = feature.properties?.ccaa_nombre || 'Comunidad sin nombre';
        layer.bindTooltip(nombreCcaa, { sticky: true, direction: 'auto' });

        layer.on('mouseover', function () {
            if (selectedLayer !== layer) {
                layer.setStyle(estiloHover);
            }
        });

        layer.on('mouseout', function () {
            if (selectedLayer !== layer) {
                layer.setStyle(estiloBase);
            }
        });

        layer.on('click', function () {
            if (selectedLayer) {
                selectedLayer.setStyle(estiloBase);
            }
            selectedLayer = layer;
            selectedLayer.setStyle(estiloSeleccionado);

            const metricas = obtenerMetrica(nombreCcaa);
            pintarPanel(nombreCcaa, metricas);
        });
    }

    function agruparPorCcaa(geojsonProvincias) {
        const agrupadas = new Map();

        geojsonProvincias.features.forEach(feature => {
            const codCcaa = feature.properties?.cod_ccaa;
            if (!codCcaa) {
                return;
            }
            if (!agrupadas.has(codCcaa)) {
                agrupadas.set(codCcaa, {
                    type: 'Feature',
                    properties: {
                        cod_ccaa: codCcaa,
                        ccaa_nombre: NOMBRES_CCAA_POR_CODIGO[codCcaa] || codCcaa
                    },
                    geometry: {
                        type: 'MultiPolygon',
                        coordinates: []
                    }
                });
            }

            const ccaaFeature = agrupadas.get(codCcaa);
            const geometry = feature.geometry || {};

            if (geometry.type === 'Polygon') {
                ccaaFeature.geometry.coordinates.push(geometry.coordinates);
            } else if (geometry.type === 'MultiPolygon') {
                geometry.coordinates.forEach(polygon => ccaaFeature.geometry.coordinates.push(polygon));
            }
        });

        return {
            type: 'FeatureCollection',
            features: Array.from(agrupadas.values())
        };
    }

    async function cargarMetricas() {
        const respuesta = await fetch('/info/metricas-ccaa');
        if (!respuesta.ok) {
            throw new Error('No se pudieron cargar las métricas por comunidad autónoma.');
        }
        const metricas = await respuesta.json();
        pintarTotales(metricas);
        metricas.forEach(item => {
            metricasPorNombre.set(canonicalizarNombre(item.ccaaNombre), item);
        });
    }

    async function init() {
        try {
            await cargarMetricas();
            const geojson = await fetch('/geojson/espana-provincias.json').then(res => res.json());
            const geojsonCcaa = agruparPorCcaa(geojson);

            geoJsonLayer = L.geoJSON(geojsonCcaa, {
                style: estiloBase,
                onEachFeature
            }).addTo(mapa);

            const islasYExteriores = new Set(['Balears, Illes', 'Canarias', 'Ceuta', 'Melilla']);
            const peninsulares = geojsonCcaa.features.filter(feature => !islasYExteriores.has(feature.properties?.ccaa_nombre));
            const peninsularBounds = L.geoJSON({
                type: 'FeatureCollection',
                features: peninsulares
            }).getBounds();

            mapa.fitBounds(peninsularBounds.pad(0.00));
            mapa.setZoom(Math.min(mapa.getZoom() + 0.2, 6.0));
        } catch (error) {
            console.error(error);
            panelNombre.textContent = 'No se pudo cargar el mapa';
            panelAyuda.textContent = 'Inténtalo de nuevo más tarde.';
        }
    }

    init();
})();
