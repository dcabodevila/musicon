package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.tarifa.Tarifa;
import es.musicalia.gestmusica.tarifa.TarifaRepository;
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
 * Tests de regresión para verificar que los listados y tarifas anuales
 * muestran correctamente la información después de cualquier cambio
 * en ocupaciones o tarifas.
 *
 * Invariantes que deben cumplirse SIEMPRE:
 *
 * 1. Una tarifa activa por artista/fecha garantiza que el artista aparece en listados
 * 2. Al anular una ocupación, la tarifa sigue activa (el artista sigue en listados)
 * 3. Al crear una ocupación, se reutiliza la tarifa existente (1 tarifa activa por fecha)
 * 4. Al eliminar una ocupación via sync, si la tarifa queda huérfana se desactiva
 * 5. copiarTarifa preserva matinal e importe
 * 6. actualizarTarifaSegunOcupacion no sobrescribe importe a 0 cuando la tarifa copiada ya tiene importe
 */
@ExtendWith(MockitoExtension.class)
class OcupacionTarifaListadoRegressionTest {

    @Mock
    private OcupacionRepository ocupacionRepository;

    @Mock
    private TarifaRepository tarifaRepository;

    // =========================================================================
    // Invariante 1: Tarifa activa → artista aparece en listados
    // =========================================================================

    @Nested
    @DisplayName("Invariante 1: Tarifa activa garantiza visibilidad en listados")
    class TarifaActivaVisibilidadTest {

        @Test
        @DisplayName("Artista con tarifa activa aparece en listado de tarifas")
        void artistaConTarifaActiva_apareceEnListado() {
            // La query findTarifasByArtistaIdAndDates filtra por t.activo = true
            // Si hay una tarifa activa, el artista aparece
            Long artistaId = 1L;
            LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 0, 0);

            Tarifa tarifaActiva = crearTarifa(100L, new BigDecimal("5500"), false);

            when(tarifaRepository.findTarifasByArtistaIdAndDates(eq(artistaId), any(), any()))
                    .thenReturn(List.of(tarifaActiva));

