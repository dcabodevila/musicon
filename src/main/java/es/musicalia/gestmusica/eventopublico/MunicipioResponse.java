package es.musicalia.gestmusica.eventopublico;

/**
 * DTO para respuesta de municipios en la API pública.
 * Usado en el endpoint /eventos/api/municipios
 */
public record MunicipioResponse(String nombre, String provincia) {
}
