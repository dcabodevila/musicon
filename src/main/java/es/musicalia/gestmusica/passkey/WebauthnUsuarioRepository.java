package es.musicalia.gestmusica.passkey;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebauthnUsuarioRepository extends JpaRepository<WebauthnUsuario, String> {

    Optional<WebauthnUsuario> findByUsuarioId(Long usuarioId);
}
