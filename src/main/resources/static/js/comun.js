

  function cargarProvincias(input,ccaaId, provinciaSelectId) {

      const url = '/localizacion/provincias/' + ccaaId;
      // Retornamos la promesa que devuelve $.ajax
      return $.ajax({
        url: url,
        type: 'GET'
      }).done(function(provincias) {
        const provinciaSelect = $(input);
        provinciaSelect.empty();

        $.each(provincias, function(index, provincia) {
          provinciaSelect.append($('<option>', {
            value: provincia.id,
            text: provincia.nombre
          }));
        });

        if (provinciaSelectId){
            provinciaSelect.val(provinciaSelectId);
        }

      }).fail(function(xhr, status, error) {
        console.error('Error al cargar provincias:', error);
      });
  }


    function cargarMunicipios(input,provinciaId, municipioSelectId) {

        // 2) Preparar la URL de la petición
        const url = '/localizacion/municipios/' + provinciaId;

        // 3) Llamar al endpoint
        return $.ajax({
          url: url,
          type: 'GET',
          success: function(municipios) {
            // 4) Limpiar el select de provincia
            const municipiosSelect = $(input);
            municipiosSelect.empty();

              const nuevasOpciones = municipios.map(m => ({
                value: m.id,
                label: m.nombre
              }));
              const municipioChoice = $(input).data('choicesInstance');
              municipioChoice.clearStore();

              // 3. Agregar las opciones con setChoices
              municipioChoice.setChoices(nuevasOpciones, 'value', 'label', false);

              if (municipioSelectId){
                municipioChoice.setChoiceByValue(municipioSelectId);
              }
          },
          error: function(xhr, status, error) {
            console.error('Error al cargar provincias:', error);
          }
        });
    }

function cargarLocalidades(selectId, idMunicipio, valorSeleccionado) {

    if (!idMunicipio) return $.Deferred().resolve();

    return $.ajax({
        url: '/localizacion/localidades/' + idMunicipio,
        method: 'GET',
        dataType: 'json',
        success: function(data) {
            $('#localidades-list').empty();

            // Añadir opciones al datalist
            data.forEach(item => {
                $('#localidades-list').append(`<option value="${item.nombre}">`);
            });

            // Establecer valor seleccionado si existe
            if (valorSeleccionado) {
                $(selectId).val(valorSeleccionado);
            }

        },
        error: function(xhr, status, error) {
            console.error('Error al cargar localidades:', error);
        }
    });
}


function notif(type, message){
    let duration = "5000";
    let ripple = true;
    let dismissible = false;
    window.notyf.open({
        type,
        message,
        duration,
        ripple,
        dismissible,
        position: {
            x: "center",
            y: "top"
        }
    });
}

function notifDuration(type, message, duration){
    let ripple = true;
    let dismissible = true;
    window.notyf.open({
        type,
        message,
        duration,
        ripple,
        dismissible,
        position: {
            x: "center",
            y: "top"
        }
    });
}

  function showConfirmationModal(callback) {
      // Mostrar la modal
      const modalElement = document.getElementById('modalConfirmacion');
      const modal = new bootstrap.Modal(modalElement);
      modal.show();

      // Responder a la confirmación
      document.getElementById('btnConfirmar').onclick = () => {
          modal.hide(); // Ocultar la modal
          callback(true); // Usuario aceptó
      };

      modalElement.addEventListener('hidden.bs.modal', () => {
          callback(false); // Usuario canceló si cerró la modal sin confirmar
      }, { once: true });
  }

  function checkPermission(targetId, targetType, permission) {
      return $.ajax({
          url: '/permisos/hasPermission',
          method: 'GET',
          data: {
              targetId: targetId,
              targetType: targetType,
              permission: permission
          }
      });
  }

  function confirmarOcupacion(idOcupacion){

      $.ajax({
          type: "GET",
          contentType: "application/json; charset=utf-8",
          url: "/ocupacion/confirmar/"+idOcupacion,
          dataType: 'json',
          cache: false,
          timeout: 600000,
          async: false,
          success: function (data) {
              $("#btn-confirmar-ocupacion").prop("disabled", false);
              if (data.success){
                  notif("success", data.message);
              }
              else {
                  notif("error", data.message);
              }
          },
          error: function (e) {
              $("#btn-confirmar-ocupacion").prop("disabled", false);
              console.log(e);
              notyf.error(e);
          }
      });

  }

  function anularOcupacion(idOcupacion){
      $.ajax({
          type: "GET",
          contentType: "application/json; charset=utf-8",
          url: "/ocupacion/anular/"+idOcupacion,
          dataType: 'json',
          cache: false,
          timeout: 600000,
          async: false,
          success: function (data) {
              $("#btn-anular-ocupacion").prop("disabled", false);
              if (data.success){
                  notif("success", data.message);
              }
              else {
                  notif("error", data.message);
              }
          },
          error: function (e) {
              $("#btn-anular-ocupacion").prop("disabled", false);
              console.log(e);
              notyf.error(e);
          }
      });

  }

function clearFlatpickrDate(inputId) {
    const input = document.getElementById(inputId);
    if (input && input._flatpickr) {
        input._flatpickr.clear(); // Limpia la fecha seleccionada
    }
}