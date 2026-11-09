package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.agencia.AgenciaRecord;
import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.artista.ArtistaDto;
import es.musicalia.gestmusica.artista.ArtistaRecord;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import es.musicalia.gestmusica.localizacion.Ccaa;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.observabilidad.FunctionalEventNames;
import es.musicalia.gestmusica.observabilidad.FunctionalEventOutcome;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.permiso.PermisoAgenciaEnum;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Characterization tests of OcupacionController branches: save outcomes and their
 * functional events, confirm/anular tracking, creation permission per artista,
 * ODG endpoint error wrapping and the DataTables listado contract.
 */
@ExtendWith(MockitoExtension.class)
class OcupacionControllerTest {

    @Mock
    private OcupacionService ocupacionService;
    @Mock
    private AgenciaService agenciaService;
    @Mock
    private ArtistaService artistaService;
    @Mock
    private LocalizacionService localizacionService;
    @Mock
    private UserService userService;
    @Mock
    private FunctionalEventTracker functionalEventTracker;

    @InjectMocks
    private OcupacionController controller;

    private CustomAuthenticatedUser user;

    @BeforeEach
    void setUp() {
        user = authenticatedUser(
                Map.of(5L, Set.of("OCUPACIONES", PermisoAgenciaEnum.VER_DATOS_ECONOMICOS.name()),
                        6L, Set.of("OCUPACIONES"),
                        7L, Set.of("VER_TARIFAS")),
                Map.of(10L, Set.of("VER_OCUPACIONES")));
    }

    @Nested
    @DisplayName("POST /ocupacion/save")
    class Save {

        @Test
        @DisplayName("Existing ocupación on that date: error response, service save not called, creation FAILURE tracked")
        void duplicada_noGuardaYTrackeaFallo() throws Exception {
            OcupacionSaveDto dto = dto(null);
            when(ocupacionService.existeOcupacionFecha(dto)).thenReturn(true);

            ResponseEntity<?> response = controller.saveOcupacion(user, dto);

            DefaultResponseBody body = (DefaultResponseBody) response.getBody();
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(body.isSuccess()).isFalse();
            assertThat(body.getMessage()).isEqualTo("Ya existe una ocupación en esa fecha");
            assertThat(body.getMessageType()).isEqualTo("error");
            verify(ocupacionService, never()).saveOcupacion(any());
            verify(functionalEventTracker).track(FunctionalEventNames.OCUPACION_CREATED, FunctionalEventOutcome.FAILURE,
                    1L, "usuario.prueba", Map.of("reason", "ocupacion_duplicada"));
        }

        @Test
        @DisplayName("Existing ocupación on edit: error response and no functional event")
        void duplicadaEnEdicion_noTrackea() throws Exception {
            OcupacionSaveDto dto = dto(33L);
            when(ocupacionService.existeOcupacionFecha(dto)).thenReturn(true);

            DefaultResponseBody body = (DefaultResponseBody) controller.saveOcupacion(user, dto).getBody();

            assertThat(body.isSuccess()).isFalse();
            assertThat(body.getMessage()).isEqualTo("Ya existe una ocupación en esa fecha");
            verify(ocupacionService, never()).saveOcupacion(any());
            verifyNoInteractions(functionalEventTracker);
        }

        @Test
        @DisplayName("Creation success: returns the service body and tracks SUCCESS with the new id")
        void creacionCorrecta_devuelveRespuestaServicioYTrackea() throws Exception {
            OcupacionSaveDto dto = dto(null);
            DefaultResponseBody serviceBody = DefaultResponseBody.builder().success(true)
                    .message("Ocupacion guardada correctamente").messageType("success").idEntidad(99L).build();
            when(ocupacionService.existeOcupacionFecha(dto)).thenReturn(false);
            when(ocupacionService.saveOcupacion(dto)).thenReturn(serviceBody);

            ResponseEntity<?> response = controller.saveOcupacion(user, dto);

            assertThat(response.getBody()).isSameAs(serviceBody);
            verify(functionalEventTracker).track(FunctionalEventNames.OCUPACION_CREATED, FunctionalEventOutcome.SUCCESS,
                    1L, "usuario.prueba", Map.of("ocupacion_id", 99L));
        }

