package es.musicalia.gestmusica.orquestasdegalicia;

import es.musicalia.gestmusica.ocupacion.OrquestasDeGaliciaException;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import org.springframework.transaction.annotation.Transactional;

public interface OrquestasDeGaliciaService {
    @Transactional(readOnly = true)
    ActuacionExterna obtenerActuacion(Integer idActuacionExterno);

    @Transactional
    DefaultResponseBody crearActuacion(ActuacionExterna actuacion);

    @Transactional
    DefaultResponseBody modificarActuacion(Integer idActuacionExterno, ActuacionExterna actuacion);

    @Transactional
    DefaultResponseBody eliminarActuacion(Integer idActuacionExterno);

//    @Transactional
//    void enviarActuacionOrquestasDeGalicia(boolean isCreacion, ActuacionExterna actuacionExterna, String nombreEstadoOcupacion) throws OrquestasDeGaliciaException;
}
