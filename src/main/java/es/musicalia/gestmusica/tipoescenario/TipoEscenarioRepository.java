package es.musicalia.gestmusica.tipoescenario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoEscenarioRepository extends JpaRepository<TipoEscenario, Long> {


}
