package es.musicalia.gestmusica.home;

import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.listado.ListadoRepository;
import es.musicalia.gestmusica.ocupacion.OcupacionRepository;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeEstadisticasServiceImplTest {

    @Mock
    private ArtistaRepository artistaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ListadoRepository listadoRepository;
    @Mock
    private OcupacionRepository ocupacionRepository;

    @InjectMocks
    private HomeEstadisticasServiceImpl service;

    @Test
    void obtenerEstadisticas_mapeaCadaContadorGlobalYUsaUltimos30Dias() {
        when(artistaRepository.countByActivoTrue()).thenReturn(80L);
        when(usuarioRepository.countByActivoTrueAndValidadoTrue()).thenReturn(1234L);
        when(listadoRepository.countByActivoTrueAndFechaCreacionGreaterThanEqual(any())).thenReturn(410L);
        when(ocupacionRepository.countByActivoTrueAndFechaCreacionGreaterThanEqual(any())).thenReturn(96L);

        LocalDateTime antes = LocalDateTime.now().minusDays(30).minusMinutes(1);
        HomeEstadisticasDto resultado = service.obtenerEstadisticas();
        LocalDateTime despues = LocalDateTime.now().minusDays(30).plusMinutes(1);

        assertThat(resultado).isEqualTo(new HomeEstadisticasDto(80L, 1234L, 410L, 96L));
        verify(usuarioRepository).countByActivoTrueAndValidadoTrue();

        ArgumentCaptor<LocalDateTime> corteListados = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> corteOcupaciones = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(listadoRepository).countByActivoTrueAndFechaCreacionGreaterThanEqual(corteListados.capture());
        verify(ocupacionRepository).countByActivoTrueAndFechaCreacionGreaterThanEqual(corteOcupaciones.capture());
        assertThat(corteListados.getValue()).isBetween(antes, despues);
        assertThat(corteOcupaciones.getValue()).isBetween(antes, despues);
    }
}
