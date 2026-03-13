$(document).ready(function(){
    flatpickr.localize(flatpickr.l10ns.es);
    flatpickr("#date1", {"locale": "es",
        locale: {
            firstDayOfWeek: 1,
            weekdays: {
                      shorthand: ['Do', 'Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sa'],
                      longhand: ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'],
                    },
                    months: {
                      shorthand: ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Оct', 'Nov', 'Dic'],
                      longhand: ['Enero', 'Febrero', 'Мarzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'],
                    }
        },
        dateFormat: "d-m-Y"
        });

    new Choices(document.querySelector("#usuario"));
    new Choices(document.querySelector("#ccaa"));

const multipleChoices = new Choices(document.querySelector('.choices-multiple'), {
        removeItemButton: true, // Permite eliminar elementos seleccionados
        placeholderValue: 'Selección tipos de artista', // Placeholder inicial
        searchPlaceholderValue: 'Escribe para buscar', // Placeholder del campo de búsqueda
    });

  function validateChoices() {
    const selectElement = document.querySelector('#tiposArtista');
    if (!selectElement.checkValidity()) {
      selectElement.reportValidity(); // Muestra el mensaje de validación nativo
      return false; // Evita que el formulario se envíe
    }
    return true; // Permite el envío si la validación es exitosa
  }

    document.querySelector('#artista-detail-edit-form').addEventListener('submit', function (event) {
      const selectElement = document.querySelector('#tiposArtista');

      // Valida si hay opciones seleccionadas
      if (selectElement.value === '') {
        notif("error", 'Seleccione al menos un tipo de artista');
        event.preventDefault(); // Evita el envío
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
            notif("error", "Error al solicitar la activación");
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
});




