package es.musicalia.gestmusica.tarifa;

import java.math.BigDecimal;

public record TarifaArtistaCcaaDto(
        Long idArtista,
        String nombreArtista,
        BigDecimal importe
) {
}
