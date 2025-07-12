package es.musicalia.gestmusica.ajustes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AjustesRepository extends JpaRepository<Ajustes, Long> {

    @Query("select a from Ajustes a where a.usuario.id=?1 ")
    Ajustes findAjustesByIdUsuario(Long idUsuario);

}
