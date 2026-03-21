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
    new Choices(document.querySelector("#provincia"));

    // Preview de logo de agencia
    const logoInputAgencia = document.getElementById('logo-input-agencia');
    const logoPreviewAgencia = document.getElementById('logo-preview-agencia');
    if (logoInputAgencia && logoPreviewAgencia) {
        logoInputAgencia.addEventListener('change', function () {
            const file = this.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function (e) {
                    logoPreviewAgencia.src = e.target.result;
                    logoPreviewAgencia.style.display = 'block';
                };
                reader.readAsDataURL(file);
            } else {
                if (!logoPreviewAgencia.dataset.hasExisting) {
                    logoPreviewAgencia.style.display = 'none';
                }
            }
        });
    }

});
