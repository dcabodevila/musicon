package es.musicalia.gestmusica.localizacion;


import es.musicalia.gestmusica.generic.CodigoNombreRecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CcaaRepository extends JpaRepository<Ccaa, Long> {
    @Query("select new es.musicalia.gestmusica.generic.CodigoNombreRecord(a.id, a.nombre) from Ccaa a order by a.nombre")
    List<CodigoNombreRecord> findAllCcaaOrderedByName();

}
