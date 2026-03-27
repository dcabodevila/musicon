package es.musicalia.gestmusica.reactivacion;

import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReactivacionTokenService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Genera y persiste token de baja en una transacción corta.
     */
    @Transactional
    public String generarYPersistirToken(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        if (usuario.getEmailBajaToken() == null) {
            usuario.setEmailBajaToken(UUID.randomUUID().toString().replace("-", ""));
            usuarioRepository.save(usuario);
        }
        return usuario.getEmailBajaToken();
    }
}
