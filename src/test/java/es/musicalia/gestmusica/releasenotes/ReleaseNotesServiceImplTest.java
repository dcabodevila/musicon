package es.musicalia.gestmusica.releasenotes;

import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReleaseNotesServiceImplTest {

    @Mock
    private ReleaseNotesReadRepository releaseNotesReadRepository;
    @Mock
    private UserService userService;

    private ReleaseNotesServiceImpl releaseNotesService;

    @BeforeEach
    void setUp() {
        releaseNotesService = new ReleaseNotesServiceImpl(
                releaseNotesReadRepository,
                userService,
                new DefaultResourceLoader(),
                new ReleaseNotesVersionPolicy()
        );
    }

    @Test
    void hasReadReleaseNotes_patchUpgradeNoDebeMostrarModalSiYaLeyeronMajorMinor() {
        when(releaseNotesReadRepository.existsByUsuarioIdAndVersion(10L, "1.2.0")).thenReturn(false);
        when(releaseNotesReadRepository.existsByUsuarioIdAndVersionStartingWith(10L, "1.2.")).thenReturn(true);

        boolean hasRead = releaseNotesService.hasReadReleaseNotes(10L, "1.2.1");

        assertTrue(hasRead);
    }

    @Test
    void hasReadReleaseNotes_cambioMajorMinorPuedeMostrarNovedades() {
        when(releaseNotesReadRepository.existsByUsuarioIdAndVersion(10L, "1.3.0")).thenReturn(false);
        when(releaseNotesReadRepository.existsByUsuarioIdAndVersionStartingWith(10L, "1.3.")).thenReturn(false);

        boolean hasRead = releaseNotesService.hasReadReleaseNotes(10L, "1.3.0");

        assertFalse(hasRead);
    }

    @Test
    void markAsRead_debePersistirSiempreVersionEfectiva() {
        when(releaseNotesReadRepository.existsByUsuarioIdAndVersion(10L, "1.2.0")).thenReturn(false);
        when(releaseNotesReadRepository.existsByUsuarioIdAndVersionStartingWith(10L, "1.2.")).thenReturn(false);

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        when(userService.findById(10L)).thenReturn(usuario);

        releaseNotesService.markAsRead(10L, "1.2.9");

        ArgumentCaptor<ReleaseNotesRead> captor = ArgumentCaptor.forClass(ReleaseNotesRead.class);
        verify(releaseNotesReadRepository).save(captor.capture());
        assertEquals("1.2.0", captor.getValue().getVersion());
    }

    @Test
    void markAsRead_debeRechazarVersionInvalida() {
        assertThrows(IllegalArgumentException.class, () -> releaseNotesService.markAsRead(10L, "v1.2.3"));
        verify(releaseNotesReadRepository, never()).save(any());
    }

    @Test
    void getReleaseNotesContent_debeCargarMarkdownPorVersionEfectiva() {
        String html = releaseNotesService.getReleaseNotesContent("1.2.1");
        assertTrue(html.contains("Sincronización con OrquestasDeGalicia.es"));
    }

    @Test
    void getCurrentEffectiveVersion_debeNormalizarVersionActual() {
        ReflectionTestUtils.setField(releaseNotesService, "currentVersion", "1.2.1");

        assertEquals("1.2.0", releaseNotesService.getCurrentEffectiveVersion().orElseThrow());
    }

    @Test
    void hasReadReleaseNotes_debeSoportarDatasetHistoricoPatchLevel() {
        when(releaseNotesReadRepository.existsByUsuarioIdAndVersion(eq(33L), eq("1.2.0"))).thenReturn(false);
        when(releaseNotesReadRepository.existsByUsuarioIdAndVersionStartingWith(eq(33L), eq("1.2."))).thenReturn(true);

        assertTrue(releaseNotesService.hasReadReleaseNotes(33L, "1.2.0"));
        verify(releaseNotesReadRepository).existsByUsuarioIdAndVersionStartingWith(33L, "1.2.");
    }
}
