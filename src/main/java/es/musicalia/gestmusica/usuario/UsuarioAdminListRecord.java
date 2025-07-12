package es.musicalia.gestmusica.usuario;

import java.sql.Timestamp;

public record UsuarioAdminListRecord(long id, String nombre, String apellidos,  String email, Timestamp fechaUltimoAcceso, String imagen, boolean activo, boolean validado){
}
