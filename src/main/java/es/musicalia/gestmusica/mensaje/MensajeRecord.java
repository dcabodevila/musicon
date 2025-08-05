package es.musicalia.gestmusica.mensaje;

public record MensajeRecord(long id, String usuarioRemite, String usuarioReceptor, String asunto, String mensaje, String imagen, String urlEnlace){

}
