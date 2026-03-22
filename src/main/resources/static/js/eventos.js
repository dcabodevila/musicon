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

    const fechaHastaPicker = flatpickr(hastaInput, {
        disableMobile: true,
        locale: "es",
        altInput: true,
        altFormat: "j F, Y",
        dateFormat: "Y-m-d",
        allowInput: false,
        defaultDate: hastaInput.value || null,
        minDate: desdeInput.value || null
    });

    const fechaDesdePicker = flatpickr(desdeInput, {
        disableMobile: true,
        locale: "es",
        altInput: true,
        altFormat: "j F, Y",
        dateFormat: "Y-m-d",
        allowInput: false,
        defaultDate: desdeInput.value || null,
        onChange: function (selectedDates) {
            if (!selectedDates || selectedDates.length === 0) {
                fechaHastaPicker.set("minDate", null);
                return;
            }
            const desdeDate = selectedDates[0];
            fechaHastaPicker.set("minDate", desdeDate);
        }
    });
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
    const provinciaSeleccionada = provinciaEl.dataset.valorSeleccionado || "";
    if (provinciaSeleccionada) {
        provinciaChoice.setChoiceByValue(provinciaSeleccionada);
    }
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
    const municipioSeleccionado = municipioEl.dataset.valorSeleccionado || "";
    if (municipioSeleccionado) {
        municipioChoice.setChoiceByValue(municipioSeleccionado);
    }

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

