package es.musicalia.gestmusica.actividad;


import java.util.List;

public interface ActividadService {

    List<ActividadRecord> findActividadTarifas();
    List<ActividadRecord> findActividadOcupaciones  ();

}
