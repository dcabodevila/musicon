$(document).ready(function(){


    $('#datatables-reponsive_ocupaciones').DataTable( {
        responsive: true,
        searching: true,
        ordering:  true,
        paging: true,
        language: {
            url: "https://cdn.datatables.net/plug-ins/1.13.1/i18n/es-ES.json"
        },
            columnDefs: [
                {
                    targets: 1,
                    type: 'date-eu'
                },
                {
                    targets: 2,
                    type: 'date-eu'
                }
            ],
            order: [
              [1, 'desc']  // Luego ordena por la tercera columna (índice 2)
            ]


    } );

        let pickerFechaDesde = flatpickr("#idFechaDesde", {
            disableMobile: true,
            "locale": "es", 
            altInput: true,
            altFormat: "j F, Y",
            dateFormat: "d-m-Y",
            allowInput: false,
            onChange: function(selectedDates, dateStr, instance) {
                document.getElementById("idFechaDesde").required = true; // Refuerza el atributo required.
            }

        });
        let pickerFechaHasta = flatpickr("#idFechaHasta", {
            disableMobile: true,
            "locale": "es",
            altInput: true,
            altFormat: "j F, Y",
            dateFormat: "d-m-Y",
            allowInput: false
        });

        document.getElementById("formListadoOcupaciones").addEventListener("submit", function(event) {
            const fechaDesde = document.getElementById("idFechaDesde").value;
            if (document.getElementById('idFechaDesde').value ==''){
                notif('error','Selecciona la fecha ocupación desde');
                event.preventDefault();
            }
        });

       $("#agencia").on("change", function () {
            const idAgencia = $(this).val(); // Obtener el valor seleccionado

            // Limpiar y reiniciar las opciones del select de artistas
            const $selectArtista = $("#artista");
            $selectArtista.empty();
            $selectArtista.append('<option value="">Todos los artistas de la agencia</option>');

            // Verificar si se seleccionó una agencia válida
            if (idAgencia) {
                // Realizar la solicitud AJAX para obtener los artistas
                $.ajax({
                    url: `/artista/artistas/${idAgencia}`, // Endpoint del controlador
                    type: "GET",
                    contentType: "application/json",
                    success: function (data) {
                        // Agregar las opciones obtenidas en la lista de artistas
                        $.each(data, function (index, artista) {
                            $selectArtista.append(
                                `<option value="${artista.id}">${artista.nombre}</option>`
                            );
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


