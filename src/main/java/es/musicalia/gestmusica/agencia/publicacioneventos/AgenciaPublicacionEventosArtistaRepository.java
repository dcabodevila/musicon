package es.musicalia.gestmusica.agencia.publicacioneventos;

import es.musicalia.gestmusica.artista.Artista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AgenciaPublicacionEventosArtistaRepository extends JpaRepository<Artista, Long> {

    boolean existsByAgenciaIdAndActivoTrueAndPublicarEventosTrue(Long idAgencia);

    @Modifying
    @Query("update Artista a set a.publicarEventos = true where a.agencia.id = :idAgencia and a.activo = true")
    int activarPublicacionEventosPorAgencia(@Param("idAgencia") Long idAgencia);

    @Modifying
    @Query("update Artista a set a.publicarEventos = false where a.agencia.id = :idAgencia and a.activo = true")
    int desactivarPublicacionEventosPorAgencia(@Param("idAgencia") Long idAgencia);
}
