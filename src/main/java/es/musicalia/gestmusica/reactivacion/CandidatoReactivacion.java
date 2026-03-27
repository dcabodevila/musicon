package es.musicalia.gestmusica.reactivacion;

/**
 * DTO inmutable que representa un candidato para el envío de email de reactivación.
 * Se construye dentro de la transacción de lectura y se usa fuera de ella,
 * evitando mantener entidades JPA abiertas durante el delay y la llamada HTTP.
 */
public record CandidatoReactivacion(
        Long usuarioId,
        String nombre,
        String email,
        String emailBajaToken,
        SegmentoReactivacion segmento,
        long diasInactivo,
        long totalArtistas,
        long totalAgencias,
        long totalRepresentantes
) {
}
