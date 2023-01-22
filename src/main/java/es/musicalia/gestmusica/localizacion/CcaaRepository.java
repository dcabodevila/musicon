package es.musicalia.gestmusica.localizacion;

import es.musicalia.gestmusica.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CcaaRepository extends JpaRepository<Ccaa, Long> {

}
