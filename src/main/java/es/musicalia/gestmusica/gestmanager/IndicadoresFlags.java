
package es.musicalia.gestmusica.gestmanager;

/**
 * Clase que representa los flags de los indicadores de Gestmanager
 */
public class IndicadoresFlags {
    private final boolean matinal;
    private final boolean soloMatinal;
    private final boolean salaFiestas;

    public IndicadoresFlags(boolean matinal, boolean soloMatinal, boolean salaFiestas) {
        this.matinal = matinal;
        this.soloMatinal = soloMatinal;
        this.salaFiestas = salaFiestas;
    }

    public boolean isMatinal() {
        return matinal;
    }

    public boolean isSoloMatinal() {
        return soloMatinal;
    }

    public boolean isSalaFiestas() {
        return salaFiestas;
    }
}

