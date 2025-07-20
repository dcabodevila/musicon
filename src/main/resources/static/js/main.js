$(document).ready(function(){
    $(".confirmar-ocupacion").click(function (event) {
        let idOcupacion = $(this).data("idocupacion");
        showConfirmationModal(function (confirmed) {
            if (confirmed) {
                confirmarOcupacion(idOcupacion);
                window.location.href = "/";
            }
        });


    });

    $(".anular-ocupacion").click(function (event) {
        let idOcupacion = $(this).data("idocupacion");
        showConfirmationModal(function (confirmed) {
            if (confirmed) {
                anularOcupacion(idOcupacion);
                window.location.href = "/";

            }
        });
    });

//    // Inicializar el carrusel
//    new bootstrap.Carousel(document.querySelector('#carrousel-lista-ocupaciones-pendientes'), {
//        interval: 5000, // Cambiar cada 5 segundos
//        wrap: true,     // Permitir ciclo continuo
//        touch: true     // Permitir control táctil
//    });
//
//    // Agregar animación a las tarjetas cuando se muestran
//    const carousel = document.querySelector('#carrousel-lista-ocupaciones-pendientes');
//    carousel.addEventListener('slide.bs.carousel', function () {
//        const cards = document.querySelectorAll('.card');
//        cards.forEach(card => {
//            card.classList.remove('animate__fadeIn');
//            void card.offsetWidth; // Forzar reflow
//            card.classList.add('animate__fadeIn');
//        });
//    });


});


