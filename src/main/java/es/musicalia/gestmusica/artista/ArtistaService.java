package es.musicalia.gestmusica.artista;

import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.tipoartista.TipoArtista;
import es.musicalia.gestmusica.tipoescenario.TipoEscenario;
import es.musicalia.gestmusica.usuario.Usuario;

import java.util.List;

public interface ArtistaService {
    List<ArtistaDto> findAllArtistasForUser(final Usuario usuario);
    Artista saveArtista(ArtistaDto agenciaDto);
    ArtistaDto findArtistaDtoById(Long idArtista);
    List<TipoEscenario> listaTipoEscenario();
    List<TipoArtista> listaTipoArtista();

    List<ArtistaDto> findAllArtistasByAgenciaId(final Long idAgencia);

}
