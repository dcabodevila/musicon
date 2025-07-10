package es.musicalia.gestmusica.usuario;

import java.sql.Timestamp;

public record UsuarioAdminListRecord(long id, String nombre, String apellidos,  String email, Timestamp fechaUltimoAcceso, boolean activo, boolean validado){
}
