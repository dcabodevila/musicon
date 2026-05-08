package es.musicalia.gestmusica.orquestasdegalicia;

import es.musicalia.gestmusica.ocupacion.OrquestasDeGaliciaException;
import es.musicalia.gestmusica.util.DefaultResponseBody;

import java.util.Optional;

public interface OrquestasDeGaliciaService {
    Optional<ActuacionExterna> obtenerActuacion(Integer idActuacionExterno) throws OrquestasDeGaliciaException;

    DefaultResponseBody crearActuacion(ActuacionExterna actuacion);

    DefaultResponseBody modificarActuacion(Integer idActuacionExterno, ActuacionExterna actuacion);

    DefaultResponseBody eliminarActuacion(Integer idActuacionExterno);

//    @Transactional
//    void enviarActuacionOrquestasDeGalicia(boolean isCreacion, ActuacionExterna actuacionExterna, String nombreEstadoOcupacion) throws OrquestasDeGaliciaException;
}
