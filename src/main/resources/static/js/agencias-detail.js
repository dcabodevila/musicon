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

    // Preview de logo de agencia con validación de tamaño
    const logoInputAgencia = document.getElementById('logo-input-agencia');
    const logoPreviewAgencia = document.getElementById('logo-preview-agencia');
    const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB — límite de Cloudinary

    if (logoInputAgencia && logoPreviewAgencia) {
        logoInputAgencia.addEventListener('change', function () {
            const file = this.files[0];
            if (file) {
                if (file.size > MAX_FILE_SIZE) {
                    notif('error', 'El archivo es demasiado grande. El tamaño máximo permitido es 10 MB. '
                        + 'El archivo seleccionado pesa ' + (file.size / 1024 / 1024).toFixed(1) + ' MB.');
                    this.value = ''; // Limpiar el input
                    if (!logoPreviewAgencia.dataset.hasExisting) {
                        logoPreviewAgencia.style.display = 'none';
                    }
                    return;
                }
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

    // Validar tamaño del archivo al enviar el formulario de agencia
    if (logoInputAgencia) {
        const agenciaForm = logoInputAgencia.closest('form');
        if (agenciaForm) {
            agenciaForm.addEventListener('submit', function (event) {
                const fileInput = document.getElementById('logo-input-agencia');
                if (fileInput && fileInput.files[0] && fileInput.files[0].size > MAX_FILE_SIZE) {
                    notif('error', 'El archivo es demasiado grande. El tamaño máximo permitido es 10 MB. '
                        + 'El archivo seleccionado pesa ' + (fileInput.files[0].size / 1024 / 1024).toFixed(1) + ' MB.');
                    event.preventDefault();
                    return false;
                }
            });
        }
    }

});
