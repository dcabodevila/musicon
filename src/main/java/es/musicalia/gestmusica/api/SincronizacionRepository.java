package es.musicalia.gestmusica.api;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SincronizacionRepository extends JpaRepository<Sincronizacion, Long> {



}