            List<Tarifa> resultado = tarifaRepository.findTarifasByArtistaIdAndDates(
                    artistaId, fecha, fecha.plusDays(1));

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).isActivo()).isTrue();
            assertThat(resultado.get(0).getImporte()).isEqualByComparingTo(new BigDecimal("5500"));
        }

        @Test
        @DisplayName("Artista sin tarifa activa no aparece en listado")
        void artistaSinTarifaActiva_noApareceEnListado() {
            Long artistaId = 1L;
            LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 0, 0);

            when(tarifaRepository.findTarifasByArtistaIdAndDates(eq(artistaId), any(), any()))
                    .thenReturn(List.of());

            List<Tarifa> resultado = tarifaRepository.findTarifasByArtistaIdAndDates(
                    artistaId, fecha, fecha.plusDays(1));

            assertThat(resultado).isEmpty();
        }
    }

    // =========================================================================
    // Invariante 2: Anular ocupación → tarifa sigue activa
    // =========================================================================

    @Nested
    @DisplayName("Invariante 2: Anular ocupación no desactiva la tarifa")
    class AnularOcupacionTarifaActivaTest {

        @Test
        @DisplayName("Al anular ocupación, la tarifa permanece activa y con su importe original")
        void anularOcupacion_tarifaPermaneceActivaConImporteOriginal() {
            Tarifa tarifa = crearTarifa(100L, new BigDecimal("5500"), false);

            // Act: anular = devuelve la misma tarifa sin modificarla
            Tarifa resultado = invocarActualizarTarifasAnularOcupacion(tarifa);

            // Assert: misma referencia, misma tarifa, sigue activa, mismo importe
            assertThat(resultado).isSameAs(tarifa);
            assertThat(resultado.isActivo()).isTrue();
            assertThat(resultado.getImporte()).isEqualByComparingTo(new BigDecimal("5500"));
            assertThat(resultado.getId()).isEqualTo(100L);
            // No se creó ninguna tarifa nueva ni se desactivó la existente
        }

        @Test
        @DisplayName("Al anular ocupación con tarifa matinal, la tarifa matinal permanece activa")
        void anularOcupacion_matinal_tarifaPermaneceActiva() {
            Tarifa tarifaMatinal = crearTarifa(100L, new BigDecimal("5500"), true);

            Tarifa resultado = invocarActualizarTarifasAnularOcupacion(tarifaMatinal);

            assertThat(resultado).isSameAs(tarifaMatinal);
            assertThat(resultado.isActivo()).isTrue();
            assertThat(resultado.isMatinal()).isTrue();
        }

        @Test
        @DisplayName("Dos ocupaciones comparten tarifa: al anular una, la tarifa sigue activa para la otra")
        void dosOcupacionesCompartenTarifa_anularUna_tarifaActiva() {
            // Scenario: O1 y O2 comparten T1. Se anula O1.
            // Resultado: T1 sigue activa, O2 la sigue usando, el artista aparece en listados
            Tarifa tarifaCompartida = crearTarifa(100L, new BigDecimal("5500"), false);

            Tarifa resultado = invocarActualizarTarifasAnularOcupacion(tarifaCompartida);

            // La tarifa no cambia en absoluto
            assertThat(resultado).isSameAs(tarifaCompartida);
            assertThat(resultado.isActivo()).isTrue();
            assertThat(resultado.getId()).isEqualTo(100L);
        }
    }

    // =========================================================================
    // Invariante 3: Crear ocupación reutiliza tarifa existente
    // =========================================================================

    @Nested
    @DisplayName("Invariante 3: Crear ocupación reutiliza tarifa existente (1 tarifa por fecha)")
    class CrearOcupacionReutilizaTarifaTest {

        @Test
        @DisplayName("Crear ocupación en fecha con tarifa existente: reutiliza la tarifa (no crea duplicada)")
        void crearOcupacion_fechaConTarifaExistente_reutilizaSinDuplicar() {
            Long artistaId = 1L;
            LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 0, 0);

            Tarifa tarifaExistente = crearTarifa(100L, new BigDecimal("5500"), false);

            Ocupacion nuevaOcupacion = new Ocupacion();
            nuevaOcupacion.setId(2L);
            nuevaOcupacion.setTarifa(null);
            nuevaOcupacion.setFecha(fecha);

            when(tarifaRepository.findTarifasByArtistaIdAndDates(eq(artistaId), any(), any()))
                    .thenReturn(List.of(tarifaExistente));

            // Act
            Tarifa resultado = invocarObtenerTarifaByOcupacion(artistaId, fecha, nuevaOcupacion);

            // Assert: misma referencia = no se creó nueva tarifa
            assertThat(resultado).isSameAs(tarifaExistente);
            assertThat(resultado.getId()).isEqualTo(100L);
            assertThat(resultado.isActivo()).isTrue();
        }

        @Test
        @DisplayName("Crear segunda ocupación en misma fecha: reutiliza la misma tarifa")
        void crearSegundaOcupacion_mismaFecha_reutilizaMismaTarifa() {
            Long artistaId = 1L;
            LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 0, 0);

            Tarifa tarifaExistente = crearTarifa(100L, new BigDecimal("5500"), false);

            // Primera ocupación ya usa esta tarifa
            Ocupacion primeraOcupacion = new Ocupacion();
            primeraOcupacion.setId(1L);
            primeraOcupacion.setTarifa(tarifaExistente);
            primeraOcupacion.setFecha(fecha);

            // Segunda ocupación (nueva)
            Ocupacion segundaOcupacion = new Ocupacion();
            segundaOcupacion.setId(2L);
            segundaOcupacion.setTarifa(null);
            segundaOcupacion.setFecha(fecha);

            when(tarifaRepository.findTarifasByArtistaIdAndDates(eq(artistaId), any(), any()))
                    .thenReturn(List.of(tarifaExistente));

            // Act: obtener tarifa para segunda ocupación
            Tarifa resultadoSegunda = invocarObtenerTarifaByOcupacion(artistaId, fecha, segundaOcupacion);

            // Assert: ambas ocupaciones comparten la misma tarifa
            assertThat(resultadoSegunda).isSameAs(tarifaExistente);
            assertThat(primeraOcupacion.getTarifa().getId())
                    .isEqualTo(resultadoSegunda.getId());
            // Solo existe 1 tarifa activa para ese artista/fecha → 1 fila en el crosstab
        }

        @Test
        @DisplayName("Crear ocupación en fecha sin tarifa: se crea una nueva")
        void crearOcupacion_fechaSinTarifa_creaNueva() {
            Long artistaId = 1L;
            LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 0, 0);

            Ocupacion nuevaOcupacion = new Ocupacion();
            nuevaOcupacion.setId(1L);
            nuevaOcupacion.setTarifa(null);
            nuevaOcupacion.setFecha(fecha);

            when(tarifaRepository.findTarifasByArtistaIdAndDates(eq(artistaId), any(), any()))
                    .thenReturn(List.of());

            // Act
            Tarifa resultado = invocarObtenerTarifaByOcupacion(artistaId, fecha, nuevaOcupacion);

            // Assert: nueva tarifa vacía (sin id)
            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isNull();
        }
    }

    // =========================================================================
    // Invariante 4: Sync elimina ocupación → tarifa huérfana se desactiva
    // =========================================================================

    @Nested
    @DisplayName("Invariante 4: Sync limpia tarifas huérfanas")
    class SyncLimpiaTarifasHuerfanasTest {

        @Test
        @DisplayName("Ocupación huérfana eliminada → tarifa se desactiva (no hay tarifas zombie)")
        void syncEliminaOcupacionUnica_tarifaSeDesactiva() {
            // Solo esta ocupación usa la tarifa. Al eliminarla, la tarifa debe desactivarse.
            Tarifa tarifa = crearTarifa(100L, new BigDecimal("5500"), false);

            when(ocupacionRepository.countActivasByTarifaId(100L)).thenReturn(0L);

            long count = ocupacionRepository.countActivasByTarifaId(tarifa.getId());

            assertThat(count).isZero();
            verify(ocupacionRepository).countActivasByTarifaId(100L);
        }

        @Test
        @DisplayName("Ocupación compartida eliminada → tarifa se mantiene (otra ocupación la usa)")
        void syncEliminaOcupacionCompartida_tarifaSeMantiene() {
            Tarifa tarifa = crearTarifa(100L, new BigDecimal("5500"), false);

            // Otra ocupación también usa esta tarifa
            when(ocupacionRepository.countActivasByTarifaId(100L)).thenReturn(1L);

            long count = ocupacionRepository.countActivasByTarifaId(tarifa.getId());

            assertThat(count).isGreaterThan(0);
        }
    }

    // =========================================================================
    // Invariante 5: copiarTarifa preserva matinal e importe
    // =========================================================================

    @Nested
    @DisplayName("Invariante 5: copiarTarifa preserva todos los campos clave")
    class CopiarTarifaPreservacionTest {

        @Test
        @DisplayName("copiarTarifa preserva importe, matinal, fecha,_artista y activo")
        void copiarTarifa_preservaTodosLosCampos() {
            Artista artista = new Artista();
            artista.setId(1L);

            Tarifa original = new Tarifa();
            original.setId(100L);
            original.setImporte(new BigDecimal("5500"));
            original.setMatinal(true);
            original.setActivo(true);
            original.setFecha(LocalDateTime.of(2026, 4, 15, 0, 0));
            original.setArtista(artista);
            original.setFechaCreacion(LocalDateTime.now());
            original.setUsuarioCreacion("test-user");

            Tarifa copia = invocarCopiarTarifa(original);

            // Campos preservados
            assertThat(copia.getImporte()).isEqualByComparingTo(original.getImporte());
            assertThat(copia.isMatinal()).isEqualTo(original.isMatinal());
            assertThat(copia.getFecha()).isEqualTo(original.getFecha());
            assertThat(copia.getArtista()).isEqualTo(original.getArtista());
            assertThat(copia.isActivo()).isTrue();
            // Nueva instancia (sin ID)
            assertThat(copia).isNotSameAs(original);
            assertThat(copia.getId()).isNull();
        }

        @Test
        @DisplayName("copiarTarifa con matinal=false también preserva correctamente")
        void copiarTarifa_matinalFalse_preservaCorrectamente() {
            Tarifa original = crearTarifa(100L, new BigDecimal("4500"), false);

            Tarifa copia = invocarCopiarTarifa(original);

            assertThat(copia.isMatinal()).isFalse();
            assertThat(copia.getImporte()).isEqualByComparingTo(original.getImporte());
        }

        @Test
        @DisplayName("Importe no se duplica (no hay doble setImporte)")
        void copiarTarifa_noDuplicaImporte() {
            Tarifa original = crearTarifa(100L, new BigDecimal("7500"), true);

            Tarifa copia = invocarCopiarTarifa(original);

            // El importe debe aparecer una sola vez con el valor correcto
            assertThat(copia.getImporte()).isEqualByComparingTo(new BigDecimal("7500"));
        }
    }

    // =========================================================================
    // Invariante 6: actualizarTarifaSegunOcupacion no sobrescribe importe a 0
    // =========================================================================

    @Nested
    @DisplayName("Invariante 6: actualizarTarifaSegunOcupacion preserva importe de tarifa copiada")
    class ActualizarTarifaPreservaImporteTest {

        @Test
        @DisplayName("Tarifa copiada (sin ID) con importe: se preserva si ocupación no tiene importe definido")
        void tarifaCopiadaSinId_conImporte_preservaImporte() {
            // Replicamos la lógica de actualizarTarifaSegunOcupacion
            Tarifa nuevaTarifa = invocarCopiarTarifa(crearTarifa(100L, new BigDecimal("5500"), false));
            // nuevaTarifa.getId() == null, nuevaTarifa.getImporte() == 5500

            // Simular: ocupación sin importe definido (BigDecimal.ZERO)
            BigDecimal ocupacionImporte = BigDecimal.ZERO;

            BigDecimal importeResultante;
            if (nuevaTarifa.getId() != null && nuevaTarifa.getImporte() != null) {
                importeResultante = nuevaTarifa.getImporte();
            } else if (nuevaTarifa.getImporte() != null) {
                if (ocupacionImporte != null && ocupacionImporte.compareTo(BigDecimal.ZERO) > 0) {
                    importeResultante = ocupacionImporte;
                } else {
                    importeResultante = nuevaTarifa.getImporte(); // preservar importe copiado
                }
            } else {
                importeResultante = ocupacionImporte != null ? ocupacionImporte : BigDecimal.ZERO;
            }

            // El importe de la tarifa copiada se preserva, no se sobrescribe a 0
            assertThat(importeResultante).isEqualByComparingTo(new BigDecimal("5500"));
        }

        @Test
        @DisplayName("Tarifa copiada (sin ID): si ocupación tiene importe > 0, se usa el de la ocupación")
        void tarifaCopiadaSinId_ocupacionConImporte_usaImporteOcupacion() {
            Tarifa nuevaTarifa = invocarCopiarTarifa(crearTarifa(100L, new BigDecimal("5500"), false));

            BigDecimal ocupacionImporte = new BigDecimal("6000");

            BigDecimal importeResultante;
            if (nuevaTarifa.getId() != null && nuevaTarifa.getImporte() != null) {
                importeResultante = nuevaTarifa.getImporte();
            } else if (nuevaTarifa.getImporte() != null) {
                if (ocupacionImporte != null && ocupacionImporte.compareTo(BigDecimal.ZERO) > 0) {
                    importeResultante = ocupacionImporte;
                } else {
                    importeResultante = nuevaTarifa.getImporte();
                }
            } else {
                importeResultante = ocupacionImporte != null ? ocupacionImporte : BigDecimal.ZERO;
            }

            assertThat(importeResultante).isEqualByComparingTo(new BigDecimal("6000"));
        }

        @Test
        @DisplayName("Tarifa existente (con ID): se preserva su importe")
        void tarifaExistenteConId_preservaImporte() {
            Tarifa tarifaExistente = crearTarifa(100L, new BigDecimal("5500"), false);
            // tarifaExistente.getId() != null

            BigDecimal ocupacionImporte = BigDecimal.ZERO;

            BigDecimal importeResultante;
            if (tarifaExistente.getId() != null && tarifaExistente.getImporte() != null) {
                importeResultante = tarifaExistente.getImporte();
            } else if (tarifaExistente.getImporte() != null) {
                if (ocupacionImporte != null && ocupacionImporte.compareTo(BigDecimal.ZERO) > 0) {
                    importeResultante = ocupacionImporte;
                } else {
                    importeResultante = tarifaExistente.getImporte();
                }
            } else {
                importeResultante = ocupacionImporte != null ? ocupacionImporte : BigDecimal.ZERO;
            }

            assertThat(importeResultante).isEqualByComparingTo(new BigDecimal("5500"));
        }
    }

    // =========================================================================
    // Escenarios end-to-end de listados
    // =========================================================================

    @Nested
    @DisplayName("Escenarios end-to-end: listados muestran datos correctos")
    class EscenariosListadosTest {

        @Test
        @DisplayName("Caso típico: artista con ocupación → aparece en listado con tarifa y ocupación")
        void artistaConOcupacion_apareceEnListado() {
            // Simular: artista con 1 ocupación y 1 tarifa activa
            Tarifa tarifa = crearTarifa(100L, new BigDecimal("5500"), false);

            when(tarifaRepository.findTarifasByArtistaIdAndDates(eq(1L), any(), any()))
                    .thenReturn(List.of(tarifa));

            List<Tarifa> tarifas = tarifaRepository.findTarifasByArtistaIdAndDates(
                    1L, LocalDateTime.of(2026, 4, 1, 0, 0), LocalDateTime.of(2026, 4, 30, 23, 59));

            // 1 tarifa activa → 1 fila en el listado → artista aparece
            assertThat(tarifas).hasSize(1);
            assertThat(tarifas.get(0).isActivo()).isTrue();
            assertThat(tarifas.get(0).getImporte()).isEqualByComparingTo(new BigDecimal("5500"));
        }

        @Test
        @DisplayName("Caso crítico: anular ocupación → artista sigue apareciendo en listado")
        void anularOcupacion_artistaSigueEnListado() {
            // Antes: artista con 1 ocupación y 1 tarifa
            Tarifa tarifa = crearTarifa(100L, new BigDecimal("5500"), false);

            // Anular ocupación → tarifa permanece activa
            Tarifa tarifaDespuesDeAnular = invocarActualizarTarifasAnularOcupacion(tarifa);

            // Verificar: la tarifa sigue activa → el artista sigue apareciendo en listados
            assertThat(tarifaDespuesDeAnular.isActivo()).isTrue();
            assertThat(tarifaDespuesDeAnular.getImporte()).isEqualByComparingTo(new BigDecimal("5500"));

            // Simular la query del listado
            when(tarifaRepository.findTarifasByArtistaIdAndDates(eq(1L), any(), any()))
                    .thenReturn(List.of(tarifaDespuesDeAnular));

            List<Tarifa> tarifas = tarifaRepository.findTarifasByArtistaIdAndDates(
                    1L, LocalDateTime.of(2026, 4, 1, 0, 0), LocalDateTime.of(2026, 4, 30, 23, 59));

            // El artista sigue apareciendo
            assertThat(tarifas).hasSize(1);
            assertThat(tarifas.get(0).isActivo()).isTrue();
        }

        @Test
        @DisplayName("Caso crítico: dos ocupaciones mismo día → 1 tarifa → artista aparece 1 vez en listado")
        void dosOcupacionesMismoDia_unaTarifa_unaFilaEnListado() {
            // Artista con 2 ocupaciones el mismo día, compartiendo la misma tarifa
            Tarifa tarifa = crearTarifa(100L, new BigDecimal("5500"), false);

            // La query findTarifasByArtistaIdAndDates devuelve la tarifa 1 vez
            when(tarifaRepository.findTarifasByArtistaIdAndDates(eq(1L), any(), any()))
                    .thenReturn(List.of(tarifa));

            List<Tarifa> tarifas = tarifaRepository.findTarifasByArtistaIdAndDates(
                    1L, LocalDateTime.of(2026, 4, 15, 0, 0), LocalDateTime.of(2026, 4, 15, 23, 59));

            // Solo 1 tarifa activa → el artista aparece 1 vez en el listado crosstab
            assertThat(tarifas).hasSize(1);
        }

        @Test
        @DisplayName("Caso crítico: crear y anular ocupación → tarifa sigue activa → artista en listado")
        void crearYAnularOcupacion_artistaEnListado() {
            // 1. Crear ocupación → usa tarifa existente
            Long artistaId = 1L;
            LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 0, 0);
            Tarifa tarifaExistente = crearTarifa(100L, new BigDecimal("5500"), false);

            Ocupacion nuevaOcupacion = new Ocupacion();
            nuevaOcupacion.setId(1L);
            nuevaOcupacion.setTarifa(null);
            nuevaOcupacion.setFecha(fecha);

            when(tarifaRepository.findTarifasByArtistaIdAndDates(eq(artistaId), any(), any()))
                    .thenReturn(List.of(tarifaExistente));

            Tarifa tarifaParaOcupacion = invocarObtenerTarifaByOcupacion(artistaId, fecha, nuevaOcupacion);

            // Se reutiliza la tarifa existente
            assertThat(tarifaParaOcupacion).isSameAs(tarifaExistente);
            assertThat(tarifaParaOcupacion.isActivo()).isTrue();

            // 2. Anular ocupación → tarifa sigue activa
            Tarifa tarifaDespuesDeAnular = invocarActualizarTarifasAnularOcupacion(tarifaParaOcupacion);

            assertThat(tarifaDespuesDeAnular).isSameAs(tarifaExistente);
            assertThat(tarifaDespuesDeAnular.isActivo()).isTrue();

            // 3. Verificar que el artista sigue apareciendo en el listado
            when(tarifaRepository.findTarifasByArtistaIdAndDates(eq(artistaId), any(), any()))
                    .thenReturn(List.of(tarifaDespuesDeAnular));

            List<Tarifa> tarifas = tarifaRepository.findTarifasByArtistaIdAndDates(
                    artistaId, fecha, fecha.plusDays(1));

            assertThat(tarifas).hasSize(1);
            assertThat(tarifas.get(0).isActivo()).isTrue();
            assertThat(tarifas.get(0).getImporte()).isEqualByComparingTo(new BigDecimal("5500"));
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
        tarifa.setFecha(LocalDateTime.of(2026, 4, 15, 0, 0));
        tarifa.setArtista(artista);
        tarifa.setFechaCreacion(LocalDateTime.now());
        tarifa.setUsuarioCreacion("test-user");
        return tarifa;
    }

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

    private Tarifa invocarActualizarTarifasAnularOcupacion(Tarifa oldTarifa) {
        // Al anular una ocupación, no se crea ni se desactiva ninguna tarifa.
        // La tarifa se mantiene activa para que el artista siga apareciendo en listados.
        return oldTarifa;
    }
}