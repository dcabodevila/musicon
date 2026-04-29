package es.musicalia.gestmusica.comunicacion;

/**
 * DTO que representa el resultado de una operación de envío de comunicaciones masivas.
 */
public record ComunicacionResult(
        int enviados,
        int fallidos,
        int excluidosBaja
) {
    /**
     * Devuelve el total de usuarios procesados.
     */
    public int total() {
        return enviados + fallidos + excluidosBaja;
    }

    /**
     * Indica si todos los envíos fueron exitosos.
     */
    public boolean exitoTotal() {
        return fallidos == 0 && excluidosBaja == 0;
    }
}
