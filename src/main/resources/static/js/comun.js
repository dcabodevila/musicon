

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