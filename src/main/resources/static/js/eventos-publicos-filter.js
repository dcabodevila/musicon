/**
 * Manejo de filtros dinámicos para página de eventos públicos.
 * Carga AJAX de municipios según provincia seleccionada.
 * Integración con Choices.js para selects mejorados.
 */
(function() {
    'use strict';

    // Configuración
    const CONFIG = {
        debounceMs: 300,
        apiBaseUrl: '/eventos/api/municipios',
        placeholderSeleccionarProvincia: 'Selecciona provincia',
        placeholderTodosMunicipios: 'Todos los municipios',
        errorCargando: 'Error cargando municipios',
        noMunicipios: 'No hay municipios disponibles'
    };

    // Estado
    let municipioChoice = null;
    let provinciaChoice = null;
    let abortController = null;
    let debounceTimer = null;

    /**
     * Inicializa el manejo de filtros cuando el DOM está listo.
     */
    function init() {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', setupFiltros);
        } else {
            setupFiltros();
        }
    }

    /**
     * Configura los filtros de provincia/municipio con Choices.js y AJAX.
     */
    function setupFiltros() {
        const provinciaEl = document.querySelector('#idProvincia');
        const municipioEl = document.querySelector('#idMunicipio');

        if (!provinciaEl || !municipioEl) {
            console.log('[Filtros] No se encontraron elementos de filtro');
            return;
        }

        // Determinar contexto de la página
        const contextoPagina = document.body.dataset.contextoPagina || detectarContextoPagina();
        const provinciaPreseleccionada = provinciaEl.dataset.valorSeleccionado || '';
        const municipioPreseleccionado = municipioEl.dataset.valorSeleccionado || '';

        // CRUCIAL: Leer options nativos ANTES de que Choices.js los elimine del DOM.
        // Choices.js destruye los <option> nativos al inicializarse,
        // por lo que debemos capturarlos aquí para no perder los datos server-rendered.
        const municipiosNativos = Array.from(municipioEl.options)
            .filter(opt => opt.value !== '')
            .map(opt => ({
                value: opt.value,
                label: opt.textContent,
                selected: opt.value === municipioPreseleccionado
            }));

        // Inicializar Choices.js para provincia
        provinciaChoice = new Choices(provinciaEl, {
            searchEnabled: true,
            shouldSort: false,
            placeholder: true,
            placeholderValue: 'Todas las provincias'
        });

        // Establecer provincia preseleccionada si existe
        if (provinciaPreseleccionada) {
            provinciaChoice.setChoiceByValue(provinciaPreseleccionada);
        }

        // Inicializar Choices.js para municipio (sin options, los agregaremos manualmente)
        municipioChoice = new Choices(municipioEl, {
            searchEnabled: true,
            shouldSort: false,
            placeholder: true
        });

        // Configurar estado inicial del select de municipio según contexto
        if (contextoPagina === 'provincia' || contextoPagina === 'municipio' || municipiosNativos.length > 0) {
            // En páginas de provincia/municipio, o con filtro activo:
            // los municipios ya vienen del servidor (capturados en municipiosNativos)
            restaurarMunicipios(municipiosNativos, municipioPreseleccionado);
        } else {
            // En página por defecto sin filtro: empezar con placeholder (municipios vía AJAX)
            setMunicipioPlaceholder(CONFIG.placeholderSeleccionarProvincia, true);
        }

        // Configurar listener para cambio de provincia con debounce
        provinciaEl.addEventListener('change', handleProvinciaChange);
    }

    /**
     * Detecta el contexto de la página basado en la URL o elementos del DOM.
     */
    function detectarContextoPagina() {
        const path = window.location.pathname;
        if (path.includes('/eventos/provincia/')) return 'provincia';
        if (path.includes('/eventos/municipio/')) return 'municipio';
        if (path.includes('/eventos/artista/')) return 'artista';
        return 'catalogo';
    }

    /**
     * Restaura municipios precargados del servidor en el select de Choices.js.
     * Recibe los datos capturados ANTES de la inicialización de Choices.js,
     * ya que Choices.js destruye los <option> nativos del DOM.
     */
    function restaurarMunicipios(municipiosNativos, municipioPreseleccionado) {
        if (municipiosNativos.length === 0) {
            setMunicipioPlaceholder(CONFIG.placeholderTodosMunicipios, false);
            return;
        }

        // Limpiar y establecer opciones con placeholder
        municipioChoice.clearStore();
        municipioChoice.setChoices([
            { value: '', label: CONFIG.placeholderTodosMunicipios, selected: !municipioPreseleccionado },
            ...municipiosNativos
        ], 'value', 'label', true);

        if (municipioPreseleccionado) {
            municipioChoice.setChoiceByValue(municipioPreseleccionado);
        }

        // Habilitar el select
        municipioChoice.enable();
    }

    /**
     * Maneja el cambio de provincia con debounce.
     */
    function handleProvinciaChange() {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            const provinciaSeleccionada = provinciaChoice.getValue(true);
            if (provinciaSeleccionada) {
                cargarMunicipios(provinciaSeleccionada);
            } else {
                resetMunicipios();
            }
        }, CONFIG.debounceMs);
    }

    /**
     * Carga municipios vía AJAX para la provincia seleccionada.
     */
    async function cargarMunicipios(nombreProvincia) {
        // Cancelar petición anterior si existe
        if (abortController) {
            abortController.abort();
        }
        abortController = new AbortController();

        // Mostrar estado de carga
        setMunicipioPlaceholder('Cargando municipios...', true);
        municipioChoice.disable();

        try {
            const response = await fetch(
                `${CONFIG.apiBaseUrl}?provincia=${encodeURIComponent(nombreProvincia)}`,
                {
                    signal: abortController.signal,
                    headers: {
                        'Accept': 'application/json'
                    }
                }
            );

            if (!response.ok) {
                if (response.status === 404) {
                    throw new Error('Provincia no encontrada');
                }
                throw new Error(`Error ${response.status}: ${response.statusText}`);
            }

            const municipios = await response.json();
            renderMunicipios(municipios);

        } catch (error) {
            if (error.name === 'AbortError') {
                console.log('[Filtros] Petición cancelada');
                return;
            }

            console.error('[Filtros] Error cargando municipios:', error);
            setMunicipioPlaceholder(CONFIG.errorCargando, true);

            // Notificación de error (opcional, no intrusiva)
            mostrarErrorSuave(error.message || CONFIG.errorCargando);
        } finally {
            abortController = null;
        }
    }

    /**
     * Renderiza la lista de municipios en el select.
     */
    function renderMunicipios(municipios) {
        if (!municipios || municipios.length === 0) {
            setMunicipioPlaceholder(CONFIG.noMunicipios, true);
            return;
        }

        // Mapear a formato Choices.js
        const opciones = municipios.map(m => ({
            value: m.nombre,
            label: m.nombre,
            selected: false
        }));

        // Limpiar y establecer nuevas opciones
        municipioChoice.clearStore();
        municipioChoice.setChoices([
            { value: '', label: CONFIG.placeholderTodosMunicipios, selected: true },
            ...opciones
        ], 'value', 'label', true);

        municipioChoice.enable();

        // Anunciar a tecnologías asistivas
        anunciarCambio(`Se cargaron ${municipios.length} municipios`);
    }

    /**
     * Resetea el select de municipios a estado inicial.
     */
    function resetMunicipios() {
        municipioChoice.clearStore();
        setMunicipioPlaceholder(CONFIG.placeholderSeleccionarProvincia, true);
    }

    /**
     * Establece un placeholder en el select de municipios.
     */
    function setMunicipioPlaceholder(texto, disabled = false) {
        municipioChoice.clearStore();
        municipioChoice.setChoices([
            { value: '', label: texto, selected: true, disabled: disabled }
        ], 'value', 'label', true);

        if (disabled) {
            municipioChoice.disable();
        } else {
            municipioChoice.enable();
        }
    }

    /**
     * Muestra un error suave (no intrusivo) al usuario.
     */
    function mostrarErrorSuave(mensaje) {
        // Podríamos mostrar un toast o notificación sutil
        // Por ahora, solo log en consola
        console.warn('[Filtros] Error:', mensaje);
    }

    /**
     * Anuncia cambios a tecnologías asistivas (accesibilidad).
     */
    function anunciarCambio(mensaje) {
        let anuncio = document.getElementById('filtros-anuncio');
        if (!anuncio) {
            anuncio = document.createElement('div');
            anuncio.id = 'filtros-anuncio';
            anuncio.setAttribute('role', 'status');
            anuncio.setAttribute('aria-live', 'polite');
            anuncio.setAttribute('aria-atomic', 'true');
            anuncio.className = 'visually-hidden';
            anuncio.style.cssText = 'position:absolute;left:-10000px;width:1px;height:1px;overflow:hidden;';
            document.body.appendChild(anuncio);
        }
        anuncio.textContent = mensaje;
    }

    // Iniciar
    init();
})();
