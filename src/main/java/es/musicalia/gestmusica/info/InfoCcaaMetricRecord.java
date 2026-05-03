package es.musicalia.gestmusica.info;

public record InfoCcaaMetricRecord(
    String ccaaNombre,
    long usuariosActivos,
    long presupuestosUltimos30Dias
) {
}
