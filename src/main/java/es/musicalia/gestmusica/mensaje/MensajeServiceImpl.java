package es.musicalia.gestmusica.mensaje;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MensajeServiceImpl implements MensajeService {

    private final MensajeRepository mensajeRepository;

    public MensajeServiceImpl(MensajeRepository mensajeRepository) {
        this.mensajeRepository = mensajeRepository;
    }

    @CacheEvict(cacheNames = "mensajesRecibidosPorUsuario", key = "#idUsuarioReceptor")
    @Override
    public Mensaje enviarMensaje(Mensaje mensaje, Long idUsuarioReceptor) {
        mensaje.setFechaCreacion(LocalDateTime.now());
        return mensajeRepository.save(mensaje);
    }


    @Override
    public Optional<Mensaje> obtenerMensajePorId(Long id) {
        return mensajeRepository.findById(id);
    }

    @Override
    @Cacheable(cacheNames  = "mensajesRecibidosPorUsuario", key = "#idUsuarioReceptor")
    public List<MensajeRecord> obtenerMensajesRecibidos(Long idUsuarioReceptor) {
        return mensajeRepository.findAllByUsuarioReceptorIdAndActivoTrue(idUsuarioReceptor, false);
    }

    @Override
    public List<MensajeRecord> obtenerMensajesEnviados(Long idUsuarioRemite) {
        return mensajeRepository.findAllByUsuarioRemiteIdAndActivoTrue(idUsuarioRemite, false);
    }

    @CacheEvict(cacheNames = "mensajesRecibidosPorUsuario", key = "#idUsuarioReceptor")
    @Override
    public void marcarComoLeido(Long id, Long idUsuarioReceptor) {
        Optional<Mensaje> mensaje = mensajeRepository.findById(id);
        if (mensaje.isPresent()) {
            mensaje.get().setLeido(true);
            mensaje.get().setFechaLeido(LocalDateTime.now());
            mensajeRepository.save(mensaje.get());
        }
    }

    @Override
    public void eliminarMensaje(Long id) {
        Optional<Mensaje> mensaje = mensajeRepository.findById(id);
        mensaje.ifPresent(m -> {
            m.setActivo(false);
            mensajeRepository.save(m);
        });
    }

}