        @Test
        @DisplayName("Creation answered with success=false: tracks FAILURE and an empty id attribute")
        void creacionSinExito_trackeaFalloConIdVacio() throws Exception {
            OcupacionSaveDto dto = dto(null);
            DefaultResponseBody serviceBody = DefaultResponseBody.builder().success(false).build();
            when(ocupacionService.existeOcupacionFecha(dto)).thenReturn(false);
            when(ocupacionService.saveOcupacion(dto)).thenReturn(serviceBody);

            controller.saveOcupacion(user, dto);

            verify(functionalEventTracker).track(FunctionalEventNames.OCUPACION_CREATED, FunctionalEventOutcome.FAILURE,
                    1L, "usuario.prueba", Map.of("ocupacion_id", ""));
        }

        @Test
        @DisplayName("Edit success: returns the service body without functional event")
        void edicionCorrecta_noTrackea() throws Exception {
            OcupacionSaveDto dto = dto(33L);
            DefaultResponseBody serviceBody = DefaultResponseBody.builder().success(true).idEntidad(33L).build();
            when(ocupacionService.existeOcupacionFecha(dto)).thenReturn(false);
            when(ocupacionService.saveOcupacion(dto)).thenReturn(serviceBody);

            assertThat(controller.saveOcupacion(user, dto).getBody()).isSameAs(serviceBody);
            verifyNoInteractions(functionalEventTracker);
        }

        @Test
        @DisplayName("ModificacionOcupacionException on edit: permission error body, no event")
        void modificacionSinPermiso_devuelveErrorPermisos() throws Exception {
            OcupacionSaveDto dto = dto(33L);
            when(ocupacionService.existeOcupacionFecha(dto)).thenReturn(false);
            when(ocupacionService.saveOcupacion(dto)).thenThrow(new ModificacionOcupacionException("No tiene permisos"));

            DefaultResponseBody body = (DefaultResponseBody) controller.saveOcupacion(user, dto).getBody();

            assertThat(body.isSuccess()).isFalse();
            assertThat(body.getMessage()).isEqualTo("No tiene permisos para modificar la ocupación de otros usuarios");
            assertThat(body.getMessageType()).isEqualTo("error");
            verifyNoInteractions(functionalEventTracker);
        }

        @Test
        @DisplayName("ModificacionOcupacionException on creation: tracks FAILURE with reason sin_permisos")
        void creacionSinPermiso_trackeaSinPermisos() throws Exception {
            OcupacionSaveDto dto = dto(null);
            when(ocupacionService.existeOcupacionFecha(dto)).thenReturn(false);
            when(ocupacionService.saveOcupacion(dto)).thenThrow(new ModificacionOcupacionException("No tiene permisos"));

            DefaultResponseBody body = (DefaultResponseBody) controller.saveOcupacion(user, dto).getBody();

            assertThat(body.getMessage()).isEqualTo("No tiene permisos para modificar la ocupación de otros usuarios");
            verify(functionalEventTracker).track(FunctionalEventNames.OCUPACION_CREATED, FunctionalEventOutcome.FAILURE,
                    1L, "usuario.prueba", Map.of("reason", "sin_permisos"));
        }

        @Test
        @DisplayName("Unexpected exception on creation: generic error body and reason unexpected_error")
        void excepcionInesperada_devuelveErrorGenerico() throws Exception {
            OcupacionSaveDto dto = dto(null);
            when(ocupacionService.existeOcupacionFecha(dto)).thenReturn(false);
            when(ocupacionService.saveOcupacion(dto)).thenThrow(new IllegalStateException("boom"));

            DefaultResponseBody body = (DefaultResponseBody) controller.saveOcupacion(user, dto).getBody();

            assertThat(body.isSuccess()).isFalse();
            assertThat(body.getMessage()).isEqualTo("Error inesperado al guardar");
            assertThat(body.getMessageType()).isEqualTo("error");
            verify(functionalEventTracker).track(FunctionalEventNames.OCUPACION_CREATED, FunctionalEventOutcome.FAILURE,
                    1L, "usuario.prueba", Map.of("reason", "unexpected_error"));
        }
    }

