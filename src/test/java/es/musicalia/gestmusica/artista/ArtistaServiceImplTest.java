package es.musicalia.gestmusica.artista;

import es.musicalia.gestmusica.acceso.AccesoService;
import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.agencia.AgenciaRepository;
import es.musicalia.gestmusica.contacto.Contacto;
import es.musicalia.gestmusica.contacto.ContactoRepository;
import es.musicalia.gestmusica.localizacion.Ccaa;
import es.musicalia.gestmusica.localizacion.CcaaRepository;
import es.musicalia.gestmusica.mail.EmailService;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.tipoartista.TipoArtista;
import es.musicalia.gestmusica.tipoartista.TipoArtistaRepository;
import es.musicalia.gestmusica.tipoescenario.TipoEscenarioRepository;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtistaServiceImplTest {

    @Mock
    private ArtistaRepository artistaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ContactoRepository agenciaContactoRepository;
    @Mock
    private TipoEscenarioRepository tipoEscenarioRepository;
    @Mock
    private TipoArtistaRepository tipoArtistaRepository;
    @Mock
    private CcaaRepository ccaaRepository;
    @Mock
    private AgenciaRepository agenciaRepository;
    @Mock
    private AccesoService accesoService;
    @Mock
    private ArtistaMapper artistaMapper;
    @Mock
    private EmailService emailService;
    @Mock
    private MensajeService mensajeService;
    @Mock
    private UserService userService;

    @InjectMocks
    private ArtistaServiceImpl artistaService;

    @Test
    @DisplayName("saveArtista reemplaza los tipos de artista eliminando los desmarcados")
    void saveArtistaReemplazaTiposArtistaExistentes() {
        Long artistaId = 7L;
        Long tipoMantenerId = 2L;
        Long tipoEliminarId = 1L;
        Long ccaaId = 3L;
        Long agenciaId = 4L;
        Long usuarioId = 5L;

        TipoArtista tipoEliminar = crearTipoArtista(tipoEliminarId, "Orquesta");
        TipoArtista tipoMantener = crearTipoArtista(tipoMantenerId, "Dúo");

        Artista artistaExistente = new Artista();
        artistaExistente.setId(artistaId);
        artistaExistente.setTiposArtista(new HashSet<>(Set.of(tipoEliminar, tipoMantener)));
        artistaExistente.setComunidadesTrabajo(new HashSet<>());
        artistaExistente.setContacto(new Contacto());

        ArtistaDto dto = new ArtistaDto();
        dto.setId(artistaId);
        dto.setNombre("Artista Test");
        dto.setIdUsuario(usuarioId);
        dto.setIdCcaa(ccaaId);
        dto.setIdAgencia(agenciaId);
        dto.setIdsTipoArtista(List.of(tipoMantenerId));
        dto.setIdsComunidadesTrabajo(List.of(ccaaId));

        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        Ccaa ccaa = new Ccaa();
        ccaa.setId(ccaaId);
        Agencia agencia = new Agencia();
        agencia.setId(agenciaId);

        when(artistaRepository.findById(artistaId)).thenReturn(Optional.of(artistaExistente));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(ccaaRepository.findById(ccaaId)).thenReturn(Optional.of(ccaa));
        when(agenciaRepository.findById(agenciaId)).thenReturn(Optional.of(agencia));
        when(tipoArtistaRepository.findById(tipoMantenerId)).thenReturn(Optional.of(tipoMantener));
        when(agenciaContactoRepository.save(any(Contacto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(artistaRepository.save(any(Artista.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Artista artistaGuardado = artistaService.saveArtista(dto);

        assertThat(artistaGuardado.getTiposArtista())
                .extracting(TipoArtista::getId)
                .containsExactly(tipoMantenerId);
        verify(tipoArtistaRepository).findById(tipoMantenerId);
    }

    private TipoArtista crearTipoArtista(Long id, String nombre) {
        TipoArtista tipoArtista = new TipoArtista();
        tipoArtista.setId(id);
        tipoArtista.setNombre(nombre);
        return tipoArtista;
    }
}
