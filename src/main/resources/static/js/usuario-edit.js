$(document).ready(function () {

    const imagenInputUsuario = document.getElementById('imagen-input-usuario');
    const imagenPreviewUsuario = document.getElementById('imagen-preview-usuario');
    const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB — límite de Cloudinary

    if (imagenInputUsuario && imagenPreviewUsuario) {
        imagenInputUsuario.addEventListener('change', function () {
            const file = this.files[0];
            if (file) {
                if (file.size > MAX_FILE_SIZE) {
                    notif('error', 'El archivo es demasiado grande. El tamaño máximo permitido es 10 MB. '
                        + 'El archivo seleccionado pesa ' + (file.size / 1024 / 1024).toFixed(1) + ' MB.');
                    this.value = ''; // Limpiar el input
                    if (!imagenPreviewUsuario.dataset.hasExisting) {
                        imagenPreviewUsuario.style.display = 'none';
                    }
                    return;
                }
                const reader = new FileReader();
                reader.onload = function (e) {
                    imagenPreviewUsuario.src = e.target.result;
                    imagenPreviewUsuario.style.display = 'block';
                };
                reader.readAsDataURL(file);
            } else {
                if (!imagenPreviewUsuario.dataset.hasExisting) {
                    imagenPreviewUsuario.style.display = 'none';
                }
            }
        });
    }

    // Validar tamaño del archivo al enviar el formulario de usuario
    if (imagenInputUsuario) {
        const usuarioForm = imagenInputUsuario.closest('form');
        if (usuarioForm) {
            usuarioForm.addEventListener('submit', function (event) {
                const fileInput = document.getElementById('imagen-input-usuario');
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
