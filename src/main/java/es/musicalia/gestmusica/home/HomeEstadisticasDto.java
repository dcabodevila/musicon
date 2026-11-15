package es.musicalia.gestmusica.home;

import java.io.Serializable;

/**
 * Contadores globales de la plataforma mostrados en la home.
 */
public record HomeEstadisticasDto(long artistasActivos,
                                  long usuariosActivos,
                                  long presupuestosUltimoMes,
                                  long ocupacionesUltimoMes) implements Serializable {
}
