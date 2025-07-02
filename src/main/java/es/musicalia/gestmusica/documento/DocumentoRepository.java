package es.musicalia.gestmusica.documento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {

	Optional<List<Documento>> findByArtistaIdAndActivoTrue(Long artistaId);

}
