package es.musicalia.gestmusica.localizacion;

import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocalidadRepository extends JpaRepository<Localidad, Long> {

    @Query("select new es.musicalia.gestmusica.generic.CodigoNombreRecord(p.id, p.nombre) from Localidad p where p.municipio.id = ?1")
    List<CodigoNombreRecord> findLocalidadByIdMunicipio(long idMunicipio);
}
