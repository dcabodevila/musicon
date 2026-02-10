package es.musicalia.gestmusica.orquestasdegalicia;

import es.musicalia.gestmusica.ocupacion.OrquestasDeGaliciaException;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface OrquestasDeGaliciaService {
    @Transactional(readOnly = true)
    Optional<ActuacionExterna> obtenerActuacion(Integer idActuacionExterno) throws OrquestasDeGaliciaException;

    @Transactional
    DefaultResponseBody crearActuacion(ActuacionExterna actuacion);

    @Transactional
    DefaultResponseBody modificarActuacion(Integer idActuacionExterno, ActuacionExterna actuacion);

    @Transactional
    DefaultResponseBody eliminarActuacion(Integer idActuacionExterno);

//    @Transactional
//    void enviarActuacionOrquestasDeGalicia(boolean isCreacion, ActuacionExterna actuacionExterna, String nombreEstadoOcupacion) throws OrquestasDeGaliciaException;
}
