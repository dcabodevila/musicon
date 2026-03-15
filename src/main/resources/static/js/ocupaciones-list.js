$(document).ready(function () {
    const $form = $("#formListadoOcupaciones");
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
