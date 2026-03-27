package es.musicalia.gestmusica.reactivacion;

/**
 * Datos de contexto para renderizar los templates de reactivación.
 */
public record ReactivacionContextDto(
        String nombre,
        long diasInactivo,
        long totalArtistas,
        long totalAgencias,
        long totalRepresentantes,
        String urlBaja
) {}
