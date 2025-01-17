package es.musicalia.gestmusica.localizacion;

import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProvinciaRepository extends JpaRepository<Provincia, Long> {

    @Query("select new es.musicalia.gestmusica.generic.CodigoNombreRecord(p.id, p.nombre) from Provincia p where p.ccaa.id = ?1")
    List<CodigoNombreRecord> findProvinciaByIdCcaa(long idCcaa);

    @Query("select new es.musicalia.gestmusica.generic.CodigoNombreRecord(p.id, p.nombre) from Provincia p order by p.nombre asc")
    List<CodigoNombreRecord> findProvinciasOrderByName();

}
