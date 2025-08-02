package es.musicalia.gestmusica.sincronizacion;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class SincronizacionResult {
    private int exitosas;
    private int creadas;
    private int actualizadas;
    private int eliminadas;
    private int sinCambios;
    private String errorGeneral;
    private Map<Integer, String> errores = new HashMap<>();

    public SincronizacionResult() {

    }

    public void addError(Integer id, String mensaje) {
        errores.put(id, mensaje);
    }
    
    public void incrementarExitosas() { exitosas++; }
    public void incrementarCreadas() { creadas++; }
    public void incrementarActualizadas() { actualizadas++; }
    public void incrementarEliminadas() { eliminadas++; }
    public void incrementarSinCambios() { sinCambios++; }
}