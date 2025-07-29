package es.musicalia.gestmusica.orquestasdegalicia;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

public interface OrquestasDeGaliciaService {
    @Transactional(readOnly = true)
    ActuacionExterna obtenerActuacion(Integer idActuacionExterno);

    @Transactional
    ResponseEntity<String> crearActuacion(ActuacionExterna actuacion);

    @Transactional
    ResponseEntity<String> modificarActuacion(Integer idActuacionExterno, ActuacionExterna actuacion);

    @Transactional
    ResponseEntity<String> eliminarActuacion(Integer idActuacionExterno);
}
