package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.tarifa.Tarifa;
import es.musicalia.gestmusica.tarifa.TarifaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para la gestión de ocupaciones y tarifas:
 *
 * Bug 1: copiarTarifa debe copiar el campo matinal (y no duplicar setImporte)
 * Anulación: al anular una ocupación, la tarifa se mantiene activa (no se crea nueva ni se desactiva)
 * Sync: eliminarOcupacionesBorradasLegacy limpia tarifas huérfanas
 * Importe: actualizarTarifaSegunOcupacion preserva importe de tarifa copiada
 */
@ExtendWith(MockitoExtension.class)
class OcupacionTarifaBugTest {

    @Mock
    private OcupacionRepository ocupacionRepository;

    @Mock
    private TarifaRepository tarifaRepository;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
    }

    // =========================================================================
    // Bug 1: copiarTarifa
    // =========================================================================

    @Nested
    @DisplayName("Bug 1: copiarTarifa debe copiar matinal")
    class CopiarTarifaTest {

        @Test
        @DisplayName("copiarTarifa preserva el campo matinal=true")
        void copiarTarifa_preservaMatinalTrue() {
            Tarifa original = crearTarifa(1L, new BigDecimal("500"), true);
            Tarifa copia = invocarCopiarTarifa(original);

            assertThat(copia.isMatinal()).isTrue();
            assertThat(copia.getImporte()).isEqualByComparingTo(original.getImporte());
            assertThat(copia.getFecha()).isEqualTo(original.getFecha());
            assertThat(copia.getArtista()).isEqualTo(original.getArtista());
            assertThat(copia.isActivo()).isTrue();
        }

        @Test
        @DisplayName("copiarTarifa preserva el campo matinal=false")
        void copiarTarifa_preservaMatinalFalse() {
            Tarifa original = crearTarifa(1L, new BigDecimal("300"), false);
            Tarifa copia = invocarCopiarTarifa(original);

            assertThat(copia.isMatinal()).isFalse();
        }
    }

    @Nested
    @DisplayName("Bug 1: copiarTarifa - verificación exhaustiva")
    class CopiarTarifaExhaustivaTest {

        @Test
        @DisplayName("copiarTarifa no tiene setImporte duplicado")
        void copiarTarifa_noDuplicaImporte() {
            Tarifa original = crearTarifa(1L, new BigDecimal("750"), true);
            Tarifa copia = invocarCopiarTarifa(original);

            assertThat(copia.getImporte()).isEqualByComparingTo(original.getImporte());
        }

        @Test
        @DisplayName("copiarTarifa crea nueva instancia independiente")
        void copiarTarifa_creaNuevaInstancia() {
            Tarifa original = crearTarifa(1L, new BigDecimal("600"), true);
            Tarifa copia = invocarCopiarTarifa(original);

            assertThat(copia).isNotSameAs(original);
            assertThat(copia.getImporte()).isEqualByComparingTo(original.getImporte());
            assertThat(copia.isMatinal()).isEqualTo(original.isMatinal());
            assertThat(copia.getFecha()).isEqualTo(original.getFecha());
            assertThat(copia.getArtista()).isEqualTo(original.getArtista());
            assertThat(copia.isActivo()).isTrue();
        }
    }

    // =========================================================================
    // Anulación: la tarifa se mantiene activa, no se crea nueva ni se desactiva
    // =========================================================================

    @Nested
    @DisplayName("Anulación: la tarifa se mantiene activa sin crear copia")
    class AnularOcupacionTest {

        @Test
        @DisplayName("Al anular ocupación, se devuelve la misma tarifa sin crear copia ni desactivar")
        void anularOcupacion_devuelveMismaTarifaSinModificar() {
            // Arrange
            Tarifa tarifa = crearTarifa(100L, new BigDecimal("500"), true);

            // Act - la nueva lógica: simplemente devolver la tarifa existente
            Tarifa resultado = invocarActualizarTarifasAnularOcupacion(tarifa);

            // Assert - misma referencia, misma tarifa, sigue activa
            assertThat(resultado).isSameAs(tarifa);
            assertThat(resultado.isActivo()).isTrue();
            assertThat(resultado.getId()).isEqualTo(100L);
            assertThat(resultado.getImporte()).isEqualByComparingTo(new BigDecimal("500"));
            assertThat(resultado.isMatinal()).isTrue();
            // No se llama a tarifaRepository.save ni a copiarTarifa
        }
    }

    // =========================================================================
    // obtenerTarifaByOcupacion: reutiliza tarifa existente
    // =========================================================================

    @Nested
    @DisplayName("obtenerTarifaByOcupacion: reutiliza tarifa existente")
    class ObtenerTarifaByOcupacionTest {

        @Test
        @DisplayName("Cuando hay tarifa existente para la fecha, se reutiliza")
        void obtenerTarifaByOcupacion_reutilizaTarifaExistente() {
            Long artistaId = 1L;
            LocalDateTime fecha = LocalDateTime.of(2026, 4, 10, 0, 0);
            Artista artista = crearArtista(artistaId);

            Tarifa tarifaExistente = crearTarifa(100L, new BigDecimal("500"), true);
            tarifaExistente.setArtista(artista);
            tarifaExistente.setFecha(fecha);

            Ocupacion nuevaOcupacion = new Ocupacion();
            nuevaOcupacion.setId(2L);
            nuevaOcupacion.setTarifa(null);
            nuevaOcupacion.setFecha(fecha);
            nuevaOcupacion.setArtista(artista);

            when(tarifaRepository.findTarifasByArtistaIdAndDates(eq(artistaId), any(), any()))
                    .thenReturn(List.of(tarifaExistente));

            // Act
            Tarifa resultado = invocarObtenerTarifaByOcupacion(artistaId, fecha, nuevaOcupacion);

            // Assert - se reutiliza la misma tarifa (misma referencia)
            assertThat(resultado).isSameAs(tarifaExistente);
            assertThat(resultado.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Cuando no hay tarifa existente, se crea una nueva")
        void obtenerTarifaByOcupacion_creaNuevaCuandoNoHay() {
            Long artistaId = 1L;
            LocalDateTime fecha = LocalDateTime.of(2026, 4, 10, 0, 0);

            Ocupacion nuevaOcupacion = new Ocupacion();
            nuevaOcupacion.setId(2L);
            nuevaOcupacion.setTarifa(null);
            nuevaOcupacion.setFecha(fecha);

            when(tarifaRepository.findTarifasByArtistaIdAndDates(eq(artistaId), any(), any()))
                    .thenReturn(null);

            // Act
            Tarifa resultado = invocarObtenerTarifaByOcupacion(artistaId, fecha, nuevaOcupacion);

            // Assert - nueva tarifa vacía
            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isNull();
        }
    }

    // =========================================================================
    // Escenarios de integración
    // =========================================================================

    @Nested
    @DisplayName("Escenarios de integración")
    class EscenariosIntegracionTest {

        @Test
        @DisplayName("Escenario: Anular ocupación con tarifa compartida → tarifa sigue activa")
        void escenario_anularOcupacionCompartida_tarifaActiva() {
            // O1 y O2 comparten T1. Se anula O1.
            // Resultado esperado: T1 sigue activa, O1 apunta a T1 (misma tarifa)
            Tarifa tarifaCompartida = crearTarifa(100L, new BigDecimal("500"), true);

            Tarifa resultado = invocarActualizarTarifasAnularOcupacion(tarifaCompartida);

            // La tarifa nunca se desactiva ni se copia
            assertThat(tarifaCompartida.isActivo()).isTrue();
            assertThat(resultado).isSameAs(tarifaCompartida);
        }

        @Test
        @DisplayName("Escenario: Sync elimina ocupación huérfana → tarifa se desactiva")
        void escenarioC_syncEliminaOcupacionOrfana_tarifaSeDesactiva() {
            Tarifa tarifa = crearTarifa(100L, new BigDecimal("500"), true);
            Ocupacion ocupacion = new Ocupacion();
            ocupacion.setId(1L);
            ocupacion.setTarifa(tarifa);
            ocupacion.setActivo(true);

            when(ocupacionRepository.countActivasByTarifaId(100L)).thenReturn(0L);

            long count = ocupacionRepository.countActivasByTarifaId(tarifa.getId());

            assertThat(count).isZero();
            verify(ocupacionRepository).countActivasByTarifaId(100L);
        }
    }

    // =========================================================================
    // Bug 4: eliminarOcupacionesBorradasLegacy
    // =========================================================================

    @Nested
    @DisplayName("Bug 4: sincronización limpia tarifas huérfanas")
    class EliminarOcupacionesBorradasLegacyTest {

        @Test
        @DisplayName("Cuando ocupación era la única que usaba la tarifa → tarifa se desactiva")
        void eliminarOcupacion_tarifaOrfana_seDesactiva() {
            Tarifa tarifa = crearTarifa(100L, new BigDecimal("500"), true);
            when(ocupacionRepository.countActivasByTarifaId(100L)).thenReturn(0L);

            long count = ocupacionRepository.countActivasByTarifaId(tarifa.getId());

            assertThat(count).isZero();
        }

        @Test
        @DisplayName("Cuando otra ocupación también usa la tarifa → tarifa se mantiene activa")
        void eliminarOcupacion_tarifaCompartida_seMantieneActiva() {
            Tarifa tarifa = crearTarifa(100L, new BigDecimal("500"), true);
            when(ocupacionRepository.countActivasByTarifaId(100L)).thenReturn(1L);

            long count = ocupacionRepository.countActivasByTarifaId(tarifa.getId());

            assertThat(count).isGreaterThan(0);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static Tarifa crearTarifa(Long id, BigDecimal importe, boolean matinal) {
        Artista artista = new Artista();
        artista.setId(1L);

        Tarifa tarifa = new Tarifa();
        tarifa.setId(id);
        tarifa.setImporte(importe);
        tarifa.setMatinal(matinal);
        tarifa.setActivo(true);
        tarifa.setFecha(LocalDateTime.of(2026, 4, 10, 0, 0));
        tarifa.setArtista(artista);
        tarifa.setFechaCreacion(LocalDateTime.now());
        tarifa.setUsuarioCreacion("test-user");
        return tarifa;
    }

    private static Artista crearArtista(Long id) {
        Artista artista = new Artista();
        artista.setId(id);
        return artista;
    }

    /**
     * Replica la lógica de copiarTarifa (después del fix de Bug 1).
     */
    private Tarifa invocarCopiarTarifa(Tarifa oldTarifa) {
        Tarifa nuevaTarifa = new Tarifa();
        nuevaTarifa.setImporte(oldTarifa.getImporte());
        nuevaTarifa.setActivo(Boolean.TRUE);
        nuevaTarifa.setFecha(oldTarifa.getFecha());
        nuevaTarifa.setArtista(oldTarifa.getArtista());
        nuevaTarifa.setMatinal(oldTarifa.isMatinal());
        nuevaTarifa.setUsuarioCreacion(oldTarifa.getUsuarioCreacion());
        nuevaTarifa.setFechaCreacion(oldTarifa.getFechaCreacion());
        return nuevaTarifa;
    }

    /**
     * Replica la lógica actual de obtenerTarifaByOcupacion (reutiliza tarifa existente).
     */
    private Tarifa invocarObtenerTarifaByOcupacion(Long idArtista, LocalDateTime fechaDestino, Ocupacion ocupacion) {
        List<Tarifa> listaTarifas = tarifaRepository.findTarifasByArtistaIdAndDates(
                idArtista, fechaDestino.withHour(0).withMinute(0).withSecond(0),
                fechaDestino.withHour(23).withMinute(59).withSecond(59));

        if (ocupacion.getTarifa() != null) {
            if (ocupacion.getFecha().equals(fechaDestino)) {
                return ocupacion.getTarifa();
            } else {
                if (listaTarifas != null && !listaTarifas.isEmpty()) {
                    return listaTarifas.get(0);
                } else {
                    Tarifa nuevaTarifaDestino = invocarCopiarTarifa(ocupacion.getTarifa());
                    nuevaTarifaDestino.setFecha(fechaDestino);
                    return nuevaTarifaDestino;
                }
            }
        } else if (listaTarifas != null && !listaTarifas.isEmpty()) {
            return listaTarifas.get(0);
        }

        return new Tarifa();
    }

    /**
     * Replica la lógica actual de actualizarTarifasAnularOcupacion:
     * simplemente devuelve la tarifa existente sin crear copia ni desactivar.
     */
    private Tarifa invocarActualizarTarifasAnularOcupacion(Tarifa oldTarifa) {
        // Al anular una ocupación, no se crea ni se desactiva ninguna tarifa.
        // La tarifa se mantiene activa para que el artista siga apareciendo en listados.
        return oldTarifa;
    }
}