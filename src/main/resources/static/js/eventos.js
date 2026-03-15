$(document).ready(function () {
    initDatePickers();
    initFiltrosChoices();
});

function initDatePickers() {
    const desdeInput = document.querySelector("#idFechaDesde");
    const hastaInput = document.querySelector("#idFechaHasta");

    if (!desdeInput || !hastaInput) {
        return;
    }

    const form = desdeInput.closest("form");
    const maxMonths = 3;

    const fechaHastaPicker = flatpickr(hastaInput, {
        disableMobile: true,
        locale: "es",
        altInput: true,
        altFormat: "j F, Y",
        dateFormat: "Y-m-d",
        allowInput: false,
        defaultDate: hastaInput.value || null,
        minDate: desdeInput.value || null,
        onChange: function (selectedDates) {
            fechaDesdePicker.set("maxDate", selectedDates.length > 0 ? selectedDates[0] : null);
            aplicarLimiteRango(fechaDesdePicker, fechaHastaPicker, maxMonths, true);
        }
    });

    const fechaDesdePicker = flatpickr(desdeInput, {
        disableMobile: true,
        locale: "es",
        altInput: true,
        altFormat: "j F, Y",
        dateFormat: "Y-m-d",
        allowInput: false,
        defaultDate: desdeInput.value || null,
        maxDate: hastaInput.value || null,
        onChange: function (selectedDates) {
            fechaHastaPicker.set("minDate", selectedDates.length > 0 ? selectedDates[0] : null);
            aplicarLimiteRango(fechaDesdePicker, fechaHastaPicker, maxMonths, true);
        }
    });

    aplicarLimiteRango(fechaDesdePicker, fechaHastaPicker, maxMonths, false);

    if (form) {
        form.addEventListener("submit", function (event) {
            const corregido = aplicarLimiteRango(fechaDesdePicker, fechaHastaPicker, maxMonths, true);
            if (corregido) {
                event.preventDefault();
            }
        });
    }
}

function initFiltrosChoices() {
    const provinciaEl = document.querySelector("#idProvincia");
    const municipioEl = document.querySelector("#idMunicipio");
    const artistaEl = document.querySelector("#idArtista");

    if (!provinciaEl || !municipioEl || typeof Choices === "undefined") {
        return;
    }

    const municipioOpcionesOriginales = Array.from(municipioEl.options).map(function (option) {
        return {
            value: option.value,
            label: option.textContent,
            provincia: option.dataset.provincia || "",
            selected: option.selected
        };
    });

    const provinciaChoice = new Choices(provinciaEl, {
        searchEnabled: true,
        shouldSort: false
    });
    if (artistaEl) {
        new Choices(artistaEl, {
            searchEnabled: true,
            shouldSort: false
        });
    }
    const municipioChoice = new Choices(municipioEl, {
        searchEnabled: true,
        shouldSort: false
    });

    function refrescarMunicipios() {
        const provinciaSeleccionada = provinciaChoice.getValue(true) || "";
        const municipioSeleccionado = municipioChoice.getValue(true) || "";

        const opcionesFiltradas = municipioOpcionesOriginales.filter(function (item) {
            if (item.value === "") {
                return true;
            }
            if (!provinciaSeleccionada) {
                return true;
            }
            return item.provincia.toLowerCase() === provinciaSeleccionada.toLowerCase();
        }).map(function (item) {
            return {
                value: item.value,
                label: item.label,
                selected: item.value === municipioSeleccionado
            };
        });

        municipioChoice.clearStore();
        municipioChoice.setChoices(opcionesFiltradas, "value", "label", true);

        const existeSeleccionado = opcionesFiltradas.some(function (item) {
            return item.value === municipioSeleccionado;
        });
        if (existeSeleccionado && municipioSeleccionado) {
            municipioChoice.setChoiceByValue(municipioSeleccionado);
        } else {
            municipioChoice.setChoiceByValue("");
        }
    }

    provinciaEl.addEventListener("change", refrescarMunicipios);
    refrescarMunicipios();
}

function aplicarLimiteRango(desdePicker, hastaPicker, maxMonths, notify) {
    const desdeDate = getPickerDate(desdePicker);
    const hastaDate = getPickerDate(hastaPicker);
    if (!desdeDate || !hastaDate) {
        return false;
    }

    const maxHasta = addMonths(desdeDate, maxMonths);
    if (hastaDate <= maxHasta) {
        return false;
    }

    hastaPicker.setDate(maxHasta, true, "Y-m-d");
    if (notify) {
        mostrarAvisoRangoMaximo(maxMonths);
    }
    return true;
}

function getPickerDate(picker) {
    return picker.selectedDates && picker.selectedDates.length > 0 ? picker.selectedDates[0] : null;
}

function addMonths(date, months) {
    const d = new Date(date.getTime());
    d.setMonth(d.getMonth() + months);
    return d;
}

function mostrarAvisoRangoMaximo(maxMonths) {
    const message = "El rango maximo permitido es de " + maxMonths + " meses.";
    if (typeof notifUnica === "function") {
        notifUnica("warning", message);
        return;
    }
    if (typeof notif === "function") {
        notif("warning", message);
        return;
    }
    if (window.notyf && typeof window.notyf.open === "function") {
        window.notyf.open({
            type: "warning",
            message: message,
            duration: 5000,
            ripple: true,
            dismissible: false,
            position: {
                x: "center",
                y: "top"
            }
        });
    }
}
