const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function createClassList(initialHidden = false) {
    const classes = new Set(initialHidden ? ['d-none'] : []);
    return {
        add(name) {
            classes.add(name);
        },
        remove(name) {
            classes.delete(name);
        },
        contains(name) {
            return classes.has(name);
        }
    };
}

function createElement(id, overrides = {}) {
    const listeners = {};
    return {
        id,
        value: '',
        disabled: false,
        dataset: {},
        textContent: '',
        classList: createClassList(overrides.hidden),
        addEventListener(event, handler) {
            listeners[event] = handler;
        },
        dispatch(event) {
            if (listeners[event]) {
                listeners[event]();
            }
        },
        ...overrides
    };
}

function buildHarness() {
    const readyCallbacks = [];
    const ajaxCalls = [];
    const elements = {
        artistHeatmapSelect: createElement('artistHeatmapSelect', {
            dataset: { heatmapUrl: '/actividad/ocupaciones-heatmap' }
        }),
        actividadHeatmapContainer: createElement('actividadHeatmapContainer', { hidden: true }),
        actividadHeatmapChart: createElement('actividadHeatmapChart'),
        actividadHeatmapStatus: createElement('actividadHeatmapStatus'),
        actividadHeatmapError: createElement('actividadHeatmapError', { hidden: true }),
        usuarioSelectGrafico: createElement('usuarioSelectGrafico'),
        fechaHastaGrafico: createElement('fechaHastaGrafico'),
        fechaDesdeGrafico: createElement('fechaDesdeGrafico')
    };

    class FakeChoices {
        constructor(element) {
            this.element = element;
            this.disabled = false;
        }

        disable() {
            this.disabled = true;
        }

        enable() {
            this.disabled = false;
        }
    }

    class FakeApexCharts {
        constructor(element, config) {
            this.element = element;
            this.config = config;
            this.rendered = false;
            this.destroyed = false;
            FakeApexCharts.instances.push(this);
        }

        render() {
            this.rendered = true;
        }

        destroy() {
            this.destroyed = true;
        }
    }

    FakeApexCharts.instances = [];

    const document = {
        getElementById(id) {
            return elements[id] ?? null;
        },
        querySelector(selector) {
            if (!selector.startsWith('#')) {
                return null;
            }
            return elements[selector.slice(1)] ?? null;
        }
    };

    function jquery(target) {
        if (target === document) {
            return {
                ready(callback) {
                    readyCallbacks.push(callback);
                }
            };
        }

        return {
            on() {},
            val() {
                return '';
            },
            DataTable() {}
        };
    }

    jquery.ajax = function ajax(options) {
        ajaxCalls.push(options);
        return {
            abort() {
                if (options.error) {
                    options.error({ status: 0 }, 'abort');
                }
            }
        };
    };

    const context = vm.createContext({
        document,
        window: {},
        console,
        setTimeout,
        clearTimeout,
        URLSearchParams,
        flatpickr() {},
        Choices: FakeChoices,
        ApexCharts: FakeApexCharts,
        Chart: function Chart() {},
        alert() {},
        $: jquery
    });

    const source = fs.readFileSync(
        path.resolve(__dirname, '../../main/resources/static/js/actividad.js'),
        'utf8'
    );
    vm.runInContext(source, context);

    return {
        context,
        elements,
        ajaxCalls,
        readyCallbacks,
        FakeApexCharts
    };
}

function sampleResponse(count = 0) {
    return {
        from: '2025-07-01',
        to: '2026-06-30',
        days: Array.from({ length: 31 }, (_, index) => index + 1),
        series: [{
            month: '2025-07',
            label: 'Julio',
            data: Array.from({ length: 31 }, (_, index) => ({ day: index + 1, count }))
        }]
    };
}

test('initializeHeatmap no auto-carga datos al entrar en /actividad', () => {
    const { context, ajaxCalls, elements } = buildHarness();

    context.initializeHeatmap();

    assert.equal(ajaxCalls.length, 0);
    assert.equal(typeof elements.artistHeatmapSelect.dispatch, 'function');
});

test('initializeHeatmap carga datos al seleccionar artista', () => {
    const { context, ajaxCalls, elements, FakeApexCharts } = buildHarness();

    context.initializeHeatmap();
    elements.artistHeatmapSelect.value = '7';
    elements.artistHeatmapSelect.dispatch('change');

    assert.equal(ajaxCalls.length, 1);
    assert.equal(ajaxCalls[0].data.artistId, '7');
    assert.equal(elements.artistHeatmapSelect.disabled, true);

    ajaxCalls[0].success(sampleResponse(2));
    ajaxCalls[0].complete();

    assert.equal(FakeApexCharts.instances.length, 1);
    assert.equal(FakeApexCharts.instances[0].rendered, true);
    assert.equal(elements.actividadHeatmapStatus.textContent, 'Periodo: 2025-07-01 → 2026-06-30');
    assert.equal(elements.artistHeatmapSelect.disabled, false);
});

test('clearHeatmapState limpia gráfico y estado al deseleccionar artista', () => {
    const { context, ajaxCalls, elements, FakeApexCharts } = buildHarness();

    context.initializeHeatmap();
    elements.artistHeatmapSelect.value = '7';
    elements.artistHeatmapSelect.dispatch('change');
    ajaxCalls[0].success(sampleResponse(1));
    ajaxCalls[0].complete();

    const chart = FakeApexCharts.instances[0];
    elements.artistHeatmapSelect.value = '';
    elements.artistHeatmapSelect.dispatch('change');

    assert.equal(chart.destroyed, true);
    assert.equal(elements.actividadHeatmapContainer.classList.contains('d-none'), true);
    assert.equal(elements.actividadHeatmapError.classList.contains('d-none'), true);
    assert.equal(elements.actividadHeatmapStatus.textContent, 'Selecciona un artista para cargar el mapa de calor.');
});

test('loadHeatmap muestra error visible para 404 y para fallo genérico', () => {
    const { context, ajaxCalls, elements } = buildHarness();

    context.initializeHeatmap();
    elements.artistHeatmapSelect.value = '7';
    elements.artistHeatmapSelect.dispatch('change');

    ajaxCalls[0].error({ status: 404 }, 'error');
    ajaxCalls[0].complete();
    assert.equal(elements.actividadHeatmapError.classList.contains('d-none'), false);
    assert.equal(elements.actividadHeatmapError.textContent, 'El artista seleccionado ya no está disponible o ya no está activo.');

    elements.artistHeatmapSelect.value = '8';
    elements.artistHeatmapSelect.dispatch('change');
    ajaxCalls[1].error({ status: 500 }, 'error');
    ajaxCalls[1].complete();
    assert.equal(elements.actividadHeatmapError.textContent, 'No se pudo cargar el mapa de calor. Inténtalo de nuevo.');
    assert.equal(elements.actividadHeatmapStatus.textContent, 'Selecciona otro artista o vuelve a intentarlo.');
});

test('renderHeatmap mantiene semántica blanca para matrices vacías', () => {
    const { context, FakeApexCharts } = buildHarness();

    context.renderHeatmap(sampleResponse(0));

    const config = FakeApexCharts.instances[0].config;
    assert.equal(config.plotOptions.heatmap.colorScale.ranges[0].from, 0);
    assert.equal(config.plotOptions.heatmap.colorScale.ranges[0].to, 0);
    assert.equal(config.plotOptions.heatmap.colorScale.ranges[0].name, '0');
    assert.equal(config.plotOptions.heatmap.colorScale.ranges[0].color, '#FFFFFF');
    assert.equal(config.series[0].data[0].y, 0);
    assert.equal(config.chart.height, 420);
});