    @Nested
    @DisplayName("GET /ocupacion/anular|confirmar/{id}")
    class EstadoEndpoints {

        @Test
        @DisplayName("anular returns the service body and tracks OCUPACION_CANCELLED SUCCESS")
        void anular_trackeaCancelacion() {
            DefaultResponseBody serviceBody = DefaultResponseBody.builder().success(true).message("Ocupacion anulada correctamente").build();
            when(ocupacionService.anularOcupacion(12L)).thenReturn(serviceBody);

            ResponseEntity<DefaultResponseBody> response = controller.anularOcupacion(user, 12L);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(serviceBody);
            verify(functionalEventTracker).track(FunctionalEventNames.OCUPACION_CANCELLED, FunctionalEventOutcome.SUCCESS,
                    1L, "usuario.prueba", Map.of("ocupacion_id", 12L));
        }

        @Test
        @DisplayName("anular with success=false and no principal tracks FAILURE with null user data")
        void anularSinExitoSinUsuario_trackeaFallo() {
            when(ocupacionService.anularOcupacion(12L)).thenReturn(DefaultResponseBody.builder().success(false).build());

            controller.anularOcupacion(null, 12L);

            verify(functionalEventTracker).track(eq(FunctionalEventNames.OCUPACION_CANCELLED), eq(FunctionalEventOutcome.FAILURE),
                    isNull(), isNull(), eq(Map.of("ocupacion_id", 12L)));
        }

        @Test
        @DisplayName("confirmar returns the service body and tracks OCUPACION_CONFIRMED")
        void confirmar_trackeaConfirmacion() {
            DefaultResponseBody serviceBody = DefaultResponseBody.builder().success(true).message("Ocupacion confirmada correctamente").build();
            when(ocupacionService.confirmarOcupacion(13L)).thenReturn(serviceBody);

            ResponseEntity<DefaultResponseBody> response = controller.confirmarOcupacion(user, 13L);

            assertThat(response.getBody()).isSameAs(serviceBody);
            verify(functionalEventTracker).track(FunctionalEventNames.OCUPACION_CONFIRMED, FunctionalEventOutcome.SUCCESS,
                    1L, "usuario.prueba", Map.of("ocupacion_id", 13L));
        }

        @Test
        @DisplayName("get/{id} returns the edit DTO from the service")
        void getEditDto_devuelveDtoServicio() {
            OcupacionEditDto editDto = new OcupacionEditDto(14L, LocalDateTime.of(2025, 8, 15, 22, 0), 5L, "1000", "10", "21",
                    1L, "Ocupado", 12L, 15L, "A Coruña", 150L, "Santiago", "Conxo", "Praza", false, false, "Ocupado",
                    null, false, null, 1L, false, null, null, true);
            when(ocupacionService.findOcupacionEditDtoByArtistaIdAndDates(14L)).thenReturn(editDto);

            assertThat(controller.getOcupacionEditDtoByArtistaIdAndDates(14L).getBody()).isSameAs(editDto);
        }
    }

    @Nested
    @DisplayName("GET /ocupacion/nueva/{idArtista} and /ocupacion/{id}")
    class Formularios {

