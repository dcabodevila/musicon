package es.musicalia.gestmusica.artista;

import es.musicalia.gestmusica.localizacion.CodigoNombreDto;
import es.musicalia.gestmusica.usuario.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface ArtistaService {
    Page<ArtistaDto> findAllArtistasForUserPaginated(Usuario usuario, Pageable pageable);

    List<ArtistaRecord> findAllArtistasForUser(final Usuario usuario);
    Artista saveArtista(ArtistaDto agenciaDto);
    ArtistaDto findArtistaDtoById(Long idArtista);
    List<CodigoNombreDto> listaTipoEscenario();
    List<CodigoNombreDto> listaTipoArtista();

    List<ArtistaRecord> findMisArtistas(Set<Long> idsMisArtistas);

    Page<ArtistaRecord> findMisArtistasPaginated(Set<Long> idsMisArtistas, Pageable pageable);

    List<ArtistaRecord> findOtrosArtistas(Set<Long> idsMisArtistas);

    List<ArtistaDto> findAllArtistasByAgenciaId(final Long idAgencia);

    List<ArtistaRecord> listaArtistaRecordByIdAgencia(Long idAgencia);

    List<ArtistaRecord> findArtistasRecordByIdAgencia(Long idAgencia);
}
