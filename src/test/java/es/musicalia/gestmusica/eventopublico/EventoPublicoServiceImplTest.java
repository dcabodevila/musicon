package es.musicalia.gestmusica.eventopublico;

import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.ocupacion.Ocupacion;
import es.musicalia.gestmusica.ocupacion.OcupacionEstado;
import es.musicalia.gestmusica.ocupacion.OcupacionEstadoEnum;
import es.musicalia.gestmusica.ocupacion.OcupacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoPublicoServiceImplTest {

    @Mock
    private OcupacionRepository ocupacionRepository;

    private EventoPublicoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EventoPublicoServiceImpl(ocupacionRepository);
    }

    @Test
    void obtenerEventosRelacionadosPublicos_debeExcluirActualRespetarVentanaYOrdenarPorFecha() {
        LocalDate fechaDesde = LocalDate.of(2026, 8, 15);
        LocalDate fechaHasta = LocalDate.of(2026, 8, 20);

        when(ocupacionRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
            crearOcupacion(10L, 20L, LocalDateTime.of(2026, 8, 16, 22, 0), "Los Satélites", "Lugo", "Lugo"),
            crearOcupacion(11L, 20L, LocalDateTime.of(2026, 8, 18, 22, 0), "Los Satélites", "Sarria", "Lugo"),
            crearOcupacion(12L, 20L, LocalDateTime.of(2026, 8, 14, 22, 0), "Los Satélites", "Monforte", "Lugo"),
            crearOcupacion(13L, 20L, LocalDateTime.of(2026, 8, 17, 22, 0), "Los Satélites", "Viveiro", "Lugo")
        ));

        List<EventoPublicoDto> relacionados = service.obtenerEventosRelacionadosPublicos(10L, 20L, fechaDesde, fechaHasta, 2);

        assertEquals(List.of(13L, 11L), relacionados.stream().map(EventoPublicoDto::getId).toList());
        verify(ocupacionRepository).findAll(any(Specification.class), eq(Sort.by(Sort.Direction.ASC, "fecha", "artista.nombre")));
    }

    private Ocupacion crearOcupacion(Long id, Long idArtista, LocalDateTime fecha, String artistaNombre, String municipioNombre, String provinciaNombre) {
        Artista artista = new Artista();
        artista.setId(idArtista);
        artista.setNombre(artistaNombre);
        artista.setActivo(true);
        artista.setPublicarEventos(true);

        Provincia provincia = new Provincia();
        provincia.setNombre(provinciaNombre);

        Municipio municipio = new Municipio();
        municipio.setNombre(municipioNombre);
        municipio.setProvincia(provincia);

        OcupacionEstado estado = new OcupacionEstado();
        estado.setId(OcupacionEstadoEnum.OCUPADO.getId());

        Ocupacion ocupacion = new Ocupacion();
        ocupacion.setId(id);
        ocupacion.setArtista(artista);
        ocupacion.setProvincia(provincia);
        ocupacion.setMunicipio(municipio);
        ocupacion.setOcupacionEstado(estado);
        ocupacion.setActivo(true);
        ocupacion.setEventoVisible(true);
        ocupacion.setFecha(fecha);
        ocupacion.setFechaCreacion(fecha.minusDays(1));
        return ocupacion;
    }
}
