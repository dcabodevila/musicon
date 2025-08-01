package es.musicalia.gestmusica.localizacion;

import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProvinciaRepository extends JpaRepository<Provincia, Long> {

    @Query("select new es.musicalia.gestmusica.generic.CodigoNombreRecord(p.id, p.nombre) from Provincia p where p.ccaa.id = ?1")
    List<CodigoNombreRecord> findProvinciaByIdCcaa(long idCcaa);

    @Query("select new es.musicalia.gestmusica.generic.CodigoNombreRecord(p.id, p.nombre) from Provincia p order by p.nombre asc")
    List<CodigoNombreRecord> findProvinciasOrderByName();

    @Query("select p from Provincia p where upper(p.nombre) = upper(?1)")
    Optional<Provincia> findProvinciaByNombreUpperCase(String nombre);

    @Query("select p from Provincia p where p.idProvinciaLegacy = ?1")
    Optional<Provincia> findProvinciaByIdProvinciaLegacy(Integer idProvinciaLegacy);

}