        @Test
        @DisplayName("Artista not in the user's permission map: AccessDeniedException, nothing loaded")
        void nueva_artistaSinPermisos_accesoDenegado() {
            Model model = new BindingAwareModelMap();

            assertThatThrownBy(() -> controller.getNuevaOcupacionModelAndView(user, model, 99L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("No tiene permisos para crear ocupaciones para este artista");
            verifyNoInteractions(artistaService, ocupacionService);
            assertThat(model.asMap()).isEmpty();
        }

        @Test
        @DisplayName("Artista with permissions but without OCUPACIONES: AccessDeniedException")
        void nueva_artistaSinPermisoOcupaciones_accesoDenegado() {
            assertThatThrownBy(() -> controller.getNuevaOcupacionModelAndView(user, new BindingAwareModelMap(), 7L))
                    .isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(artistaService, ocupacionService);
        }

        @Test
        @DisplayName("Artista with OCUPACIONES: new DTO with zero amounts, user location and only artistas with OCUPACIONES")
        void nueva_conPermiso_preparaFormulario() {
            ArtistaDto artista = new ArtistaDto();
            artista.setIdAgencia(10L);
            artista.setPermiteOrquestasDeGalicia(true);
            artista.setPublicarEventos(true);
            Ccaa ccaa = new Ccaa();
            ccaa.setId(12L);
            Provincia provincia = new Provincia();
            provincia.setId(15L);
            Usuario usuario = user.getUsuario();
            List<ArtistaRecord> misArtistas = List.of(new ArtistaRecord(5L, "Orquesta", null, null, null, null, null, null, null, null));
            List<CodigoNombreRecord> provincias = List.of(new CodigoNombreRecord(15L, "A Coruña"));
            List<CodigoNombreRecord> municipios = List.of(new CodigoNombreRecord(150L, "Santiago"));
            List<CodigoNombreRecord> localidades = List.of(new CodigoNombreRecord(1500L, "Conxo"));

            when(artistaService.findArtistaDtoById(5L)).thenReturn(artista);
            when(localizacionService.findCcaaByUsuarioId(1L)).thenReturn(ccaa);
            when(localizacionService.findProvinciaByUsuarioId(1L)).thenReturn(provincia);
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuario));
            when(userService.isUserAutheticated()).thenReturn(true);
            when(localizacionService.findAllProvinciasByCcaaId(12L)).thenReturn(provincias);
            when(localizacionService.findAllMunicipiosByIdProvincia(15L)).thenReturn(municipios);
            when(localizacionService.findLocalidadByIdMunicipio(150L)).thenReturn(localidades);
            when(artistaService.findMisArtistas(Set.of(5L, 6L))).thenReturn(misArtistas);
            Model model = new BindingAwareModelMap();

            String view = controller.getNuevaOcupacionModelAndView(user, model, 5L);

            assertThat(view).isEqualTo("ocupacion-detail");
            OcupacionSaveDto dto = (OcupacionSaveDto) model.getAttribute("ocupacionDto");
            assertThat(dto.getId()).isNull();
            assertThat(dto.getIdArtista()).isEqualTo(5L);
            assertThat(dto.getIdAgencia()).isEqualTo(10L);
            assertThat(dto.getIdCcaa()).isEqualTo(12L);
            assertThat(dto.getIdProvincia()).isEqualTo(15L);
            assertThat(dto.getImporte()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(dto.getPorcentajeRepre()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(dto.getIva()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(dto.getIdUsuario()).isEqualTo(1L);
            assertThat(model.getAttribute("listaArtistas")).isEqualTo(misArtistas);
            assertThat(model.getAttribute("listaProvinciasCcaaArtista")).isEqualTo(provincias);
            assertThat(model.getAttribute("listaMunicipioListado")).isEqualTo(municipios);
            assertThat(model.getAttribute("listaLocalidadesListado")).isEqualTo(localidades);
            assertThat(model.getAttribute("isArtistaPermiteOdg")).isEqualTo(true);
            assertThat(model.getAttribute("isArtistaPublicarEventos")).isEqualTo(true);
            assertThat(model.getAttribute("idUsuarioAutenticado")).isEqualTo(1L);
            verify(ocupacionService).listarTiposOcupacion(5L);
        }

        @Test
        @DisplayName("Detail view loads the saved DTO and only its own artista")
        void detalle_cargaDtoYArtista() {
            OcupacionSaveDto saved = dto(40L);
            saved.setIdCcaa(12L);
            saved.setIdProvincia(15L);
            saved.setIdMunicipio(150L);
            ArtistaDto artista = new ArtistaDto();
            when(ocupacionService.getOcupacionSaveDto(40L)).thenReturn(saved);
            when(artistaService.findArtistaDtoById(5L)).thenReturn(artista);
            when(localizacionService.findAllProvinciasByCcaaId(12L)).thenReturn(List.of(new CodigoNombreRecord(15L, "A Coruña")));
            when(localizacionService.findAllMunicipiosByIdProvincia(15L)).thenReturn(List.of(new CodigoNombreRecord(150L, "Santiago")));
            when(userService.isUserAutheticated()).thenReturn(false);
            Model model = new BindingAwareModelMap();

            String view = controller.getOcupacionModelAndView(user, model, 40L);

            assertThat(view).isEqualTo("ocupacion-detail");
            assertThat(model.getAttribute("ocupacionDto")).isSameAs(saved);
            assertThat(model.getAttribute("listaArtistas")).isEqualTo(List.of(artista));
            assertThat(model.getAttribute("idUsuarioAutenticado")).isNull();
            assertThat(model.getAttribute("isArtistaPermiteOdg")).isEqualTo(false);
            verify(localizacionService).findLocalidadByIdMunicipio(150L);
        }
    }

    @Nested
    @DisplayName("ODG and visibility endpoints")
    class OdgEndpoints {

        @Test
        @DisplayName("Service results are returned unchanged")
        void devuelvenRespuestaServicio() {
            DefaultResponseBody publicar = DefaultResponseBody.builder().success(true).build();
            DefaultResponseBody actualizar = DefaultResponseBody.builder().success(true).build();
            DefaultResponseBody eliminar = DefaultResponseBody.builder().success(true).build();
            DefaultResponseBody visibilidad = DefaultResponseBody.builder().success(true).build();
            when(ocupacionService.publicarOcupacionOrquestasDeGalicia(1L)).thenReturn(publicar);
            when(ocupacionService.actualizarOcupacionOrquestasDeGalicia(1L)).thenReturn(actualizar);
            when(ocupacionService.eliminarOcupacionOrquestasDeGalicia(1L)).thenReturn(eliminar);
            when(ocupacionService.toggleVisibilidadEvento(1L)).thenReturn(visibilidad);

            assertThat(controller.publicarEnOrquestasDeGalicia(1L).getBody()).isSameAs(publicar);
            assertThat(controller.actualizarEnOrquestasDeGalicia(1L).getBody()).isSameAs(actualizar);
            assertThat(controller.eliminarDeOrquestasDeGalicia(1L).getBody()).isSameAs(eliminar);
            assertThat(controller.toggleVisibilidadEvento(1L).getBody()).isSameAs(visibilidad);
        }

        @Test
        @DisplayName("Exceptions become 200 error bodies with the exception message")
        void excepcionesSeConviertenEnError() {
            when(ocupacionService.publicarOcupacionOrquestasDeGalicia(1L)).thenThrow(new RuntimeException("caido"));
            when(ocupacionService.actualizarOcupacionOrquestasDeGalicia(1L)).thenThrow(new RuntimeException("caido"));
            when(ocupacionService.eliminarOcupacionOrquestasDeGalicia(1L)).thenThrow(new RuntimeException("caido"));
            when(ocupacionService.toggleVisibilidadEvento(1L)).thenThrow(new RuntimeException("caido"));

            assertError(controller.publicarEnOrquestasDeGalicia(1L), "Error al publicar en OrquestasDeGalicia: caido");
            assertError(controller.actualizarEnOrquestasDeGalicia(1L), "Error al actualizar en OrquestasDeGalicia: caido");
            assertError(controller.eliminarDeOrquestasDeGalicia(1L), "Error al eliminar de OrquestasDeGalicia: caido");
            assertError(controller.toggleVisibilidadEvento(1L), "Error al cambiar visibilidad del evento: caido");
        }

        private void assertError(ResponseEntity<DefaultResponseBody> response, String message) {
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getMessageType()).isEqualTo("error");
            assertThat(response.getBody().getMessage()).isEqualTo(message);
        }
    }

    @Nested
    @DisplayName("POST /ocupacion/list/data")
    class ListadoData {

        @Test
        @DisplayName("Builds filters, page, sort and rows; importe only with mostrarImportes and VER_DATOS_ECONOMICOS on the artista")
        void construyeFiltrosPaginaYFilas() {
            OcupacionListRecord conPermiso = record(1L, 5L, "1500");
            OcupacionListRecord sinPermiso = record(2L, 6L, "900");
            when(ocupacionService.findOcupacionesByArtistasListAndDatesActivoPaginado(eq(user), any(), eq("orq"), any()))
                    .thenReturn(new PageImpl<>(List.of(conPermiso, sinPermiso), PageRequest.of(2, 10), 22));
            when(ocupacionService.findOcupacionesByArtistasListAndDatesActivoPaginado(eq(user), any(), isNull(), eq(PageRequest.of(0, 1))))
                    .thenReturn(new PageImpl<>(List.of(conPermiso), PageRequest.of(0, 1), 40));

            Map<String, Object> response = controller.getListadoOcupacionesData(
                    user, 3, 20, 10, "10", "5", "01-07-2025", "31-07-2025", "orq", 2, "asc", true);

            ArgumentCaptor<OcupacionListFilterDto> filterCaptor = ArgumentCaptor.forClass(OcupacionListFilterDto.class);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(ocupacionService).findOcupacionesByArtistasListAndDatesActivoPaginado(eq(user), filterCaptor.capture(), eq("orq"), pageableCaptor.capture());
            OcupacionListFilterDto filtro = filterCaptor.getValue();
            assertThat(filtro.getIdAgencia()).isEqualTo(10L);
            assertThat(filtro.getIdArtista()).isEqualTo(5L);
            assertThat(filtro.getFechaDesde()).isEqualTo(LocalDate.of(2025, 7, 1));
            assertThat(filtro.getFechaHasta()).isEqualTo(LocalDate.of(2025, 7, 31));
            assertThat(filtro.isMostrarImportes()).isTrue();
            assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(2, 10, Sort.by(Sort.Direction.ASC, "fecha")));

            assertThat(response.get("draw")).isEqualTo(3);
            assertThat(response.get("recordsTotal")).isEqualTo(40L);
            assertThat(response.get("recordsFiltered")).isEqualTo(22L);
            assertThat(response).doesNotContainKey("error");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) response.get("data");
            assertThat(rows).hasSize(2);
            assertThat(rows.get(0))
                    .containsEntry("id", 1L)
                    .containsEntry("artista", "Artista 5")
                    .containsEntry("start", "15/08/2025")
                    .containsEntry("fechaCreacion", "01/06/2025")
                    .containsEntry("municipioProvincia", "Santiago, A Coruña")
                    .containsEntry("estado", "Ocupado")
                    .containsEntry("importe", "1500");
            assertThat(rows.get(1)).containsEntry("importe", "");
        }

