package es.musicalia.gestmusica.incremento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoIncrementoRepository extends JpaRepository<TipoIncremento, Long> {


}
