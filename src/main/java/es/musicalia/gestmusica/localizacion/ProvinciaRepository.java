package es.musicalia.gestmusica.localizacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProvinciaRepository extends JpaRepository<Provincia, Long> {

    @Query("select p from Provincia p where p.ccaa.id = ?1")
    List<Provincia> findProvinciaByIdCcaa(long idCcaa);

    @Query("select p from Provincia p order by p.nombre asc")
    List<Provincia> findProvinciasOrderByName();


    @Query("select m from Municipio m where m.provincia.id = ?1")
    List<Municipio> findMunicipiosByIdProvincia(long idProvincia);

}
