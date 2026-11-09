package es.musicalia.gestmusica.sincronizacion;

import es.musicalia.gestmusica.artista.ArtistaAgenciaRecord;
import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.localizacion.Ccaa;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.MunicipioRepository;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.localizacion.ProvinciaRepository;
import es.musicalia.gestmusica.ocupacion.ModificacionOcupacionException;
import es.musicalia.gestmusica.ocupacion.Ocupacion;
import es.musicalia.gestmusica.ocupacion.OcupacionRepository;
import es.musicalia.gestmusica.ocupacion.OcupacionSaveDto;
import es.musicalia.gestmusica.ocupacion.OcupacionService;
import es.musicalia.gestmusica.ocupacion.TipoOcupacionEnum;
import es.musicalia.gestmusica.tarifa.Tarifa;
import es.musicalia.gestmusica.tarifa.TarifaRepository;
import es.musicalia.gestmusica.util.ConstantsGestmusica;
import es.musicalia.gestmusicalegacy.ocupacion.OcupacionLegacy;
import es.musicalia.gestmusicalegacy.ocupacion.OcupacionLegacyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SincronizacionServiceTest {

    private static final LocalDate FECHA_DESDE = LocalDate.of(2025, 6, 1);
    private static final LocalDateTime MODIFICADO_DESDE = LocalDateTime.of(2025, 6, 1, 10, 0);
    private static final int ID_ARTISTA_LEGACY = 5;
    private static final int ID_OCUPACION_LEGACY = 900;
    private static final ArtistaAgenciaRecord ARTISTA = new ArtistaAgenciaRecord(70L, 80L);

    @Mock private OcupacionService ocupacionService;
    @Mock private OcupacionLegacyService ocupacionLegacyService;
    @Mock private ArtistaRepository artistaRepository;
    @Mock private SincronizacionRepository sincronizacionRepository;
    @Mock private ProvinciaRepository provinciaRepository;
    @Mock private MunicipioRepository municipioRepository;
    @Mock private OcupacionRepository ocupacionRepository;
    @Mock private TarifaRepository tarifaRepository;

    @InjectMocks
    private SincronizacionService service;

    // --- Alta (new ocupación) ---

    @Test
    void newLegacyOcupacionIsCreatedAndRecordedAsProcessed() throws Exception {
        OcupacionLegacy legacy = legacy(ID_OCUPACION_LEGACY, OcupacionLegacy.EstadoOcupacion.ocupado, null);
        legacy.setIdProvincia(15);
        legacy.setPoblacion("Sada");
        legacy.setLugar("Praza");
        legacy.setObservaciones("Fiestas patronales");
        legacy.setAyuntamiento("Sada");
        legacy.setMt(true);
        legacy.setSmt(false);
        givenLegacy(legacy);
        givenArtistaExists();
        givenSaveReturnsId(321L);

        SincronizacionResult result = service.sincronizarOcupacionesDesde(FECHA_DESDE, MODIFICADO_DESDE);

        assertThat(result.getExitosas()).isEqualTo(1);
        assertThat(result.getCreadas()).isEqualTo(1);
        assertThat(result.getActualizadas()).isZero();
        assertThat(result.getEliminadas()).isZero();
        assertThat(result.getErrorGeneral()).isNull();
        assertThat(result.getErrores()).isEmpty();

        OcupacionSaveDto dto = capturedSavedDto();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getIdOcupacionLegacy()).isEqualTo(ID_OCUPACION_LEGACY);
        assertThat(dto.getIdArtista()).isEqualTo(70L);
        assertThat(dto.getIdAgencia()).isEqualTo(80L);
        assertThat(dto.getFecha()).isEqualTo(LocalDate.of(2025, 7, 15).atStartOfDay());
        assertThat(dto.getEstado()).isEqualTo("ocupado");
        assertThat(dto.getIdTipoOcupacion()).isEqualTo(TipoOcupacionEnum.OCUPADO.getId());
        assertThat(dto.getLocalidad()).isEqualTo("Sada");
        assertThat(dto.getLugar()).isEqualTo("Praza");
        assertThat(dto.getObservaciones()).isEqualTo("Fiestas patronales");
        assertThat(dto.getImporte()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getIva()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getPorcentajeRepre()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getMatinal()).isTrue();
        assertThat(dto.getSoloMatinal()).isFalse();
        assertThat(dto.getProvisional()).isFalse();
        verify(ocupacionService).guardarOcupacion(any(OcupacionSaveDto.class), eq(true), eq(true));

        Sincronizacion registro = capturedSincronizacion();
        assertThat(registro.getProcesado()).isTrue();
        assertThat(registro.getCodigoError()).isNull();
        assertThat(registro.getAccion()).isEqualTo("sincronizar");
        assertThat(registro.getIdArtista()).isEqualTo(5L);
        assertThat(registro.getFecha()).isEqualTo("2025-07-15");
        assertThat(registro.getDescripcion()).isEqualTo("Fiestas patronales");
        assertThat(registro.getPoblacion()).isEqualTo("Sada");
        assertThat(registro.getMunicipio()).isEqualTo("Sada");
        assertThat(registro.getProvincia()).isEqualTo("15");
        assertThat(registro.getLugar()).isEqualTo("Praza");
        assertThat(registro.getEstado()).isEqualTo("ocupado");
        assertThat(registro.getMatinal()).isTrue();
        assertThat(registro.getSoloMatinal()).isFalse();
        assertThat(registro.getFechaRecepcion()).isNotNull();
    }

    @Test
    void toSincronizacionUsesEmptyStringsForMissingProvinciaAndEstado() {
        OcupacionLegacy legacy = legacy(ID_OCUPACION_LEGACY, null, "ESPAÑA");

        Sincronizacion registro = SincronizacionService.toSincronizacion(legacy);

        assertThat(registro.getProvincia()).isEmpty();
        assertThat(registro.getEstado()).isEmpty();
        assertThat(registro.getPais()).isEqualTo("ESPAÑA");
        assertThat(registro.getProcesado()).isNull();
    }

    // --- Modificación (existing ocupación) ---

    @Test
    void existingOcupacionIsUpdatedKeepingItsEconomicDataAndRecordedAsProcessed() throws Exception {
        OcupacionLegacy legacy = legacy(ID_OCUPACION_LEGACY, OcupacionLegacy.EstadoOcupacion.ocupado, null);
        givenLegacy(legacy);
        givenArtistaExists();
        Ocupacion existente = new Ocupacion();
        existente.setId(44L);
        existente.setImporte(new BigDecimal("1500.00"));
        existente.setIva(new BigDecimal("21"));
        existente.setPorcentajeRepre(new BigDecimal("10"));
        existente.setTextoOrquestasDeGalicia("texto OdG");
        when(ocupacionService.buscarPorIdOcupacionLegacy(ID_OCUPACION_LEGACY)).thenReturn(Optional.of(existente));

        SincronizacionResult result = service.sincronizarOcupacionesDesde(FECHA_DESDE, MODIFICADO_DESDE);

        assertThat(result.getExitosas()).isEqualTo(1);
        assertThat(result.getActualizadas()).isEqualTo(1);
        assertThat(result.getCreadas()).isZero();

        OcupacionSaveDto dto = capturedSavedDto();
        assertThat(dto.getId()).isEqualTo(44L);
        assertThat(dto.getIdOcupacionLegacy()).isEqualTo(ID_OCUPACION_LEGACY);
        assertThat(dto.getImporte()).isEqualByComparingTo("1500.00");
        assertThat(dto.getIva()).isEqualByComparingTo("21");
        assertThat(dto.getPorcentajeRepre()).isEqualByComparingTo("10");
        assertThat(dto.getTextoOrquestasDeGalicia()).isEqualTo("texto OdG");
        assertThat(dto.getFecha()).isEqualTo(LocalDate.of(2025, 7, 15).atStartOfDay());
        verify(ocupacionService).guardarOcupacion(any(OcupacionSaveDto.class), eq(true), eq(true));
        assertThat(capturedSincronizacion().getProcesado()).isTrue();
    }

    // --- Error handling ---

    @Test
    void unknownArtistaIsRecordedAsErrorWithoutSavingOcupacion() throws Exception {
        givenLegacy(legacy(ID_OCUPACION_LEGACY, OcupacionLegacy.EstadoOcupacion.ocupado, null));
        when(artistaRepository.findArtistaByIdArtistaGestmanager(5L)).thenReturn(Optional.empty());

        SincronizacionResult result = service.sincronizarOcupacionesDesde(FECHA_DESDE, MODIFICADO_DESDE);

        assertThat(result.getExitosas()).isZero();
        assertThat(result.getCreadas()).isZero();
        assertThat(result.getActualizadas()).isZero();
        assertThat(result.getErrorGeneral()).isNull();
        verify(ocupacionService, never()).guardarOcupacion(any(), anyBoolean(), anyBoolean());
        verify(ocupacionService, never()).buscarPorIdOcupacionLegacy(any());

        Sincronizacion registro = capturedSincronizacion();
        assertThat(registro.getProcesado()).isFalse();
        assertThat(registro.getCodigoError()).isEqualTo("No existe el id artista: 5");
    }

    @Test
    void saveFailureIsRecordedAsErrorAndProcessingContinuesWithNextOcupacion() throws Exception {
        OcupacionLegacy falla = legacy(ID_OCUPACION_LEGACY, OcupacionLegacy.EstadoOcupacion.ocupado, null);
        OcupacionLegacy correcta = legacy(ID_OCUPACION_LEGACY + 1, OcupacionLegacy.EstadoOcupacion.ocupado, null);
        when(ocupacionLegacyService.findOcupacionLegacyFromGestmusicaLegacyDesdeMofidicaic(FECHA_DESDE, MODIFICADO_DESDE))
                .thenReturn(List.of(falla, correcta));
        givenArtistaExists();
        Ocupacion guardada = new Ocupacion();
        guardada.setId(1L);
        when(ocupacionService.guardarOcupacion(any(OcupacionSaveDto.class), eq(true), eq(true)))
                .thenThrow(new ModificacionOcupacionException("fecha bloqueada"))
                .thenReturn(guardada);

        SincronizacionResult result = service.sincronizarOcupacionesDesde(FECHA_DESDE, MODIFICADO_DESDE);

        assertThat(result.getExitosas()).isEqualTo(1);
        assertThat(result.getCreadas()).isEqualTo(1);
        assertThat(result.getErrorGeneral()).isNull();
        ArgumentCaptor<Sincronizacion> captor = ArgumentCaptor.forClass(Sincronizacion.class);
        verify(sincronizacionRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getProcesado()).isFalse();
        assertThat(captor.getAllValues().get(0).getCodigoError()).isEqualTo("Error inesperado guardando la ocupacion: 5");
        assertThat(captor.getAllValues().get(1).getProcesado()).isTrue();
        assertThat(captor.getAllValues().get(1).getCodigoError()).isNull();
    }

    @Test
    void legacyWithoutEstadoFailsMappingAndIsRecordedAsError() throws Exception {
        givenLegacy(legacy(ID_OCUPACION_LEGACY, null, null));
        givenArtistaExists();

        SincronizacionResult result = service.sincronizarOcupacionesDesde(FECHA_DESDE, MODIFICADO_DESDE);

        assertThat(result.getExitosas()).isZero();
        verify(ocupacionService, never()).guardarOcupacion(any(), anyBoolean(), anyBoolean());
        Sincronizacion registro = capturedSincronizacion();
        assertThat(registro.getProcesado()).isFalse();
        assertThat(registro.getEstado()).isEmpty();
        assertThat(registro.getCodigoError()).isEqualTo("Error inesperado guardando la ocupacion: 5");
    }

    @Test
    void legacyReadFailureIsReportedAsGeneralErrorWithoutTouchingAnything() {
        when(ocupacionLegacyService.findOcupacionLegacyFromGestmusicaLegacyDesdeMofidicaic(FECHA_DESDE, MODIFICADO_DESDE))
                .thenThrow(new IllegalStateException("MariaDB caída"));

        SincronizacionResult result = service.sincronizarOcupacionesDesde(FECHA_DESDE, MODIFICADO_DESDE);

        assertThat(result.getErrorGeneral()).isEqualTo("MariaDB caída");
        assertThat(result.getExitosas()).isZero();
        assertThat(result.getEliminadas()).isZero();
        verifyNoInteractions(sincronizacionRepository, ocupacionService, ocupacionRepository, tarifaRepository);
    }

    @Test
    void vacacionesIsSkippedButCountedAsSuccessfulAndProcessed() throws Exception {
        // Characterizes current behavior: the "not implemented" response is discarded.
        givenLegacy(legacy(ID_OCUPACION_LEGACY, OcupacionLegacy.EstadoOcupacion.ocupado, "Vacaciones"));
        givenArtistaExists();

        SincronizacionResult result = service.sincronizarOcupacionesDesde(FECHA_DESDE, MODIFICADO_DESDE);

        assertThat(result.getExitosas()).isEqualTo(1);
        assertThat(result.getCreadas()).isZero();
        verify(ocupacionService, never()).guardarOcupacion(any(), anyBoolean(), anyBoolean());
        Sincronizacion registro = capturedSincronizacion();
        assertThat(registro.getProcesado()).isTrue();
        assertThat(registro.getCodigoError()).isNull();
    }

    // --- Field mapping: localización and tipo ---

    @Test
    void provinciaFoundMapsItsIdAndCcaa() throws Exception {
        OcupacionLegacy legacy = legacy(ID_OCUPACION_LEGACY, OcupacionLegacy.EstadoOcupacion.ocupado, null);
        legacy.setIdProvincia(15);
        givenLegacy(legacy);
        givenArtistaExists();
        givenSaveReturnsId(1L);
        when(provinciaRepository.findProvinciaByIdProvinciaLegacy(15)).thenReturn(Optional.of(provincia(3L, 12L)));

        service.sincronizarOcupacionesDesde(FECHA_DESDE, MODIFICADO_DESDE);

        OcupacionSaveDto dto = capturedSavedDto();
        assertThat(dto.getIdProvincia()).isEqualTo(3L);
        assertThat(dto.getIdCcaa()).isEqualTo(12L);
        assertThat(dto.getIdMunicipio()).isEqualTo(ConstantsGestmusica.ID_MUNICIPIO_PROVISIONAL);
    }

    @Test
    void provinciaWithoutCcaaFallsBackToProvisionalCcaa() throws Exception {
        OcupacionLegacy legacy = legacy(ID_OCUPACION_LEGACY, OcupacionLegacy.EstadoOcupacion.ocupado, null);
        legacy.setIdProvincia(15);
        givenLegacy(legacy);
        givenArtistaExists();
        givenSaveReturnsId(1L);
        when(provinciaRepository.findProvinciaByIdProvinciaLegacy(15)).thenReturn(Optional.of(provincia(3L, null)));

        service.sincronizarOcupacionesDesde(FECHA_DESDE, MODIFICADO_DESDE);

        OcupacionSaveDto dto = capturedSavedDto();
        assertThat(dto.getIdProvincia()).isEqualTo(3L);
        assertThat(dto.getIdCcaa()).isEqualTo(ConstantsGestmusica.ID_CCAA_PROVISIONAL);
    }

    @Test
    void unknownProvinciaAndMissingPoblacionUseProvisionalValues() throws Exception {
        givenLegacy(legacy(ID_OCUPACION_LEGACY, OcupacionLegacy.EstadoOcupacion.ocupado, null));
        givenArtistaExists();
        givenSaveReturnsId(1L);

        service.sincronizarOcupacionesDesde(FECHA_DESDE, MODIFICADO_DESDE);

        OcupacionSaveDto dto = capturedSavedDto();
        assertThat(dto.getIdProvincia()).isEqualTo(ConstantsGestmusica.ID_PROVINCIA_PROVISIONAL);
        assertThat(dto.getIdCcaa()).isEqualTo(ConstantsGestmusica.ID_CCAA_PROVISIONAL);
        assertThat(dto.getIdMunicipio()).isEqualTo(ConstantsGestmusica.ID_MUNICIPIO_PROVISIONAL);
        assertThat(dto.getLocalidad()).isEqualTo(ConstantsGestmusica.LOCALIDAD_PROVISIONAL);
        verify(municipioRepository, never()).findMunicipioByNombreLike(any(), any());
    }

    @Test
    void municipioIsSearchedByNormalizedNameAndClosestLengthMatchWins() throws Exception {
        OcupacionLegacy legacy = legacy(ID_OCUPACION_LEGACY, OcupacionLegacy.EstadoOcupacion.ocupado, null);
        legacy.setIdProvincia(15);
        legacy.setAyuntamiento("A Laracha Ó");
        givenLegacy(legacy);
        givenArtistaExists();
        givenSaveReturnsId(1L);
        when(provinciaRepository.findProvinciaByIdProvinciaLegacy(15)).thenReturn(Optional.of(provincia(3L, 12L)));
        when(municipioRepository.findMunicipioByNombreLike("LARACHA O", 3L))
                .thenReturn(Optional.of(List.of(municipio(100L, "Laracha O Concello"), municipio(101L, "Laracha"))));

        service.sincronizarOcupacionesDesde(FECHA_DESDE, MODIFICADO_DESDE);

        assertThat(capturedSavedDto().getIdMunicipio()).isEqualTo(101L);
    }

    @Test
    void ocupadoPaisMarksProvisionalAndForcesProvisionalLocation() throws Exception {
        OcupacionLegacy legacy = legacy(ID_OCUPACION_LEGACY, OcupacionLegacy.EstadoOcupacion.ocupado, "ocupado");
        legacy.setIdProvincia(15);
        givenLegacy(legacy);
        givenArtistaExists();
        givenSaveReturnsId(1L);
        when(provinciaRepository.findProvinciaByIdProvinciaLegacy(15)).thenReturn(Optional.of(provincia(3L, 12L)));

        service.sincronizarOcupacionesDesde(FECHA_DESDE, MODIFICADO_DESDE);

        OcupacionSaveDto dto = capturedSavedDto();
        assertThat(dto.getProvisional()).isTrue();
        assertThat(dto.getIdCcaa()).isEqualTo(ConstantsGestmusica.ID_CCAA_PROVISIONAL);
        assertThat(dto.getIdProvincia()).isEqualTo(ConstantsGestmusica.ID_PROVINCIA_PROVISIONAL);
        assertThat(dto.getIdMunicipio()).isEqualTo(ConstantsGestmusica.ID_MUNICIPIO_PROVISIONAL);
        assertThat(dto.getIdTipoOcupacion()).isEqualTo(TipoOcupacionEnum.OCUPADO.getId());
    }

    @Test
    void reservaPaisUsesProvisionalLocationAndReservadoTipoButNotProvisionalFlag() throws Exception {
        OcupacionLegacy legacy = legacy(ID_OCUPACION_LEGACY, OcupacionLegacy.EstadoOcupacion.ocupado, "Reserva");
        legacy.setIdProvincia(15);
        givenLegacy(legacy);
        givenArtistaExists();
        givenSaveReturnsId(1L);
        when(provinciaRepository.findProvinciaByIdProvinciaLegacy(15)).thenReturn(Optional.of(provincia(3L, 12L)));

        service.sincronizarOcupacionesDesde(FECHA_DESDE, MODIFICADO_DESDE);

        OcupacionSaveDto dto = capturedSavedDto();
        assertThat(dto.getProvisional()).isFalse();
        assertThat(dto.getIdProvincia()).isEqualTo(ConstantsGestmusica.ID_PROVINCIA_PROVISIONAL);
        assertThat(dto.getIdCcaa()).isEqualTo(ConstantsGestmusica.ID_CCAA_PROVISIONAL);
        assertThat(dto.getIdMunicipio()).isEqualTo(ConstantsGestmusica.ID_MUNICIPIO_PROVISIONAL);
        assertThat(dto.getIdTipoOcupacion()).isEqualTo(TipoOcupacionEnum.RESERVADO.getId());
        // estado text is copied before the RESERVA override (current behavior)
        assertThat(dto.getEstado()).isEqualTo("ocupado");
    }

    @Test
    void reservadoEstadoMapsToReservadoTipo() throws Exception {
        givenLegacy(legacy(ID_OCUPACION_LEGACY, OcupacionLegacy.EstadoOcupacion.reservado, null));
        givenArtistaExists();
        givenSaveReturnsId(1L);

        service.sincronizarOcupacionesDesde(FECHA_DESDE, MODIFICADO_DESDE);

        OcupacionSaveDto dto = capturedSavedDto();
        assertThat(dto.getEstado()).isEqualTo("reservado");
        assertThat(dto.getIdTipoOcupacion()).isEqualTo(TipoOcupacionEnum.RESERVADO.getId());
    }

    @Test
    void portugalPaisUsesPortugalLocation() throws Exception {
        givenLegacy(legacy(ID_OCUPACION_LEGACY, OcupacionLegacy.EstadoOcupacion.ocupado, "Portugal"));
        givenArtistaExists();
        givenSaveReturnsId(1L);

        service.sincronizarOcupacionesDesde(FECHA_DESDE, MODIFICADO_DESDE);

        OcupacionSaveDto dto = capturedSavedDto();
        assertThat(dto.getProvisional()).isFalse();
        assertThat(dto.getIdCcaa()).isEqualTo(ConstantsGestmusica.ID_CCAA_PORTUGAL);
        assertThat(dto.getIdProvincia()).isEqualTo(ConstantsGestmusica.ID_PROVINCIA_PORTUGAL);
        assertThat(dto.getIdMunicipio()).isEqualTo(ConstantsGestmusica.ID_MUNICIPIO_PORTUGAL);
    }

    // --- Borrado (ocupaciones removed in legacy) ---

    @Test
    void ocupacionesMissingInLegacyAreDeactivatedAndOrphanTarifaToo() {
        Set<Integer> idsLegacy = Set.of(1, 2);
        Tarifa huerfana = tarifa(10L);
        Tarifa compartida = tarifa(11L);
        Ocupacion borrada1 = ocupacionActiva(1L, huerfana);
        Ocupacion borrada2 = ocupacionActiva(2L, compartida);
        Ocupacion borradaSinTarifa = ocupacionActiva(3L, null);
        when(ocupacionLegacyService.findOcupacionLegacyFromGestmusicaLegacyDesdeMofidicaic(FECHA_DESDE, MODIFICADO_DESDE))
                .thenReturn(List.of());
        when(ocupacionLegacyService.findIdsOcupacionesFromDate(FECHA_DESDE)).thenReturn(Optional.of(idsLegacy));
        when(ocupacionRepository.findByIdOcupacionLegacyNotIn(FECHA_DESDE.atStartOfDay(), idsLegacy))
                .thenReturn(Optional.of(List.of(borrada1, borrada2, borradaSinTarifa)));
        when(ocupacionRepository.countActivasByTarifaId(10L)).thenReturn(0L);
        when(ocupacionRepository.countActivasByTarifaId(11L)).thenReturn(2L);

        LocalDateTime antes = LocalDateTime.now();
        SincronizacionResult result = service.sincronizarOcupacionesDesde(FECHA_DESDE, MODIFICADO_DESDE);

        assertThat(result.getEliminadas()).isEqualTo(3);
        assertThat(result.getErrorGeneral()).isNull();
        for (Ocupacion o : List.of(borrada1, borrada2, borradaSinTarifa)) {
            assertThat(o.isActivo()).isFalse();
            assertThat(o.getUsuarioModificacion()).isEqualTo("usuario_sincronizacion");
            assertThat(o.getFechaModificacion()).isAfterOrEqualTo(antes);
            verify(ocupacionRepository).save(o);
        }
        assertThat(huerfana.isActivo()).isFalse();
        assertThat(compartida.isActivo()).isTrue();
        verify(tarifaRepository).save(huerfana);
        verify(tarifaRepository, never()).save(compartida);
        verifyNoInteractions(sincronizacionRepository);
    }

    @Test
    void noDeletionWhenLegacyReturnsNoIds() {
        when(ocupacionLegacyService.findOcupacionLegacyFromGestmusicaLegacyDesdeMofidicaic(FECHA_DESDE, MODIFICADO_DESDE))
                .thenReturn(List.of());
        when(ocupacionLegacyService.findIdsOcupacionesFromDate(FECHA_DESDE)).thenReturn(Optional.of(Set.of()));

        SincronizacionResult result = service.sincronizarOcupacionesDesde(FECHA_DESDE, MODIFICADO_DESDE);

        assertThat(result.getEliminadas()).isZero();
        verifyNoInteractions(ocupacionRepository, tarifaRepository);
    }

    @Test
    void deletionFailureIsSwallowedAndKeepsCountOfAlreadyDeactivated() {
        Set<Integer> idsLegacy = Set.of(1);
        Ocupacion primera = ocupacionActiva(1L, null);
        Ocupacion segunda = ocupacionActiva(2L, null);
        when(ocupacionLegacyService.findOcupacionLegacyFromGestmusicaLegacyDesdeMofidicaic(FECHA_DESDE, MODIFICADO_DESDE))
                .thenReturn(List.of());
        when(ocupacionLegacyService.findIdsOcupacionesFromDate(FECHA_DESDE)).thenReturn(Optional.of(idsLegacy));
        when(ocupacionRepository.findByIdOcupacionLegacyNotIn(FECHA_DESDE.atStartOfDay(), idsLegacy))
                .thenReturn(Optional.of(List.of(primera, segunda)));
        lenient().when(ocupacionRepository.save(segunda)).thenThrow(new IllegalStateException("db"));

        SincronizacionResult result = service.sincronizarOcupacionesDesde(FECHA_DESDE, MODIFICADO_DESDE);

        assertThat(result.getEliminadas()).isEqualTo(1);
        assertThat(result.getErrorGeneral()).isNull();
    }

    // --- Per-artista sync ---

    @Test
    void artistaSyncReadsOnlyThatArtistaAndAppliesSameProcessing() throws Exception {
        when(ocupacionLegacyService.findOcupacionLegacyArtistaFromGestmusicaLegacyDesdeMofidicaic(FECHA_DESDE, MODIFICADO_DESDE, ID_ARTISTA_LEGACY))
                .thenReturn(List.of(legacy(ID_OCUPACION_LEGACY, OcupacionLegacy.EstadoOcupacion.ocupado, null)));
        givenArtistaExists();
        givenSaveReturnsId(1L);

        SincronizacionResult result = service.sincronizarOcupacionesArtista(FECHA_DESDE, MODIFICADO_DESDE, ID_ARTISTA_LEGACY);

        assertThat(result.getCreadas()).isEqualTo(1);
        assertThat(result.getExitosas()).isEqualTo(1);
        assertThat(capturedSincronizacion().getProcesado()).isTrue();
        verify(ocupacionLegacyService, never()).findOcupacionLegacyFromGestmusicaLegacyDesdeMofidicaic(any(), any());
        verify(ocupacionLegacyService).findIdsOcupacionesFromDate(FECHA_DESDE);
    }

    @Test
    void artistaSyncReadFailureIsReportedAsGeneralError() {
        when(ocupacionLegacyService.findOcupacionLegacyArtistaFromGestmusicaLegacyDesdeMofidicaic(FECHA_DESDE, MODIFICADO_DESDE, ID_ARTISTA_LEGACY))
                .thenThrow(new IllegalStateException("timeout"));

        SincronizacionResult result = service.sincronizarOcupacionesArtista(FECHA_DESDE, MODIFICADO_DESDE, ID_ARTISTA_LEGACY);

        assertThat(result.getErrorGeneral()).isEqualTo("timeout");
        verifyNoInteractions(sincronizacionRepository, ocupacionService);
    }

    // --- helpers ---

    private void givenLegacy(OcupacionLegacy legacy) {
        when(ocupacionLegacyService.findOcupacionLegacyFromGestmusicaLegacyDesdeMofidicaic(FECHA_DESDE, MODIFICADO_DESDE))
                .thenReturn(List.of(legacy));
    }

    private void givenArtistaExists() {
        when(artistaRepository.findArtistaByIdArtistaGestmanager((long) ID_ARTISTA_LEGACY)).thenReturn(Optional.of(ARTISTA));
    }

    private void givenSaveReturnsId(Long id) throws ModificacionOcupacionException {
        Ocupacion guardada = new Ocupacion();
        guardada.setId(id);
        when(ocupacionService.guardarOcupacion(any(OcupacionSaveDto.class), eq(true), eq(true))).thenReturn(guardada);
    }

    private OcupacionSaveDto capturedSavedDto() throws ModificacionOcupacionException {
        ArgumentCaptor<OcupacionSaveDto> captor = ArgumentCaptor.forClass(OcupacionSaveDto.class);
        verify(ocupacionService).guardarOcupacion(captor.capture(), eq(true), eq(true));
        return captor.getValue();
    }

    private Sincronizacion capturedSincronizacion() {
        ArgumentCaptor<Sincronizacion> captor = ArgumentCaptor.forClass(Sincronizacion.class);
        verify(sincronizacionRepository).save(captor.capture());
        return captor.getValue();
    }

    private static OcupacionLegacy legacy(int idOcupacion, OcupacionLegacy.EstadoOcupacion estado, String pais) {
        OcupacionLegacy legacy = new OcupacionLegacy();
        legacy.setIdOcupacion(idOcupacion);
        legacy.setIdArtista(ID_ARTISTA_LEGACY);
        legacy.setFecha(LocalDate.of(2025, 7, 15));
        legacy.setEstado(estado);
        legacy.setPais(pais);
        return legacy;
    }

    private static Provincia provincia(long id, Long idCcaa) {
        Provincia provincia = new Provincia();
        provincia.setId(id);
        if (idCcaa != null) {
            Ccaa ccaa = new Ccaa();
            ccaa.setId(idCcaa);
            provincia.setCcaa(ccaa);
        }
        return provincia;
    }

    private static Municipio municipio(long id, String nombre) {
        Municipio municipio = new Municipio();
        municipio.setId(id);
        municipio.setNombre(nombre);
        return municipio;
    }

    private static Tarifa tarifa(Long id) {
        Tarifa tarifa = new Tarifa();
        tarifa.setId(id);
        tarifa.setActivo(true);
        return tarifa;
    }

    private static Ocupacion ocupacionActiva(Long id, Tarifa tarifa) {
        Ocupacion ocupacion = new Ocupacion();
        ocupacion.setId(id);
        ocupacion.setActivo(true);
        ocupacion.setTarifa(tarifa);
        return ocupacion;
    }
}
