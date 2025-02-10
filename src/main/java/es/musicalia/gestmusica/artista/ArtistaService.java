package es.musicalia.gestmusica.artista;

import es.musicalia.gestmusica.localizacion.CodigoNombreDto;
import es.musicalia.gestmusica.usuario.Usuario;

import java.util.List;

public interface ArtistaService {
    List<ArtistaDto> findAllArtistasForUser(final Usuario usuario);
    Artista saveArtista(ArtistaDto agenciaDto);
    ArtistaDto findArtistaDtoById(Long idArtista);
    List<CodigoNombreDto> listaTipoEscenario();
    List<CodigoNombreDto> listaTipoArtista();

    List<ArtistaDto> findAllArtistasByAgenciaId(final Long idAgencia);

    List<ArtistaRecord> listaArtistaRecordByIdAgencia(Long idAgencia);
}
