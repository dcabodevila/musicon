package es.musicalia.gestmusica.acceso;

import es.musicalia.gestmusica.accesoartista.AccesoArtista;
import es.musicalia.gestmusica.accesoartista.AccesoArtistaRepository;
import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.agencia.AgenciaRepository;
import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.permiso.Permiso;
import es.musicalia.gestmusica.permiso.PermisoRepository;
import es.musicalia.gestmusica.permiso.TipoPermisoEnum;
import es.musicalia.gestmusica.rol.Rol;
import es.musicalia.gestmusica.rol.RolRepository;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccesoServiceImplTest {

    private static final long ID_USUARIO = 7L;
    private static final long ID_AGENCIA = 96L;
    private static final long ID_ROL = 3L;
    private static final long ID_ARTISTA = 198L;
    private static final long ID_PERMISO = 11L;

    @Mock private AccesoRepository accesoRepository;
    @Mock private RolRepository rolRepository;
    @Mock private PermisoRepository permisoRepository;
    @Mock private AgenciaRepository agenciaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AccesoArtistaRepository accesoArtistaRepository;
    @Mock private ArtistaRepository artistaRepository;
    @Mock private AccesoMapper accesoMapper;

    private AccesoServiceImpl accesoService;

    @BeforeEach
    void setUp() {
        accesoService = new AccesoServiceImpl(accesoRepository, rolRepository, permisoRepository, agenciaRepository,
                usuarioRepository, accesoArtistaRepository, artistaRepository, accesoMapper);

        Permiso permiso = new Permiso();
        permiso.setId(ID_PERMISO);
        Artista artista = new Artista();
        artista.setId(ID_ARTISTA);
        lenient().when(permisoRepository.findAllPermisosByIdRolAndTipoPermiso(ID_ROL, TipoPermisoEnum.ARTISTA.getId()))
                .thenReturn(Optional.of(Set.of(permiso)));
        lenient().when(artistaRepository.findAllArtistasByIdAgencia(ID_AGENCIA)).thenReturn(List.of(artista));
        lenient().when(accesoArtistaRepository.findAllAccesosByIdArtistaIdUsuarioIdPermiso(anyLong(), anyLong(), anyLong()))
                .thenReturn(List.of());
    }

    @Test
    void guardarPermisosArtistas_revocaLosPermisosAntiguosDelUsuarioEnLosArtistasDeSuAgencia() {
        accesoService.guardarPermisosArtistas(acceso(), null, true);

        // Old permisos are looked up in the artistas of the acceso's agencia, for the acceso's usuario.
        verify(artistaRepository, never()).findAllArtistasByIdAgencia(ID_USUARIO);
        verify(accesoArtistaRepository, never()).findAllAccesosByIdArtistaIdUsuarioIdPermiso(anyLong(), eq(ID_AGENCIA), anyLong());
        verify(accesoArtistaRepository, org.mockito.Mockito.atLeastOnce())
                .findAllAccesosByIdArtistaIdUsuarioIdPermiso(ID_ARTISTA, ID_USUARIO, ID_PERMISO);
    }

    @Test
    void guardarPermisosArtistas_sinEliminarAntiguosSoloConcedeLosPermisos() {
        accesoService.guardarPermisosArtistas(acceso(), null, false);

        verify(accesoArtistaRepository, never()).deleteAll(org.mockito.ArgumentMatchers.<Iterable<AccesoArtista>>any());
        verify(accesoArtistaRepository).findAllAccesosByIdArtistaIdUsuarioIdPermiso(ID_ARTISTA, ID_USUARIO, ID_PERMISO);
    }

    private static Acceso acceso() {
        Usuario usuario = new Usuario();
        usuario.setId(ID_USUARIO);
        Agencia agencia = new Agencia();
        agencia.setId(ID_AGENCIA);
        Rol rol = new Rol();
        rol.setId(ID_ROL);
        Acceso acceso = new Acceso();
        acceso.setUsuario(usuario);
        acceso.setAgencia(agencia);
        acceso.setRol(rol);
        return acceso;
    }
}
