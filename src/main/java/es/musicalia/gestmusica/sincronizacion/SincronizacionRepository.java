package es.musicalia.gestmusica.sincronizacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SincronizacionRepository extends JpaRepository<Sincronizacion, Long> {



}
