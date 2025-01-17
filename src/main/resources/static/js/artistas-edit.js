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
});




