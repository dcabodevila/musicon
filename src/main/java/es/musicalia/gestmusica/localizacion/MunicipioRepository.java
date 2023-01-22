package es.musicalia.gestmusica.localizacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MunicipioRepository extends JpaRepository<Municipio, Long> {

    @Query("select m from Municipio m where m.provincia.id = ?1")
    List<Municipio> findMunicipioByProvinciaId(long idProvincia);

}
