package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.config.CustomPermissionEvaluator;
import es.musicalia.gestmusica.config.RateLimitingFilter;
import es.musicalia.gestmusica.config.WebSecurityConfig;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.permiso.PermisoService;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract of /ocupacion/** through the real security chain: authentication required,
 * CSRF on POST, JSON binding of the save DTO, agency access checks and export responses.
 */
@WebMvcTest(controllers = OcupacionController.class)
@Import(WebSecurityConfig.class)
class OcupacionControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OcupacionService ocupacionService;
    @MockitoBean
    private AgenciaService agenciaService;
    @MockitoBean
    private ArtistaService artistaService;
    @MockitoBean
    private LocalizacionService localizacionService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private FunctionalEventTracker functionalEventTracker;

    @MockitoBean
    private PermisoService permisoService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private RateLimitingFilter rateLimitingFilter;
    @MockitoBean
    private CustomPermissionEvaluator customPermissionEvaluator;
    @MockitoBean
    private MensajeService mensajeService;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());
        when(mensajeService.obtenerMensajesRecibidos(any())).thenReturn(Collections.emptyList());
    }

    @Test
    void anonymousRequests_redirectToLoginWithoutTouchingTheService() throws Exception {
        mockMvc.perform(get("/ocupacion/list"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
        mockMvc.perform(get("/ocupacion/anular/5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
        mockMvc.perform(get("/ocupacion/confirmar/5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));

        verifyNoInteractions(ocupacionService, functionalEventTracker);
    }

    @Test
    void save_withoutCsrf_isForbidden() throws Exception {
        mockMvc.perform(post("/ocupacion/save")
                        .with(user(authenticatedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idArtista\":5,\"idTipoOcupacion\":1}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(ocupacionService);
    }

    @Test
    void save_bindsJsonBodyAndReturnsServiceResponse() throws Exception {
        when(ocupacionService.existeOcupacionFecha(any())).thenReturn(false);
        when(ocupacionService.saveOcupacion(any())).thenReturn(DefaultResponseBody.builder()
                .success(true).message("Ocupacion guardada correctamente").messageType("success").idEntidad(77L).build());

        mockMvc.perform(post("/ocupacion/save")
                        .with(csrf())
                        .with(user(authenticatedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idArtista":5,"idTipoOcupacion":1,"idCcaa":12,"idProvincia":15,"idMunicipio":150,
                                 "localidad":"Conxo","importe":1500.50,"porcentajeRepre":10,"iva":21,
                                 "fecha":"2025-08-15T22:00:00","matinal":true,"soloMatinal":false,"provisional":false,
                                 "horaActuacionDesde":"21:30","eventoVisible":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ocupacion guardada correctamente"))
                .andExpect(jsonPath("$.messageType").value("success"))
                .andExpect(jsonPath("$.idEntidad").value(77));

        ArgumentCaptor<OcupacionSaveDto> captor = ArgumentCaptor.forClass(OcupacionSaveDto.class);
        verify(ocupacionService).saveOcupacion(captor.capture());
        OcupacionSaveDto dto = captor.getValue();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getIdArtista()).isEqualTo(5L);
        assertThat(dto.getIdMunicipio()).isEqualTo(150L);
        assertThat(dto.getLocalidad()).isEqualTo("Conxo");
        assertThat(dto.getImporte()).isEqualByComparingTo(new BigDecimal("1500.50"));
        assertThat(dto.getFecha()).isEqualTo(LocalDateTime.of(2025, 8, 15, 22, 0));
        assertThat(dto.getMatinal()).isTrue();
        assertThat(dto.getHoraActuacionDesde()).hasToString("21:30");
        assertThat(dto.getEventoVisible()).isFalse();
    }

    @Test
    void save_duplicateDate_returnsErrorJsonAndDoesNotSave() throws Exception {
        when(ocupacionService.existeOcupacionFecha(any())).thenReturn(true);

        mockMvc.perform(post("/ocupacion/save")
                        .with(csrf())
                        .with(user(authenticatedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idArtista\":5,\"idTipoOcupacion\":1,\"fecha\":\"2025-08-15T22:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Ya existe una ocupación en esa fecha"))
                .andExpect(jsonPath("$.messageType").value("error"));

        verify(ocupacionService, never()).saveOcupacion(any());
    }

    @Test
    void confirmar_returnsServiceJson() throws Exception {
        when(ocupacionService.confirmarOcupacion(9L)).thenReturn(DefaultResponseBody.builder()
                .success(true).message("Ocupacion confirmada correctamente").messageType("success").build());

        mockMvc.perform(get("/ocupacion/confirmar/9").with(user(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ocupacion confirmada correctamente"));

        verify(ocupacionService).confirmarOcupacion(9L);
    }

    @Test
    void listData_agencyWithoutAccess_isForbiddenAndServiceNotCalled() throws Exception {
        mockMvc.perform(post("/ocupacion/list/data")
                        .with(csrf())
                        .with(user(authenticatedUser()))
                        .param("idAgencia", "99"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(ocupacionService);
    }

    @Test
    void listData_returnsDataTablesJson() throws Exception {
        OcupacionListRecord record = new OcupacionListRecord(1L, LocalDateTime.of(2025, 8, 15, 22, 0), 5L, "Orquesta", "1500", true,
                "Ocupado", "A Coruña", "Santiago", "Conxo", false, false, "Ocupado", 1L, "Usuario Prueba",
                null, null, LocalDateTime.of(2025, 6, 1, 10, 0), null, null, null, null);
        when(ocupacionService.findOcupacionesByArtistasListAndDatesActivoPaginado(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(record)));

        mockMvc.perform(post("/ocupacion/list/data")
                        .with(csrf())
                        .with(user(authenticatedUser()))
                        .param("draw", "2")
                        .param("idAgencia", "10")
                        .param("fechaDesde", "01-08-2025")
                        .param("mostrarImportes", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draw").value(2))
                .andExpect(jsonPath("$.recordsTotal").value(1))
                .andExpect(jsonPath("$.recordsFiltered").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].start").value("15/08/2025"))
                .andExpect(jsonPath("$.data[0].importe").value("1500"));

        ArgumentCaptor<OcupacionListFilterDto> filtro = ArgumentCaptor.forClass(OcupacionListFilterDto.class);
        verify(ocupacionService).findOcupacionesByArtistasListAndDatesActivoPaginado(any(), filtro.capture(), eq(null),
                eq(org.springframework.data.domain.PageRequest.of(0, 10,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "fechaCreacion"))));
        assertThat(filtro.getValue().getIdAgencia()).isEqualTo(10L);
        assertThat(filtro.getValue().getFechaDesde()).isEqualTo(LocalDate.of(2025, 8, 1));
    }

    @Test
    void postList_redirectsWithFlashFilterBoundFromForm() throws Exception {
        mockMvc.perform(post("/ocupacion/list")
                        .with(csrf())
                        .with(user(authenticatedUser()))
                        .param("idAgencia", "10")
                        .param("fechaDesde", "01-08-2025")
                        .param("fechaHasta", "31-08-2025"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ocupacion/list"))
                .andExpect(flash().attribute("ocupacionListFilterDto",
                        OcupacionListFilterDto.builder().idAgencia(10L)
                                .fechaDesde(LocalDate.of(2025, 8, 1)).fechaHasta(LocalDate.of(2025, 8, 31)).build()));
    }

    @Test
    void exportExcel_returnsAttachment() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.writeBytes(new byte[]{1, 2, 3});
        when(ocupacionService.exportOcupacionesToExcel(any(), any())).thenReturn(stream);

        mockMvc.perform(post("/ocupacion/ocupaciones-excel")
                        .with(csrf())
                        .with(user(authenticatedUser()))
                        .param("idAgencia", "10")
                        .param("fechaDesde", "01-08-2025"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string("Content-Disposition", matchesPattern("form-data; name=\"attachment\"; filename=\"Ocupaciones_\\d{14}\\.xlsx\"")))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));

        ArgumentCaptor<OcupacionListFilterDto> filtro = ArgumentCaptor.forClass(OcupacionListFilterDto.class);
        verify(ocupacionService).exportOcupacionesToExcel(any(), filtro.capture());
        assertThat(filtro.getValue().getIdAgencia()).isEqualTo(10L);
        assertThat(filtro.getValue().getFechaDesde()).isEqualTo(LocalDate.of(2025, 8, 1));
    }

    @Test
    void exportPdf_agencyWithoutAccess_isForbidden() throws Exception {
        mockMvc.perform(post("/ocupacion/ocupaciones-pdf")
                        .with(csrf())
                        .with(user(authenticatedUser()))
                        .param("idAgencia", "99"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(ocupacionService);
    }

    private CustomAuthenticatedUser authenticatedUser() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Usuario");
        usuario.setApellidos("Prueba");
        usuario.setUsername("usuario.prueba");
        usuario.setPassword("{noop}password");

        return new CustomAuthenticatedUser(usuario, true, true, true, true, List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(5L, Set.of("OCUPACIONES", "VER_DATOS_ECONOMICOS")), Map.of(10L, Set.of("VER_OCUPACIONES")));
    }
}
