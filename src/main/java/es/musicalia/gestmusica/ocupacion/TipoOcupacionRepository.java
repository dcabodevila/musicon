package es.musicalia.gestmusica.ocupacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoOcupacionRepository extends JpaRepository<TipoOcupacion, Long> {

}
