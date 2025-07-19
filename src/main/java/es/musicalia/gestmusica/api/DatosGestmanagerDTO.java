package es.musicalia.gestmusica.api;

public record DatosGestmanagerDTO(
        String id_artista,
        String accion,
        String fecha,
        String descripcion,
        String poblacion,
        String municipio,
        String provincia,
        String pais,
        String nombre_local,
        String estado,
        String indicadores
) {}
