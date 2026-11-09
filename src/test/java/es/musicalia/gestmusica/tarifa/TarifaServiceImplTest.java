package es.musicalia.gestmusica.tarifa;

import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.excel.ExcelExportService;
import es.musicalia.gestmusica.informe.InformeService;
import es.musicalia.gestmusica.listado.TipoTarifaEnum;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.localizacion.ProvinciaRepository;
import es.musicalia.gestmusica.tipoescenario.TipoEscenario;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TarifaServiceImplTest {

    private static final long ID_ARTISTA = 7L;

    @Mock
    private TarifaRepository tarifaRepository;
    @Mock
    private ArtistaRepository artistaRepository;
    @Mock
    private UserService userService;
    @Mock
    private InformeService informeService;
    @Mock
    private ProvinciaRepository provinciaRepository;
    @Mock
    private ExcelExportService excelExportService;

    @InjectMocks
    private TarifaServiceImpl service;

    @Nested
    class SaveTarifa {

        @Test
        void rangeOfDays_createsOneActiveTarifaPerDayIncludingBothEnds() {
            Artista artista = artista();
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.of(artista));
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuario("gestor")));
            LocalDateTime desde = LocalDateTime.of(2026, 7, 1, 0, 0);
            TarifaSaveDto dto = new TarifaSaveDto(null, ID_ARTISTA, desde, desde.plusDays(2), new BigDecimal("1500"), null);

            service.saveTarifa(dto);

            ArgumentCaptor<Tarifa> saved = ArgumentCaptor.forClass(Tarifa.class);
            verify(tarifaRepository, times(3)).save(saved.capture());
            assertThat(saved.getAllValues()).extracting(Tarifa::getFecha)
                    .containsExactly(desde, desde.plusDays(1), desde.plusDays(2));
            assertThat(saved.getAllValues()).allSatisfy(tarifa -> {
                assertThat(tarifa.getArtista()).isSameAs(artista);
                assertThat(tarifa.getImporte()).isEqualByComparingTo("1500");
                assertThat(tarifa.isActivo()).isTrue();
                assertThat(tarifa.getFechaCreacion()).isNotNull();
                assertThat(tarifa.getUsuarioCreacion()).isEqualTo("gestor");
                assertThat(tarifa.getFechaModificacion()).isNull();
                assertThat(tarifa.getUsuarioModificacion()).isNull();
            });
        }

        @Test
        void eachDay_looksUpExistingTarifaWithinThatWholeDay() {
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.of(artista()));
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuario("gestor")));
            LocalDateTime desde = LocalDateTime.of(2026, 7, 1, 18, 30);
            TarifaSaveDto dto = new TarifaSaveDto(null, ID_ARTISTA, desde, desde.plusDays(1), BigDecimal.TEN, true);

            service.saveTarifa(dto);

            verify(tarifaRepository).findTarifasByArtistaIdAndDates(ID_ARTISTA,
                    LocalDateTime.of(2026, 7, 1, 0, 0, 0), LocalDateTime.of(2026, 7, 1, 23, 59, 59));
            verify(tarifaRepository).findTarifasByArtistaIdAndDates(ID_ARTISTA,
                    LocalDateTime.of(2026, 7, 2, 0, 0, 0), LocalDateTime.of(2026, 7, 2, 23, 59, 59));
        }

        @Test
        void withoutFechaHasta_savesOnlyFechaDesde() {
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.of(artista()));
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuario("gestor")));
            LocalDateTime desde = LocalDateTime.of(2026, 8, 15, 0, 0);
            TarifaSaveDto dto = new TarifaSaveDto(null, ID_ARTISTA, desde, null, new BigDecimal("900"), true);

            service.saveTarifa(dto);

            ArgumentCaptor<Tarifa> saved = ArgumentCaptor.forClass(Tarifa.class);
            verify(tarifaRepository).save(saved.capture());
            assertThat(saved.getValue().getFecha()).isEqualTo(desde);
            assertThat(saved.getValue().getImporte()).isEqualByComparingTo("900");
        }

        @Test
        void fechaHastaBeforeFechaDesde_savesNothing() {
            LocalDateTime desde = LocalDateTime.of(2026, 8, 15, 0, 0);
            TarifaSaveDto dto = new TarifaSaveDto(null, ID_ARTISTA, desde, desde.minusDays(3), BigDecimal.TEN, true);

            service.saveTarifa(dto);

            verify(tarifaRepository, never()).save(any());
            verifyNoInteractions(userService, artistaRepository);
        }

        @Test
        void existingTarifaThatDay_isUpdatedKeepingCreationAudit() {
            Artista artista = artista();
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.of(artista));
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuario("editor")));
            LocalDateTime fecha = LocalDateTime.of(2026, 9, 1, 0, 0);
            LocalDateTime creada = LocalDateTime.of(2025, 1, 1, 10, 0);
            Tarifa existente = tarifa(55L, creada, "creador", new BigDecimal("100"));
            when(tarifaRepository.findTarifasByArtistaIdAndDates(eq(ID_ARTISTA), any(), any())).thenReturn(List.of(existente));
            TarifaSaveDto dto = new TarifaSaveDto(null, ID_ARTISTA, fecha, fecha, new BigDecimal("2500"), null);

            service.saveTarifa(dto);

            ArgumentCaptor<Tarifa> saved = ArgumentCaptor.forClass(Tarifa.class);
            verify(tarifaRepository).save(saved.capture());
            Tarifa tarifa = saved.getValue();
            assertThat(tarifa).isSameAs(existente);
            assertThat(tarifa.getId()).isEqualTo(55L);
            assertThat(tarifa.getImporte()).isEqualByComparingTo("2500");
            assertThat(tarifa.getFecha()).isEqualTo(fecha);
            assertThat(tarifa.isActivo()).isTrue();
            assertThat(tarifa.getFechaCreacion()).isEqualTo(creada);
            assertThat(tarifa.getUsuarioCreacion()).isEqualTo("creador");
            assertThat(tarifa.getFechaModificacion()).isNotNull();
            assertThat(tarifa.getUsuarioModificacion()).isEqualTo("editor");
            verify(tarifaRepository, never()).findById(anyLong());
        }

        @Test
        void existingTarifaThatDay_winsOverTarifaLoadedById() {
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.of(artista()));
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuario("editor")));
            Tarifa porId = tarifa(1L, LocalDateTime.of(2025, 1, 1, 0, 0), "a", BigDecimal.ONE);
            Tarifa delDia = tarifa(2L, LocalDateTime.of(2025, 1, 1, 0, 0), "b", BigDecimal.ONE);
            when(tarifaRepository.findById(1L)).thenReturn(Optional.of(porId));
            when(tarifaRepository.findTarifasByArtistaIdAndDates(eq(ID_ARTISTA), any(), any())).thenReturn(List.of(delDia));
            LocalDateTime fecha = LocalDateTime.of(2026, 9, 1, 0, 0);

            service.saveTarifa(new TarifaSaveDto(1L, ID_ARTISTA, fecha, fecha, BigDecimal.TEN, true));

            ArgumentCaptor<Tarifa> saved = ArgumentCaptor.forClass(Tarifa.class);
            verify(tarifaRepository).save(saved.capture());
            assertThat(saved.getValue()).isSameAs(delDia);
            assertThat(porId.getImporte()).isEqualByComparingTo("1");
        }

        @Test
        void idGivenAndNoTarifaThatDay_updatesTarifaLoadedById() {
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.of(artista()));
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuario("editor")));
            Tarifa porId = tarifa(1L, LocalDateTime.of(2025, 1, 1, 0, 0), "creador", BigDecimal.ONE);
            when(tarifaRepository.findById(1L)).thenReturn(Optional.of(porId));
            LocalDateTime fecha = LocalDateTime.of(2026, 9, 2, 0, 0);

            service.saveTarifa(new TarifaSaveDto(1L, ID_ARTISTA, fecha, fecha, new BigDecimal("300"), true));

            ArgumentCaptor<Tarifa> saved = ArgumentCaptor.forClass(Tarifa.class);
            verify(tarifaRepository).save(saved.capture());
            assertThat(saved.getValue()).isSameAs(porId);
            assertThat(porId.getFecha()).isEqualTo(fecha);
            assertThat(porId.getImporte()).isEqualByComparingTo("300");
            assertThat(porId.getUsuarioModificacion()).isEqualTo("editor");
        }

        @Test
        void unknownId_createsNewTarifa() {
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.of(artista()));
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuario("gestor")));
            when(tarifaRepository.findById(99L)).thenReturn(Optional.empty());
            LocalDateTime fecha = LocalDateTime.of(2026, 9, 3, 0, 0);

            service.saveTarifa(new TarifaSaveDto(99L, ID_ARTISTA, fecha, fecha, BigDecimal.TEN, true));

            ArgumentCaptor<Tarifa> saved = ArgumentCaptor.forClass(Tarifa.class);
            verify(tarifaRepository).save(saved.capture());
            assertThat(saved.getValue().getId()).isNull();
            assertThat(saved.getValue().getUsuarioCreacion()).isEqualTo("gestor");
            assertThat(saved.getValue().getUsuarioModificacion()).isNull();
        }

        @Test
        void activoFalse_isSavedAsInactive() {
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.of(artista()));
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuario("gestor")));
            Tarifa existente = tarifa(3L, LocalDateTime.of(2025, 1, 1, 0, 0), "creador", BigDecimal.TEN);
            existente.setActivo(true);
            when(tarifaRepository.findTarifasByArtistaIdAndDates(eq(ID_ARTISTA), any(), any())).thenReturn(List.of(existente));
            LocalDateTime fecha = LocalDateTime.of(2026, 9, 4, 0, 0);

            service.saveTarifa(new TarifaSaveDto(3L, ID_ARTISTA, fecha, fecha, BigDecimal.TEN, false));

            verify(tarifaRepository).save(existente);
            assertThat(existente.isActivo()).isFalse();
        }

        @Test
        void unknownArtista_throwsAndSavesNothing() {
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.empty());
            LocalDateTime fecha = LocalDateTime.of(2026, 9, 5, 0, 0);

            assertThatThrownBy(() -> service.saveTarifa(new TarifaSaveDto(null, ID_ARTISTA, fecha, fecha, BigDecimal.TEN, true)))
                    .isInstanceOf(NoSuchElementException.class);

            verify(tarifaRepository, never()).save(any());
        }

        @Test
        void withoutAuthenticatedUser_throwsAndSavesNothing() {
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.of(artista()));
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.empty());
            LocalDateTime fecha = LocalDateTime.of(2026, 9, 5, 0, 0);

            assertThatThrownBy(() -> service.saveTarifa(new TarifaSaveDto(null, ID_ARTISTA, fecha, fecha, BigDecimal.TEN, true)))
                    .isInstanceOf(NoSuchElementException.class);

            verify(tarifaRepository, never()).save(any());
        }
    }

    @Nested
    class Lookups {

        @Test
        void findByArtistaIdAndDate_returnsFirstTarifaOfThatWholeDay() {
            TarifaDto primera = new TarifaDto(1L, LocalDateTime.of(2026, 7, 1, 0, 0), ID_ARTISTA, "1500", true);
            TarifaDto segunda = new TarifaDto(2L, LocalDateTime.of(2026, 7, 1, 0, 0), ID_ARTISTA, "900", true);
            when(tarifaRepository.findTarifasDtoByArtistaIdAndDates(ID_ARTISTA,
                    LocalDateTime.of(2026, 7, 1, 0, 0, 0), LocalDateTime.of(2026, 7, 1, 23, 59, 59)))
                    .thenReturn(Optional.of(List.of(primera, segunda)));

            assertThat(service.findByArtistaIdAndDate(ID_ARTISTA, LocalDate.of(2026, 7, 1))).isEqualTo(primera);
        }

        @Test
        void findByArtistaIdAndDate_withoutTarifas_returnsNull() {
            when(tarifaRepository.findTarifasDtoByArtistaIdAndDates(eq(ID_ARTISTA), any(), any()))
                    .thenReturn(Optional.of(List.of()))
                    .thenReturn(Optional.empty());

            assertThat(service.findByArtistaIdAndDate(ID_ARTISTA, LocalDate.of(2026, 7, 1))).isNull();
            assertThat(service.findByArtistaIdAndDate(ID_ARTISTA, LocalDate.of(2026, 7, 1))).isNull();
        }

        @Test
        void findByArtistaId_returnsRepositoryListOrEmptyList() {
            LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2026, 1, 31, 0, 0);
            TarifaDto dto = new TarifaDto(1L, start, ID_ARTISTA, "1500", true);
            when(tarifaRepository.findTarifasDtoByArtistaIdAndDates(ID_ARTISTA, start, end))
                    .thenReturn(Optional.of(List.of(dto)))
                    .thenReturn(Optional.empty());

            assertThat(service.findByArtistaId(ID_ARTISTA, start, end)).containsExactly(dto);
            assertThat(service.findByArtistaId(ID_ARTISTA, start, end)).isEmpty();
        }

        @Test
        void findTarifasByFechaAndNumComponentesArtista_usesArtistaComponentesDayWindowAndExcludesArtista() {
            Artista artista = artista();
            artista.setComponentes(12);
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.of(artista));
            List<TarifaArtistaCcaaDto> similares = List.of(new TarifaArtistaCcaaDto(8L, "Otra", new BigDecimal("2000")));
            when(tarifaRepository.findTarifasByFechaAndNumeroComponentes(12,
                    LocalDateTime.of(2026, 7, 1, 0, 0, 0), LocalDateTime.of(2026, 7, 1, 23, 59, 59), ID_ARTISTA))
                    .thenReturn(similares);

            assertThat(service.findTarifasByFechaAndNumComponentesArtista(ID_ARTISTA, LocalDate.of(2026, 7, 1)))
                    .isEqualTo(similares);
        }

        @Test
        void findTarifasByFechaAndNumComponentesArtista_unknownArtista_throws() {
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findTarifasByFechaAndNumComponentesArtista(ID_ARTISTA, LocalDate.of(2026, 7, 1)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Artista no encontrado");
            verifyNoInteractions(tarifaRepository);
        }

        @Test
        void agenciaTieneTarifasActivas_andContarTarifasActivasAnio_delegateToRepository() {
            when(tarifaRepository.existsByArtistaAgenciaIdAndActivoTrue(4L)).thenReturn(true);
            when(tarifaRepository.countTarifasActivasByArtistaAndAnio(ID_ARTISTA, 2026)).thenReturn(42);

            assertThat(service.agenciaTieneTarifasActivas(4L)).isTrue();
            assertThat(service.contarTarifasActivasAnio(ID_ARTISTA, 2026)).isEqualTo(42);
        }
    }

    @Nested
    class InformeTarifaAnual {

        @Test
        void anual_buildsParametersAndUsesHorizontalReport() {
            Artista artista = artista();
            artista.setComponentes(9);
            artista.setEscenario(true);
            TipoEscenario tipoEscenario = new TipoEscenario();
            tipoEscenario.setNombre("Trailer");
            artista.setTipoEscenario(tipoEscenario);
            artista.setMedidasEscenario("12x8");
            artista.setCondicionesContratacion("50% por adelantado");
            artista.setLogo("https://res.cloudinary.com/demo/image/upload/v1/artista.webp");
            artista.getAgencia().setLogo("https://res.cloudinary.com/demo/image/upload/v2/agencia.webp");
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.of(artista));
            when(provinciaRepository.findById(15L)).thenReturn(Optional.of(provincia("A Coruña")));
            byte[] pdf = {1, 2, 3};
            when(informeService.imprimirInforme(anyMap(), anyString(), anyString())).thenReturn(pdf);

            byte[] result = service.getInformeTarifaAnual(new TarifaAnualDto(ID_ARTISTA, 15L, 2026, true, TipoTarifaEnum.ANUAL));

            assertThat(result).isSameAs(pdf);
            ArgumentCaptor<Map<String, Object>> parametros = ArgumentCaptor.captor();
            ArgumentCaptor<String> fileName = ArgumentCaptor.forClass(String.class);
            verify(informeService).imprimirInforme(parametros.capture(), fileName.capture(),
                    eq("tarifa_anual_horizontal_ocupacion.jrxml"));
            assertThat(parametros.getValue())
                    .containsEntry("titulo", "Orquesta Prueba")
                    .containsEntry("idArtista", ID_ARTISTA)
                    .containsEntry("ano", "2026")
                    .containsEntry("idProvincia", 15)
                    .containsEntry("conOcupacion", true)
                    .containsEntry("datosAgrupacion",
                            "Número de componentes: 9\nEscenario: Trailer\nMedidas escenario: 12x8\n")
                    .containsEntry("datosAgencia",
                            "Provincia: A Coruña\nCondiciones de contratación: 50% por adelantado\n")
                    .containsEntry("logoArtista", "https://res.cloudinary.com/demo/image/upload/f_png/v1/artista.webp")
                    .containsEntry("logoAgencia", "https://res.cloudinary.com/demo/image/upload/f_png/v2/agencia.webp");
            assertThat(fileName.getValue()).startsWith("Orquesta Prueba").endsWith(".pdf");
        }

        @Test
        void ochoMeses_withoutEscenarioCondicionesOrLogos_usesEightMonthReportAndMinimalTexts() {
            Artista artista = artista();
            artista.setComponentes(4);
            artista.setCondicionesContratacion("");
            artista.setLogo(null);
            artista.getAgencia().setLogo(" ");
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.of(artista));
            when(provinciaRepository.findById(27L)).thenReturn(Optional.of(provincia("Lugo")));

            service.getInformeTarifaAnual(new TarifaAnualDto(ID_ARTISTA, 27L, 2026, false, TipoTarifaEnum.OCHO_MESES));

            ArgumentCaptor<Map<String, Object>> parametros = ArgumentCaptor.captor();
            verify(informeService).imprimirInforme(parametros.capture(), anyString(),
                    eq("tarifa_anual_horizontal_ocupacion_8meses.jrxml"));
            assertThat(parametros.getValue())
                    .containsEntry("conOcupacion", false)
                    .containsEntry("datosAgrupacion", "Número de componentes: 4\n")
                    .containsEntry("datosAgencia", "Provincia: Lugo\n")
                    .containsEntry("logoArtista", null)
                    .containsEntry("logoAgencia", null);
        }

        @Test
        void withoutTipoTarifa_fallsBackToEightMonthReport() {
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.of(artista()));
            when(provinciaRepository.findById(27L)).thenReturn(Optional.of(provincia("Lugo")));

            service.getInformeTarifaAnual(new TarifaAnualDto(ID_ARTISTA, 27L, 2026, false, null));

            verify(informeService).imprimirInforme(anyMap(), anyString(), eq("tarifa_anual_horizontal_ocupacion_8meses.jrxml"));
        }

        @Test
        void unknownArtista_throwsWithoutPrinting() {
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getInformeTarifaAnual(new TarifaAnualDto(ID_ARTISTA, 27L, 2026, false, TipoTarifaEnum.ANUAL)))
                    .isInstanceOf(NoSuchElementException.class);
            verifyNoInteractions(informeService);
        }
    }

    @Nested
    class ExportTarifaAnualExcel {

        @Test
        void anual_mapsEveryColumnAndExportsAllMonthsWithSideInfo() {
            Object[] row = IntStream.range(0, 26).mapToObj(i -> "v" + i).toArray();
            when(tarifaRepository.findTarifaAnualData(ID_ARTISTA, 2026, 15, true)).thenReturn(List.<Object[]>of(row));
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.of(artista()));
            when(provinciaRepository.findById(15L)).thenReturn(Optional.of(provincia("A Coruña")));
            ByteArrayOutputStream excel = new ByteArrayOutputStream();
            when(excelExportService.exportToExcelWithSideInfo(any(), eq(TarifaAnualExportDto.class), eq("Orquesta Prueba"), any(), isNull()))
                    .thenReturn(excel);

            ByteArrayOutputStream result = service.exportTarifaAnualToExcel(new TarifaAnualDto(ID_ARTISTA, 15L, 2026, true, TipoTarifaEnum.ANUAL));

            assertThat(result).isSameAs(excel);
            ArgumentCaptor<List<TarifaAnualExportDto>> datos = ArgumentCaptor.captor();
            ArgumentCaptor<List<String>> sideInfo = ArgumentCaptor.captor();
            verify(excelExportService).exportToExcelWithSideInfo(datos.capture(), eq(TarifaAnualExportDto.class),
                    eq("Orquesta Prueba"), sideInfo.capture(), isNull());
            assertThat(datos.getValue()).containsExactly(new TarifaAnualExportDto(
                    "v0", "v1", "v2", "v3", "v4", "v5", "v6", "v7", "v8", "v9", "v10", "v11", "v12",
                    "v13", "v14", "v15", "v16", "v17", "v18", "v19", "v20", "v21", "v22", "v23", "v24", "v25"));
            assertThat(sideInfo.getValue()).hasSize(5);
            assertThat(sideInfo.getValue().subList(0, 4)).containsExactly(
                    "Artista: Orquesta Prueba", "Agencia: Agencia Prueba", "Provincia: A Coruña", "Año: 2026");
            assertThat(sideInfo.getValue().get(4)).matches("Fecha de generación: \\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}");
        }

        @Test
        void ochoMeses_restrictsColumnsToMarchThroughOctober() {
            when(tarifaRepository.findTarifaAnualData(ID_ARTISTA, 2026, 15, false)).thenReturn(List.of());
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.of(artista()));
            when(provinciaRepository.findById(15L)).thenReturn(Optional.of(provincia("A Coruña")));

            service.exportTarifaAnualToExcel(new TarifaAnualDto(ID_ARTISTA, 15L, 2026, false, TipoTarifaEnum.OCHO_MESES));

            ArgumentCaptor<List<String>> fields = ArgumentCaptor.captor();
            verify(excelExportService).exportToExcelWithSideInfo(eq(List.of()), eq(TarifaAnualExportDto.class),
                    eq("Orquesta Prueba"), any(), fields.capture());
            assertThat(fields.getValue()).containsExactly(
                    "marzo", "marzoValor", "abril", "abrilValor", "mayo", "mayoValor", "junio", "junioValor",
                    "julio", "julioValor", "agosto", "agostoValor", "septiembre", "septiembreValor",
                    "octubre", "octubreValor");
        }

        @Test
        void unknownProvincia_isReportedAsNotAvailable() {
            when(tarifaRepository.findTarifaAnualData(ID_ARTISTA, 2026, 99, true)).thenReturn(List.of());
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.of(artista()));
            when(provinciaRepository.findById(99L)).thenReturn(Optional.empty());

            service.exportTarifaAnualToExcel(new TarifaAnualDto(ID_ARTISTA, 99L, 2026, true, TipoTarifaEnum.ANUAL));

            ArgumentCaptor<List<String>> sideInfo = ArgumentCaptor.captor();
            verify(excelExportService).exportToExcelWithSideInfo(any(), eq(TarifaAnualExportDto.class), any(), sideInfo.capture(), isNull());
            assertThat(sideInfo.getValue()).contains("Provincia: N/A");
        }

        @Test
        void unknownArtista_throwsWithoutExporting() {
            when(tarifaRepository.findTarifaAnualData(ID_ARTISTA, 2026, 15, true)).thenReturn(List.of());
            when(artistaRepository.findById(ID_ARTISTA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.exportTarifaAnualToExcel(new TarifaAnualDto(ID_ARTISTA, 15L, 2026, true, TipoTarifaEnum.ANUAL)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Artista no encontrado");
            verifyNoInteractions(excelExportService);
        }
    }

    private static Artista artista() {
        Agencia agencia = new Agencia();
        agencia.setNombre("Agencia Prueba");
        Artista artista = new Artista();
        artista.setId(ID_ARTISTA);
        artista.setNombre("Orquesta Prueba");
        artista.setAgencia(agencia);
        return artista;
    }

    private static Provincia provincia(String nombre) {
        Provincia provincia = new Provincia();
        provincia.setNombre(nombre);
        return provincia;
    }

    private static Usuario usuario(String username) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        return usuario;
    }

    private static Tarifa tarifa(Long id, LocalDateTime fechaCreacion, String usuarioCreacion, BigDecimal importe) {
        Tarifa tarifa = new Tarifa();
        tarifa.setId(id);
        tarifa.setFechaCreacion(fechaCreacion);
        tarifa.setUsuarioCreacion(usuarioCreacion);
        tarifa.setImporte(importe);
        return tarifa;
    }
}
