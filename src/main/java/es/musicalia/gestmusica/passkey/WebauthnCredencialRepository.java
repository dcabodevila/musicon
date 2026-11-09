package es.musicalia.gestmusica.passkey;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebauthnCredencialRepository extends JpaRepository<WebauthnCredencial, String> {

    List<WebauthnCredencial> findByWebauthnUsuarioIdOrderByFechaCreacionAsc(String webauthnUsuarioId);
}