        @Test
        @DisplayName("Defaults: fechaDesde today, length<=0 uses 10, unknown column sorts by fechaCreacion desc, importe hidden")
        void valoresPorDefecto() {
            when(ocupacionService.findOcupacionesByArtistasListAndDatesActivoPaginado(any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(record(1L, 5L, "1500"))));
            LocalDate before = LocalDate.now();

            Map<String, Object> response = controller.getListadoOcupacionesData(
                    user, 1, 0, 0, null, " ", null, "", null, 99, "desc", false);

            LocalDate after = LocalDate.now();
            ArgumentCaptor<OcupacionListFilterDto> filterCaptor = ArgumentCaptor.forClass(OcupacionListFilterDto.class);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            // first call: requested page; second call: total count with PageRequest.of(0, 1)
            verify(ocupacionService, org.mockito.Mockito.times(2)).findOcupacionesByArtistasListAndDatesActivoPaginado(eq(user), filterCaptor.capture(), isNull(), pageableCaptor.capture());
            OcupacionListFilterDto filtro = filterCaptor.getAllValues().get(0);
            assertThat(filtro.getIdAgencia()).isNull();
            assertThat(filtro.getIdArtista()).isNull();
            assertThat(filtro.getFechaDesde()).isBetween(before, after);
            assertThat(filtro.getFechaHasta()).isNull();
            assertThat(filtro.isMostrarImportes()).isFalse();
            assertThat(pageableCaptor.getAllValues()).containsExactly(
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "fechaCreacion")), PageRequest.of(0, 1));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) response.get("data");
            assertThat(rows.get(0)).containsEntry("importe", "");
        }

        @Test
        @DisplayName("Service failure: DataTables error payload with zero totals")
        void errorServicio_devuelvePayloadError() {
            when(ocupacionService.findOcupacionesByArtistasListAndDatesActivoPaginado(any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("db"));

            Map<String, Object> response = controller.getListadoOcupacionesData(
                    user, 4, 0, 10, null, null, null, null, null, 1, "desc", false);

            assertThat(response)
                    .containsEntry("draw", 4)
                    .containsEntry("recordsTotal", 0)
                    .containsEntry("recordsFiltered", 0)
                    .containsEntry("data", List.of())
                    .containsEntry("error", "Error al cargar las ocupaciones");
        }

        @Test
        @DisplayName("Malformed date: handled as service error payload")
        void fechaMalFormada_devuelvePayloadError() {
            Map<String, Object> response = controller.getListadoOcupacionesData(
                    user, 1, 0, 10, null, null, "2025-07-01", null, null, 1, "desc", false);

            assertThat(response).containsEntry("error", "Error al cargar las ocupaciones");
            verifyNoInteractions(ocupacionService);
        }
    }

    @Nested
    @DisplayName("GET/POST /ocupacion/list")
    class Listado {

        @Test
        @DisplayName("POST keeps the filter as flash attribute and redirects to the list")
        void post_redirigeConFiltroFlash() {
            OcupacionListFilterDto filtro = OcupacionListFilterDto.builder().idAgencia(10L).fechaDesde(LocalDate.of(2025, 1, 1)).build();
            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

            String view = controller.postListadoOcupaciones(user, filtro, new BindingAwareModelMap(), redirectAttributes);

            assertThat(view).isEqualTo("redirect:/ocupacion/list");
            assertThat(redirectAttributes.getFlashAttributes().get("ocupacionListFilterDto")).isSameAs(filtro);
        }

        @Test
        @DisplayName("GET without flash filter: default filter, empty list, artistas of the first agencia, importes visibility")
        void get_sinFiltro_modeloPorDefecto() {
            AgenciaRecord agencia = new AgenciaRecord(10L, "Agencia", null, null, null, null, null, null, null, null);
            List<ArtistaDto> artistasAgencia = List.of(new ArtistaDto());
            List<ArtistaRecord> misArtistas = List.of(new ArtistaRecord(5L, "Orquesta", null, null, null, null, null, null, null, null));
            when(agenciaService.findMisAgencias(Set.of(10L))).thenReturn(List.of(agencia));
            when(artistaService.findAllArtistasByAgenciaId(10L)).thenReturn(artistasAgencia);
            when(artistaService.findMisArtistas(Set.of(5L, 6L))).thenReturn(misArtistas);
            LocalDate before = LocalDate.now();
            Model model = new BindingAwareModelMap();

            String view = controller.getListadoOcupaciones(user, model);

            assertThat(view).isEqualTo("ocupaciones");
            OcupacionListFilterDto filtro = (OcupacionListFilterDto) model.getAttribute("ocupacionListFilterDto");
            assertThat(filtro.getFechaDesde()).isBetween(before.minusMonths(2), LocalDate.now().minusMonths(2));
            assertThat(filtro.getIdAgencia()).isNull();
            assertThat(model.getAttribute("listaOcupaciones")).isEqualTo(List.of());
            assertThat(model.getAttribute("listaAgencias")).isEqualTo(List.of(agencia));
            assertThat(model.getAttribute("listaArtistas")).isEqualTo(artistasAgencia);
            assertThat(model.getAttribute("listaArtistasPermisosOcupacion")).isEqualTo(misArtistas);
            assertThat(model.getAttribute("puedeVerImportes")).isEqualTo(true);
        }

        @Test
        @DisplayName("GET without agencias: no agencia/artista lists and importes hidden without VER_DATOS_ECONOMICOS")
        void get_sinAgencias_noCargaListasAgencia() {
            CustomAuthenticatedUser sinAgencias = authenticatedUser(Map.of(6L, Set.of("OCUPACIONES")), Map.of());
            when(agenciaService.findMisAgencias(Set.of())).thenReturn(List.of());
            Model model = new BindingAwareModelMap();

            controller.getListadoOcupaciones(sinAgencias, model);

            assertThat(model.containsAttribute("listaAgencias")).isFalse();
            assertThat(model.containsAttribute("listaArtistas")).isFalse();
            assertThat(model.getAttribute("puedeVerImportes")).isEqualTo(false);
            verify(artistaService, never()).findAllArtistasByAgenciaId(anyLong());
        }
    }

    @Nested
    @DisplayName("Exports")
    class Exports {

        @Test
        @DisplayName("Excel: octet-stream form-data disposition named attachment, Ocupaciones_<timestamp>.xlsx with the service bytes")
        void excel_devuelveAdjunto() {
            OcupacionListFilterDto filtro = OcupacionListFilterDto.builder().idAgencia(10L).build();
            java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
            stream.writeBytes(new byte[]{1, 2, 3});
            when(ocupacionService.exportOcupacionesToExcel(user, filtro)).thenReturn(stream);

            ResponseEntity<byte[]> response = controller.exportarOcupacionesExcel(user, filtro);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsExactly(1, 2, 3);
            assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/octet-stream");
            // setContentDispositionFormData("attachment", ...) yields form-data; name="attachment"
            assertThat(response.getHeaders().getContentDisposition().getType()).isEqualTo("form-data");
            assertThat(response.getHeaders().getContentDisposition().getName()).isEqualTo("attachment");
            assertThat(response.getHeaders().getContentDisposition().getFilename()).matches("Ocupaciones_\\d{14}\\.xlsx");
        }

        @Test
        @DisplayName("PDF: application/pdf form-data disposition named attachment, Ocupaciones_<timestamp>.pdf with the service bytes")
        void pdf_devuelveAdjunto() {
            OcupacionListFilterDto filtro = OcupacionListFilterDto.builder().build();
            when(ocupacionService.exportOcupacionesToPDF(user, filtro)).thenReturn(new byte[]{9, 8});

            ResponseEntity<byte[]> response = controller.exportarOcupacionesPDF(user, filtro);

            assertThat(response.getBody()).containsExactly(9, 8);
            assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/pdf");
            // setContentDispositionFormData("attachment", ...) yields form-data; name="attachment"
            assertThat(response.getHeaders().getContentDisposition().getType()).isEqualTo("form-data");
            assertThat(response.getHeaders().getContentDisposition().getName()).isEqualTo("attachment");
            assertThat(response.getHeaders().getContentDisposition().getFilename()).matches("Ocupaciones_\\d{14}\\.pdf");
        }
    }

    private static OcupacionSaveDto dto(Long id) {
        OcupacionSaveDto dto = new OcupacionSaveDto();
        dto.setId(id);
        dto.setIdArtista(5L);
        dto.setIdTipoOcupacion(TipoOcupacionEnum.OCUPADO.getId());
        dto.setFecha(LocalDateTime.of(2025, 8, 15, 22, 0));
        return dto;
    }

    private static OcupacionListRecord record(long id, long idArtista, String importe) {
        return new OcupacionListRecord(id, LocalDateTime.of(2025, 8, 15, 22, 0), idArtista, "Artista " + idArtista, importe, true,
                "Ocupado", "A Coruña", "Santiago", "Conxo", false, false, "Ocupado", 1L, "Usuario Prueba",
                null, null, LocalDateTime.of(2025, 6, 1, 10, 0), null, null, null, null);
    }

    private static CustomAuthenticatedUser authenticatedUser(Map<Long, Set<String>> permisosArtista, Map<Long, Set<String>> permisosAgencia) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Usuario");
        usuario.setApellidos("Prueba");
        usuario.setUsername("usuario.prueba");
        usuario.setPassword("secret");
        return new CustomAuthenticatedUser(usuario, true, true, true, true, List.of(), permisosArtista, permisosAgencia);
    }
}
