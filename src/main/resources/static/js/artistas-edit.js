$(document).ready(function () {
    flatpickr.localize(flatpickr.l10ns.es);
    flatpickr("#date1", {
        "locale": "es",
        locale: {
            firstDayOfWeek: 1,
            weekdays: {
                shorthand: ['Do', 'Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sa'],
                longhand: ['Domingo', 'Lunes', 'Martes', 'Miercoles', 'Jueves', 'Viernes', 'Sabado'],
            },
            months: {
                shorthand: ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'],
                longhand: ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'],
            }
        },
        dateFormat: "d-m-Y"
    });

    new Choices(document.querySelector("#usuario"));
    new Choices(document.querySelector("#ccaa"));

    new Choices(document.querySelector('.choices-multiple'), {
        removeItemButton: true,
        placeholderValue: 'Seleccion tipos de artista',
        searchPlaceholderValue: 'Escribe para buscar',
    });

    const comunidadesTrabajoSelect = document.querySelector('#comunidadesTrabajo');
    if (comunidadesTrabajoSelect) {
        new Choices(comunidadesTrabajoSelect, {
            removeItemButton: true,
            placeholderValue: 'Seleccion comunidades de trabajo',
            searchPlaceholderValue: 'Escribe para buscar',
        });
    }

    document.querySelector('#artista-detail-edit-form').addEventListener('submit', function (event) {
        const tiposArtistaElement = document.querySelector('#tiposArtista');
        const comunidadesElement = document.querySelector('#comunidadesTrabajo');

        if (tiposArtistaElement.value === '') {
            notif("error", 'Seleccione al menos un tipo de artista');
            event.preventDefault();
            return false;
        }

        if (comunidadesElement && comunidadesElement.value === '') {
            notif("error", 'Seleccione al menos una comunidad en la que trabajará el artista');
            event.preventDefault();
            return false;
        }
    });

    const $permiteOdg = $('#permiteOrquestasDeGalicia');
    const $sincronizarOdg = $('#sincronizarOdg');

    if ($permiteOdg.length && $sincronizarOdg.length) {
        const syncSincronizarOdgWithPermiteOdg = function () {
            const permiteOdgActivo = $permiteOdg.is(':checked');
            $sincronizarOdg.prop('disabled', !permiteOdgActivo);
            if (!permiteOdgActivo) {
                $sincronizarOdg.prop('checked', false);
            }
        };

        $permiteOdg.on('change', syncSincronizarOdgWithPermiteOdg);
        syncSincronizarOdgWithPermiteOdg();
    } else if ($sincronizarOdg.length) {
        $sincronizarOdg.prop('disabled', true);
        $sincronizarOdg.prop('checked', false);
    }

    const $btnSolicitarActivacionOdg = $('#btnSolicitarActivacionOdg');
    if ($btnSolicitarActivacionOdg.length) {
        $btnSolicitarActivacionOdg.on('click', function () {
            const idArtista = $(this).data('id-artista');
            if (!idArtista) {
                notif('error', 'No se ha encontrado el artista');
                return;
            }

            $btnSolicitarActivacionOdg.prop('disabled', true);

            $.ajax({
                type: "POST",
                contentType: "application/json; charset=utf-8",
                url: "/artista/" + idArtista + "/solicitar-activacion-odg",
                dataType: 'json',
                cache: false,
                success: function (data) {
                    if (data.success) {
                        notif(data.messageType ? data.messageType : "success", data.message);
                        $btnSolicitarActivacionOdg.text("Solicitud enviada");
                    } else {
                        notif("error", data.message);
                        $btnSolicitarActivacionOdg.prop('disabled', false);
                    }
                },
                error: function () {
                    notif("error", "Error al solicitar la activacion");
                    $btnSolicitarActivacionOdg.prop('disabled', false);
                }
            });
        });
    }

    const $btnActivarOdgAdmin = $('#btnActivarOdgAdmin');
    if ($btnActivarOdgAdmin.length) {
        $btnActivarOdgAdmin.on('click', function () {
            const idArtista = $(this).data('id-artista');
            if (!idArtista) {
                notif('error', 'No se ha encontrado el artista');
                return;
            }

            $btnActivarOdgAdmin.prop('disabled', true);

            $.ajax({
                type: "POST",
                contentType: "application/json; charset=utf-8",
                url: "/artista/" + idArtista + "/activar-odg",
                dataType: 'json',
                cache: false,
                success: function (data) {
                    if (data.success) {
                        notif("success", data.message);
                        window.location.reload();
                    } else {
                        notif(data.messageType ? data.messageType : "error", data.message);
                        $btnActivarOdgAdmin.prop('disabled', false);
                    }
                },
                error: function () {
                    notif("error", "Error al activar Orquestas de Galicia");
                    $btnActivarOdgAdmin.prop('disabled', false);
                }
            });
        });
    }

    // Preview de imagen de artista
    const logoInputArtista = document.getElementById('logo-input-artista');
    const logoPreviewArtista = document.getElementById('logo-preview-artista');
    if (logoInputArtista && logoPreviewArtista) {
        logoInputArtista.addEventListener('change', function () {
            const file = this.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function (e) {
                    logoPreviewArtista.src = e.target.result;
                    logoPreviewArtista.style.display = 'block';
                };
                reader.readAsDataURL(file);
            } else {
                if (!logoPreviewArtista.dataset.hasExisting) {
                    logoPreviewArtista.style.display = 'none';
                }
            }
        });
    }
});
