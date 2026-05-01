$(document).ready(function () {
    const $form = $("#formListadoOcupaciones");

    // Inicializar mapa Leaflet
    L.Icon.Default.imagePath = "/leaflet/images/";
    const mapaOcupaciones = L.map("mapaOcupaciones").setView([40.4168, -3.7038], 6);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(mapaOcupaciones);
    const markersLayer = L.layerGroup().addTo(mapaOcupaciones);

    function actualizarMarcadores(api) {
        markersLayer.clearLayers();
        const datos = api.rows({ page: "current" }).data();
        const bounds = [];
        const coordenadasUsadas = new Map(); // clave: "lat,lng", valor: cantidad de usos

        datos.each(function (d) {
            // Extraer municipio y provincia de "municipioProvincia" (formato: "Municipio, Provincia")
            const munProvParts = (d.municipioProvincia || "").split(",");
            const municipioRaw = (munProvParts[0] || "").trim().toLowerCase();
            const provinciaRaw = (munProvParts[1] || "").trim().toLowerCase();

            // Si la provincia es "Provisional", no mostrar nada en el mapa
            if (provinciaRaw === "provisional") {
                return;
            }

            let lat = NaN;
            let lng = NaN;
            let esAproximada = false;

            const sinMunicipio = municipioRaw === "sin municipio" || municipioRaw === "provisional";

            if (sinMunicipio) {
                // Sin municipio o provisional con provincia conocida: mostrar en capital de provincia
                if (d.latitudProvincia != null && d.longitudProvincia != null) {
                    lat = parseFloat(d.latitudProvincia);
                    lng = parseFloat(d.longitudProvincia);
                    esAproximada = true;
                }
            } else {
                // Prioridad 1: coordenadas exactas del municipio
                if (d.latitud != null && d.longitud != null) {
                    lat = parseFloat(d.latitud);
                    lng = parseFloat(d.longitud);
                }
                // Prioridad 2: coordenadas de la capital de provincia (fallback)
                else if (d.latitudProvincia != null && d.longitudProvincia != null) {
                    lat = parseFloat(d.latitudProvincia);
                    lng = parseFloat(d.longitudProvincia);
                    esAproximada = true;
                }
            }

            if (!isNaN(lat) && !isNaN(lng)) {
                // Si la ubicación es aproximada y ya fue usada, aplicar un pequeño desplazamiento
                // para evitar que múltiples marcas queden exactamente superpuestas
                if (esAproximada) {
                    const clave = `${lat.toFixed(4)},${lng.toFixed(4)}`;
                    const usos = coordenadasUsadas.get(clave) || 0;
                    if (usos > 0) {
                        // Desplazar en espiral simple: ~300m por paso
                        const offset = 0.003 * usos;
                        lat += Math.cos(usos) * offset;
                        lng += Math.sin(usos) * offset;
                    }
                    coordenadasUsadas.set(clave, usos + 1);
                }

                const ubicacionLabel = esAproximada
                    ? `<span class="badge bg-info">Ubicación aproximada (capital de provincia)</span>`
                    : `<span class="badge bg-success">Ubicación exacta</span>`;

                const popupContent = `<strong>${escapeHtml(d.artista)}</strong><br>` +
                    `<small>${escapeHtml(d.start || "")}</small><br>` +
                    `<span class="text-muted">${escapeHtml(d.municipioProvincia || "")}</span><br>` +
                    ubicacionLabel;

                let marker;
                if (esAproximada) {
                    // Círculo azul para ubicaciones aproximadas (capital de provincia)
                    marker = L.circleMarker([lat, lng], {
                        radius: 8,
                        fillColor: "#3b82f6",
                        color: "#1d4ed8",
                        weight: 2,
                        opacity: 1,
                        fillOpacity: 0.7
                    }).bindPopup(popupContent);
                } else {
                    // Marker normal para ubicaciones exactas
                    marker = L.marker([lat, lng]).bindPopup(popupContent);
                }

                markersLayer.addLayer(marker);
                bounds.push([lat, lng]);
            }
        });

        if (bounds.length > 0) {
            mapaOcupaciones.fitBounds(bounds, { padding: [30, 30], maxZoom: 12 });
        } else {
            mapaOcupaciones.setView([40.4168, -3.7038], 6);
        }
    }

    function escapeHtml(text) {
        if (!text) return "";
        return text
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    const table = $("#datatables-reponsive_ocupaciones").DataTable({
        responsive: true,
        searching: true,
        ordering: true,
        paging: true,
        processing: true,
        serverSide: true,
        deferLoading: 0,
        language: {
            url: "https://cdn.datatables.net/plug-ins/1.13.1/i18n/es-ES.json"
        },
        order: [[1, "desc"]],
        ajax: {
            url: "/ocupacion/list/data",
            type: "POST",
            data: function (d) {
                $form.serializeArray().forEach(function (field) {
                    d[field.name] = field.value;
                });
            }
        },
        drawCallback: function (settings) {
            actualizarMarcadores(this.api());
        },
        columns: [
            { data: "artista", defaultContent: "" },
            { data: "fechaCreacion", defaultContent: "" },
            { data: "start", defaultContent: "" },
            { data: "localidad", defaultContent: "" },
            { data: "municipioProvincia", defaultContent: "" },
            { data: "nombreUsuario", defaultContent: "" },
            {
                data: null,
                render: function (data, type, row) {
                    if (row.tipoOcupacion === "Reservado") {
                        return '<a href="#" class="badge bg-warning me-1 my-1">Reservado</a>';
                    }
                    if (row.estado === "Pendiente") {
                        return '<a href="#" class="badge bg-secondary me-1 my-1">Pendiente</a>';
                    }
                    if (row.estado === "Ocupado") {
                        return '<a href="#" class="badge bg-success me-1 my-1">Ocupado</a>';
                    }
                    if (row.estado === "Anulado") {
                        return '<a href="#" class="badge bg-danger me-1 my-1">Anulado</a>';
                    }
                    return row.estado || "";
                }
            },
            {
                data: null,
                render: function (data, type, row) {
                    if (row.matinal) {
                        return '<a href="#" class="badge bg-primary me-1 my-1">Matinal</a>';
                    }
                    if (row.soloMatinal) {
                        return '<a href="#" class="badge bg-primary me-1 my-1">Solo matinal</a>';
                    }
                    return "";
                }
            },
            {
                data: "id",
                orderable: false,
                searchable: false,
                render: function (id) {
                    return `<a href="/ocupacion/${id}" class="btn btn-primary btn-sm" title="Ver detalles de la ocupación"><i class="far fa-eye"></i></a>`;
                }
            }
        ]
    });

    flatpickr("#idFechaDesde", {
        disableMobile: true,
        locale: "es",
        altInput: true,
        altFormat: "j F, Y",
        dateFormat: "d-m-Y",
        allowInput: false,
        onChange: function () {
            document.getElementById("idFechaDesde").required = true;
        }
    });

    flatpickr("#idFechaHasta", {
        disableMobile: true,
        locale: "es",
        altInput: true,
        altFormat: "j F, Y",
        dateFormat: "d-m-Y",
        allowInput: false
    });

    $form.on("submit", function (event) {
        event.preventDefault();
        if ($("#idFechaDesde").val() === "") {
            notif("error", "Selecciona la fecha ocupación desde");
            return;
        }
        table.ajax.reload();
    });

    $("#agencia").on("change", function () {
        const idAgencia = $(this).val();
        const $selectArtista = $("#artista");
        $selectArtista.empty();
        $selectArtista.append('<option value="">Todos los artistas de la agencia</option>');

        if (idAgencia) {
            $.ajax({
                url: `/artista/artistas/${idAgencia}`,
                type: "GET",
                contentType: "application/json",
                success: function (data) {
                    $.each(data, function (index, artista) {
                        $selectArtista.append(`<option value="${artista.id}">${artista.nombre}</option>`);
                    });
                },
                error: function (xhr, status, error) {
                    console.error("Error al cargar los artistas:", error);
                    alert("Error al cargar los artistas. Intente de nuevo.");
                }
            });
        }
    });

    // Ejecutar búsqueda automáticamente al cargar la página
    $form.trigger("submit");
});

function crearNuevaOcupacion() {
    const selectedArtistId = $("#selectArtistasNuevaOcupacion").val();
    window.location.href = `/ocupacion/nueva/${selectedArtistId}`;
}

function descargarOcupacionesPDF() {
    const $btn = $("#btn-descargar-pdf-ocupaciones");
    const textoOriginal = $btn.html();
    $btn.prop("disabled", true).html('<i class="fas fa-spinner fa-spin"></i> Generando...');

    const formData = $("#formListadoOcupaciones").serialize();

    $.ajax({
        type: "POST",
        url: "/ocupacion/ocupaciones-pdf",
        data: formData,
        xhrFields: {
            responseType: "blob"
        },
        success: function (data, status, xhr) {
            $btn.prop("disabled", false).html(textoOriginal);

            const contentType = xhr.getResponseHeader("Content-Type");
            if (!contentType || !contentType.includes("application/pdf")) {
                notif("error", "Error: La respuesta del servidor no es válida");
                return;
            }

            const contentDisposition = xhr.getResponseHeader("Content-Disposition");
            let filename = "Ocupaciones_" + new Date().getTime() + ".pdf";

            if (contentDisposition) {
                const matches = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
                if (matches != null && matches[1]) {
                    filename = matches[1].replace(/['"]/g, "");
                }
            }

            const blob = new Blob([data], { type: "application/pdf" });
            const url = window.URL.createObjectURL(blob);
            const fileId = "pdf_ocupaciones_" + Date.now();
            window[fileId] = { blob: blob, url: url, filename: filename };

            notifDuration(
                "success",
                `<div class="d-flex gap-2">
                    <button onclick="window.open(window.${fileId}.url, '_blank')" class="btn btn-sm btn-outline-light w-50">
                        <i class="fas fa-eye"></i> Ver PDF
                    </button>
                    <button id="btn-share-pdf-ocupaciones" class="btn btn-sm btn-outline-light w-50" data-file-id="${fileId}">
                        <i class="fas fa-share"></i> Compartir PDF
                    </button>
                </div>`,
                60000
            );

            $(document).on("click", "#btn-share-pdf-ocupaciones", function () {
                const $shareBtn = $(this);
                const fileIdToShare = $shareBtn.data("file-id");
                const fileMetadata = window[fileIdToShare];

                if ($shareBtn.prop("disabled")) {
                    return;
                }

                if (navigator.share && navigator.canShare) {
                    const file = new File([fileMetadata.blob], fileMetadata.filename, { type: "application/pdf" });

                    if (navigator.canShare({ files: [file] })) {
                        $shareBtn.prop("disabled", true);

                        navigator.share({
                            files: [file],
                            title: "Listado de Ocupaciones",
                            text: "Aquí tienes el PDF de ocupaciones generado."
                        }).catch(function (error) {
                            console.error("Error al compartir:", error);
                            notif("error", "No se pudo compartir el archivo.");
                        }).finally(function () {
                            $shareBtn.prop("disabled", false);
                        });
                    } else {
                        notif("error", "Tu dispositivo no admite compartir archivos.");
                    }
                } else {
                    notif("error", "Compartir archivos no está disponible en este navegador.");
                }
            });

            setTimeout(function () {
                delete window[fileId];
                window.URL.revokeObjectURL(url);
            }, 60000);
        },
        error: function (xhr, status, error) {
            $btn.prop("disabled", false).html(textoOriginal);
            console.error("Error al generar PDF:", error);

            let errorMessage = "Error al generar el PDF de ocupaciones";
            switch (xhr.status) {
                case 400:
                    errorMessage = "Datos del formulario inválidos";
                    break;
                case 500:
                    errorMessage = "Error interno del servidor";
                    break;
                case 0:
                    errorMessage = "Error de conexión con el servidor";
                    break;
            }

            notif("error", errorMessage);
        }
    });
}
