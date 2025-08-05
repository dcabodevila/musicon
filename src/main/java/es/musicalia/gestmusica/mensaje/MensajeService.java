package es.musicalia.gestmusica.mensaje;

import org.springframework.cache.annotation.CacheEvict;

import java.util.List;
import java.util.Optional;

public interface MensajeService {

    @CacheEvict(cacheNames = "mensajesRecibidosPorUsuario", key = "#idUsuarioReceptor")
    Mensaje enviarMensaje(Mensaje mensaje, Long idUsuarioReceptor);

    Optional<Mensaje> obtenerMensajePorId(Long id);

    List<MensajeRecord> obtenerMensajesRecibidos(Long idUsuarioReceptor);

    List<MensajeRecord> obtenerMensajesEnviados(Long idUsuarioRemite);

    @CacheEvict(cacheNames  = "mensajesRecibidosPorUsuario", key = "#idUsuarioReceptor")
    void marcarComoLeido(Long id, Long idUsuarioReceptor);

    void eliminarMensaje(Long id);

}