package es.musicalia.gestmusica.tarifa;

import es.musicalia.gestmusica.artista.ArtistaDto;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.config.CustomPermissionEvaluator;
import es.musicalia.gestmusica.config.RateLimitingFilter;
import es.musicalia.gestmusica.config.WebSecurityConfig;
import es.musicalia.gestmusica.listado.TipoTarifaEnum;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TarifaController.class)
@Import(WebSecurityConfig.class)
class TarifaControllerMvcTest {

    private static final String SAVE_BODY = """
            {"id":null,"idArtista":7,"fechaDesde":"2026-07-01T00:00:00","fechaHasta":"2026-07-03T00:00:00","importe":1500.50,"activo":true}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TarifaService tarifaService;
    @MockitoBean
    private ArtistaService artistaService;

    @MockitoBean
    private FunctionalEventTracker functionalEventTracker;
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
    void save_validBody_savesTarifaAndReturnsSuccessJson() throws Exception {
        mockMvc.perform(post("/tarifa/save")
                        .with(csrf())
                        .with(user("gestor").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAVE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tarifa guardada"))
                .andExpect(jsonPath("$.messageType").value("success"));

        ArgumentCaptor<TarifaSaveDto> captor = ArgumentCaptor.forClass(TarifaSaveDto.class);
        verify(tarifaService).saveTarifa(captor.capture());
        TarifaSaveDto dto = captor.getValue();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getIdArtista()).isEqualTo(7L);
        assertThat(dto.getFechaDesde()).isEqualTo(LocalDateTime.of(2026, 7, 1, 0, 0));
        assertThat(dto.getFechaHasta()).isEqualTo(LocalDateTime.of(2026, 7, 3, 0, 0));
        assertThat(dto.getImporte()).isEqualByComparingTo("1500.50");
        assertThat(dto.getActivo()).isTrue();
    }

    @Test
    void save_missingImporte_returnsBadRequestWithoutSaving() throws Exception {
        mockMvc.perform(post("/tarifa/save")
                        .with(csrf())
                        .with(user("gestor").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idArtista":7,"fechaDesde":"2026-07-01T00:00:00","fechaHasta":"2026-07-03T00:00:00"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tarifaService);
    }

    @Test
    void save_missingFechaHasta_returnsBadRequestWithoutSaving() throws Exception {
        mockMvc.perform(post("/tarifa/save")
                        .with(csrf())
                        .with(user("gestor").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idArtista":7,"fechaDesde":"2026-07-01T00:00:00","importe":100}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tarifaService);
    }

    @Test
    void save_withoutSession_redirectsToLoginWithoutSaving() throws Exception {
        mockMvc.perform(post("/tarifa/save")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAVE_BODY))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));

        verifyNoInteractions(tarifaService);
    }

    @Test
    void save_withoutCsrfToken_isForbiddenWithoutSaving() throws Exception {
        mockMvc.perform(post("/tarifa/save")
                        .with(user("gestor").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAVE_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(tarifaService);
    }

    @Test
    void eliminar_forcesActivoFalseAndReturnsSuccessJson() throws Exception {
        mockMvc.perform(post("/tarifa/eliminar")
                        .with(csrf())
                        .with(user("gestor").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAVE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tarifa eliminada"))
                .andExpect(jsonPath("$.messageType").value("success"));

        ArgumentCaptor<TarifaSaveDto> captor = ArgumentCaptor.forClass(TarifaSaveDto.class);
        verify(tarifaService).saveTarifa(captor.capture());
        assertThat(captor.getValue().getActivo()).isFalse();
        assertThat(captor.getValue().getIdArtista()).isEqualTo(7L);
        assertThat(captor.getValue().getFechaDesde()).isEqualTo(LocalDateTime.of(2026, 7, 1, 0, 0));
    }

    @Test
    void eliminar_invalidBody_returnsBadRequestWithoutDeleting() throws Exception {
        mockMvc.perform(post("/tarifa/eliminar")
                        .with(csrf())
                        .with(user("gestor").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idArtista\":7}"))
                .andExpect(status().isBadRequest());

        verify(tarifaService, never()).saveTarifa(any());
    }

    @Test
    void findByArtistaAndFecha_parsesDayMonthYearAndReturnsTarifaJson() throws Exception {
        when(tarifaService.findByArtistaIdAndDate(7L, LocalDate.of(2026, 7, 1)))
                .thenReturn(new TarifaDto(55L, LocalDateTime.of(2026, 7, 1, 0, 0), 7L, "1500", true));

        mockMvc.perform(get("/tarifa/7/01-07-2026").with(user("gestor").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(55))
                .andExpect(jsonPath("$.idArtista").value(7))
                .andExpect(jsonPath("$.title").value("1500"))
                .andExpect(jsonPath("$.allDay").value(true))
                .andExpect(jsonPath("$.start").value("2026-07-01T00:00:00"));
    }

    @Test
    void findByArtistaAndFecha_withoutTarifa_returnsOkWithEmptyBody() throws Exception {
        mockMvc.perform(get("/tarifa/7/01-07-2026").with(user("gestor").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(tarifaService).findByArtistaIdAndDate(7L, LocalDate.of(2026, 7, 1));
    }

    @Test
    void findByArtistaAndFecha_isoDate_isAlsoAcceptedThroughSpringIsoFallback() throws Exception {
        mockMvc.perform(get("/tarifa/7/2026-07-01").with(user("gestor").roles("USER")))
                .andExpect(status().isOk());

        verify(tarifaService).findByArtistaIdAndDate(7L, LocalDate.of(2026, 7, 1));
    }

    @Test
    void findByArtistaAndFecha_unparseableDate_returnsBadRequestWithoutLookup() throws Exception {
        mockMvc.perform(get("/tarifa/7/31-13-2026").with(user("gestor").roles("USER")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tarifaService);
    }

    @Test
    void findByArtistaAndFecha_withoutSession_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/tarifa/7/01-07-2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));

        verifyNoInteractions(tarifaService);
    }

    @Test
    void listaByCcaa_returnsSimilarArtistasTarifas() throws Exception {
        when(tarifaService.findTarifasByFechaAndNumComponentesArtista(7L, LocalDate.of(2026, 7, 1)))
                .thenReturn(List.of(new TarifaArtistaCcaaDto(8L, "Orquesta Similar", new BigDecimal("2000"))));

        mockMvc.perform(get("/tarifa/lista/7/01-07-2026").with(user("gestor").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idArtista").value(8))
                .andExpect(jsonPath("$[0].nombreArtista").value("Orquesta Similar"))
                .andExpect(jsonPath("$[0].importe").value(2000));
    }

    @Test
    void tarifaAnualPdf_returnsAttachmentNamedAfterArtistaAndYear() throws Exception {
        byte[] pdf = "%PDF-fake".getBytes(StandardCharsets.UTF_8);
        when(tarifaService.getInformeTarifaAnual(any())).thenReturn(pdf);
        when(artistaService.findArtistaDtoById(7L)).thenReturn(artistaDto("Orquesta"));

        mockMvc.perform(post("/tarifa/tarifa-anual")
                        .with(csrf())
                        .with(user("gestor").roles("USER"))
                        .param("idArtista", "7")
                        .param("idProvincia", "15")
                        .param("ano", "2026")
                        .param("conOcupacion", "true")
                        .param("tipoTarifa", "ANUAL"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        matchesPattern("form-data; name=\"attachment\"; filename=\"Orquesta2026-\\d{14}\\.pdf\"")))
                .andExpect(content().bytes(pdf));

        ArgumentCaptor<TarifaAnualDto> captor = ArgumentCaptor.forClass(TarifaAnualDto.class);
        verify(tarifaService).getInformeTarifaAnual(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new TarifaAnualDto(7L, 15L, 2026, true, TipoTarifaEnum.ANUAL));
    }

    @Test
    void tarifaAnualExcel_returnsXlsxAttachmentNamedAfterArtistaAndYear() throws Exception {
        ByteArrayOutputStream excel = new ByteArrayOutputStream();
        excel.writeBytes(new byte[]{9, 8, 7});
        when(tarifaService.exportTarifaAnualToExcel(any())).thenReturn(excel);
        when(artistaService.findArtistaDtoById(7L)).thenReturn(artistaDto("Orquesta"));

        mockMvc.perform(post("/tarifa/tarifa-anual-excel")
                        .with(csrf())
                        .with(user("gestor").roles("USER"))
                        .param("idArtista", "7")
                        .param("idProvincia", "15")
                        .param("ano", "2026")
                        .param("conOcupacion", "false")
                        .param("tipoTarifa", "OCHO_MESES"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        matchesPattern("form-data; name=\"attachment\"; filename=\"Orquesta_2026-\\d{14}\\.xlsx\"")))
                .andExpect(content().bytes(new byte[]{9, 8, 7}));

        ArgumentCaptor<TarifaAnualDto> captor = ArgumentCaptor.forClass(TarifaAnualDto.class);
        verify(tarifaService).exportTarifaAnualToExcel(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new TarifaAnualDto(7L, 15L, 2026, false, TipoTarifaEnum.OCHO_MESES));
    }

    @Test
    void tarifaAnualPdf_withoutSession_redirectsToLoginWithoutGenerating() throws Exception {
        mockMvc.perform(post("/tarifa/tarifa-anual")
                        .with(csrf())
                        .param("idArtista", "7")
                        .param("ano", "2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));

        verifyNoInteractions(tarifaService, artistaService);
    }

    private static ArtistaDto artistaDto(String nombre) {
        ArtistaDto dto = new ArtistaDto();
        dto.setNombre(nombre);
        return dto;
    }
}
