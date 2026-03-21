$(document).ready(function () {

    const imagenInputUsuario = document.getElementById('imagen-input-usuario');
    const imagenPreviewUsuario = document.getElementById('imagen-preview-usuario');

    if (imagenInputUsuario && imagenPreviewUsuario) {
        imagenInputUsuario.addEventListener('change', function () {
            const file = this.files[0];
            if (file) {
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

});
