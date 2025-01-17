package es.musicalia.gestmusica.localizacion;

import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MunicipioRepository extends JpaRepository<Municipio, Long> {

    @Query("select new es.musicalia.gestmusica.generic.CodigoNombreRecord(m.id, m.nombre) from Municipio m where m.provincia.id = ?1")
    List<CodigoNombreRecord> findMunicipioByProvinciaId(long idProvincia);

}
