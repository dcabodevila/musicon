package es.musicalia.gestmusica.usuario;

import java.time.OffsetDateTime;

public record UsuarioAdminListRecord(long id, String nombre, String apellidos, String email, String rol, OffsetDateTime fechaUltimoAcceso, String imagen, boolean activo, boolean validado, String nombreComercial, String provincia){
}
