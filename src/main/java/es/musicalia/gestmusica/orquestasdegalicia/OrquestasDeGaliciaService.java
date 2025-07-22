package es.musicalia.gestmusica.orquestasdegalicia;

import org.springframework.transaction.annotation.Transactional;

public interface OrquestasDeGaliciaService {
    @Transactional(readOnly = true)
    ActuacionExterna obtenerActuacion(Integer idActuacionExterno);

    @Transactional
    void crearActuacion(ActuacionExterna actuacion);

    @Transactional
    void modificarActuacion(Integer idActuacionExterno, ActuacionExterna actuacion);

    @Transactional
    void eliminarActuacion(Integer idActuacionExterno);
}
