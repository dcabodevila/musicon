package es.musicalia.gestmusica.listado;

import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.ajustes.AjustesService;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.config.CustomPermissionEvaluator;
import es.musicalia.gestmusica.config.RateLimitingFilter;
import es.musicalia.gestmusica.config.WebSecurityConfig;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.permiso.PermisoService;
import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import es.musicalia.gestmusica.agencia.AgenciaRecord;
import es.musicalia.gestmusica.ajustes.AjustesDto;
import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import es.musicalia.gestmusica.localizacion.Ccaa;
import es.musicalia.gestmusica.localizacion.CodigoNombreDto;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.observabilidad.FunctionalEventNames;
import es.musicalia.gestmusica.observabilidad.FunctionalEventOutcome;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ListadoController.class)
@Import(WebSecurityConfig.class)
class ListadoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocalizacionService localizacionService;
    @MockitoBean
    private ListadoService listadoService;
    @MockitoBean
    private FunctionalEventTracker functionalEventTracker;
    @MockitoBean
    private ArtistaService artistaService;
    @MockitoBean
    private AgenciaService agenciaService;
    @MockitoBean
    private AjustesService ajustesService;
    @MockitoBean
    private PermisoService permisoService;
    @MockitoBean
    private ListadoChartDataFactory listadoChartDataFactory;

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

        when(userDetailsService.loadUserByUsername(any())).thenReturn(
                User.withUsername("test")
                        .password("{noop}password")
                        .authorities("USER")
                        .build()
        );
        when(mensajeService.obtenerMensajesRecibidos(any())).thenReturn(Collections.emptyList());
    }

    @Test
    void chartData_sinListados_devuelveFallbackSinDatosCero() throws Exception {
        when(listadoService.obtenerListadoEntreFechas(any())).thenReturn(List.of());
        when(listadoService.obtenerListadosPorPeriodo(any(), anyBoolean())).thenReturn(List.of());
        when(listadoChartDataFactory.from(any())).thenReturn(List.of(
                java.util.Map.of("mes", "Sin datos", "cantidad", 0L)
        ));

        mockMvc.perform(post("/listado/audiencias/chart-data")
                        .with(csrf())
                        .with(user("auth").roles("USER"))
                        .param("porDia", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.chartData[0].mes").value("Sin datos"))
                .andExpect(jsonPath("$.chartData[0].cantidad").value(0));
    }

    @Test
    void generarListado_sinRangoNiFechasSueltas_devuelveBadRequestSinInvocarServicio() throws Exception {
        mockMvc.perform(post("/listado/generar")
                        .with(csrf())
                        .with(user(authenticatedUser()))
                        .param("solicitadoPara", "Cliente")
                        .param("idCcaa", "1")
                        .param("idProvincia", "1")
                        .param("idTipoOcupacion", "1")
                        .param("idsAgencias", "3")
                        .param("idsTipoArtista", "1")
                        .param("idsComunidades", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Introduce fecha inicial y final, o al menos una fecha suelta."));

        verify(listadoService, never()).generarInformeListado(any(), any());
        verify(functionalEventTracker).track(FunctionalEventNames.LISTADO_GENERATED, FunctionalEventOutcome.FAILURE,
                1L, "usuario.prueba", Map.of("reason", "invalid_date_selection"));
    }

    @Test
    void generarListado_rangoIncompleto_devuelveBadRequestSinInvocarServicio() throws Exception {
        mockMvc.perform(generarValido().param("fechaDesde", "02-03-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Completa fecha inicial y fecha final, o borra el rango y usa fechas sueltas."));

        verify(listadoService, never()).generarInformeListado(any(), any());
    }

    @Test
    void generarListado_rangoYFechasSueltas_devuelveBadRequestSinInvocarServicio() throws Exception {
        mockMvc.perform(generarValido()
                        .param("fechaDesde", "02-03-2026")
                        .param("fechaHasta", "08-03-2026")
                        .param("fecha1", "10-03-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No mezcles rango de fechas con fechas sueltas. Borra uno de los dos bloques."));

        verify(listadoService, never()).generarInformeListado(any(), any());
    }

    @ParameterizedTest(name = "sin {0}")
    @CsvSource(delimiter = '|', value = {
            "solicitadoPara|Indica para quién es el listado.",
            "idCcaa|Selecciona una comunidad autónoma.",
            "idProvincia|Selecciona una provincia.",
            "idTipoOcupacion|Selecciona un tipo de ocupación.",
            "idsAgencias|Selecciona al menos una agencia.",
            "idsTipoArtista|Selecciona al menos un tipo de artista.",
            "idsComunidades|Selecciona al menos una comunidad del artista."
    })
    void generarListado_sinCampoObligatorio_devuelveMensajeDeValidacionSinInvocarServicio(String campo, String mensaje) throws Exception {
        MockHttpServletRequestBuilder peticion = generarValido().param("fecha1", "10-03-2026");
        peticion.with(request -> {
            request.removeParameter(campo);
            return request;
        });

        mockMvc.perform(peticion)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(mensaje));

        verify(listadoService, never()).generarInformeListado(any(), any());
        verify(functionalEventTracker).track(FunctionalEventNames.LISTADO_GENERATED, FunctionalEventOutcome.FAILURE,
                1L, "usuario.prueba", Map.of("reason", "validation"));
    }

    @Test
    void generarListado_peticionValida_devuelvePdfAdjuntoYRegistraExito() throws Exception {
        byte[] pdf = {37, 80, 68, 70};
        when(listadoService.generarInformeListado(any(), any())).thenReturn(pdf);

        mockMvc.perform(generarValido()
                        .param("fechaDesde", "02-03-2026")
                        .param("fechaHasta", "08-03-2026")
                        .param("idMunicipio", "150")
                        .param("localidad", "Plaza"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("form-data; name=\"attachment\"")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString(".pdf")))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, "4"))
                .andExpect(header().string("Content-Transfer-Encoding", "binary"))
                .andExpect(content().bytes(pdf));

        ArgumentCaptor<ListadoDto> dto = ArgumentCaptor.forClass(ListadoDto.class);
        verify(listadoService).generarInformeListado(dto.capture(), eq(1L));
        assertThat(dto.getValue().getSolicitadoPara()).isEqualTo("Cliente");
        assertThat(dto.getValue().getIdTipoOcupacion()).isEqualTo(2L);
        assertThat(dto.getValue().getFechaDesde()).isEqualTo(LocalDate.of(2026, 3, 2));
        assertThat(dto.getValue().getFechaHasta()).isEqualTo(LocalDate.of(2026, 3, 8));
        assertThat(dto.getValue().getIdMunicipio()).isEqualTo(150L);
        assertThat(dto.getValue().getIdsAgencias()).containsExactly(3L);
        assertThat(dto.getValue().getIdsTipoArtista()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(dto.getValue().getIdsComunidades()).containsExactly(11L);
        verify(functionalEventTracker).track(FunctionalEventNames.LISTADO_GENERATED, FunctionalEventOutcome.SUCCESS,
                1L, "usuario.prueba", Map.of("tipo_ocupacion", "2"));
    }

    @Test
    void generarListado_servicioRechazaPeticion_devuelveBadRequestConMensajeDelServicio() throws Exception {
        when(listadoService.generarInformeListado(any(), any()))
                .thenThrow(new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio."));

        mockMvc.perform(generarValido().param("fechaDesde", "08-03-2026").param("fechaHasta", "02-03-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha de fin no puede ser anterior a la fecha de inicio."));

        verify(functionalEventTracker).track(FunctionalEventNames.LISTADO_GENERATED, FunctionalEventOutcome.FAILURE,
                1L, "usuario.prueba", Map.of("reason", "validation"));
    }

    @Test
    void generarListado_errorInesperado_devuelveErrorInternoConMensaje() throws Exception {
        when(listadoService.generarInformeListado(any(), any())).thenThrow(new IllegalStateException("Jasper caído"));

        mockMvc.perform(generarValido().param("fecha1", "10-03-2026"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Error al generar el listado: Jasper caído"));

        verify(functionalEventTracker).track(FunctionalEventNames.LISTADO_GENERATED, FunctionalEventOutcome.FAILURE,
                1L, "usuario.prueba", Map.of("reason", "unexpected_error"));
    }

    @Test
    void generarListado_sinCsrf_devuelveForbiddenSinInvocarServicio() throws Exception {
        mockMvc.perform(post("/listado/generar")
                        .with(user(authenticatedUser()))
                        .param("fecha1", "10-03-2026"))
                .andExpect(status().isForbidden());

        verify(listadoService, never()).generarInformeListado(any(), any());
    }

    @Test
    void listado_sinSesion_redirigeAlLogin() throws Exception {
        mockMvc.perform(get("/listado"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));

        verifyNoInteractions(listadoService, localizacionService);
    }

    @Test
    void listado_conAjustesPredeterminados_preparaFormularioConFiltrosDelUsuario() throws Exception {
        Ccaa ccaa = new Ccaa();
        ccaa.setId(12L);
        Provincia provincia = new Provincia();
        provincia.setId(15L);
        AjustesDto ajustes = new AjustesDto();
        ajustes.setIdsTipoArtista(List.of(1L, 2L));
        ajustes.setIdsAgencias(List.of(3L));
        ajustes.setIdsComunidades(List.of(11L));
        List<CodigoNombreRecord> comunidades = List.of(new CodigoNombreRecord(12L, "Galicia"));
        List<CodigoNombreDto> tiposOcupacion = List.of(new CodigoNombreDto(1L, "Con ocupación"));
        when(localizacionService.findCcaaByUsuarioId(1L)).thenReturn(ccaa);
        when(localizacionService.findProvinciaByUsuarioId(1L)).thenReturn(provincia);
        when(localizacionService.findAllComunidades()).thenReturn(comunidades);
        when(listadoService.findAllTiposOcupacion()).thenReturn(tiposOcupacion);
        when(ajustesService.getAjustesByIdUsuario(1L)).thenReturn(ajustes);
        when(ajustesService.getAllAjustesByIdUsuario(1L)).thenReturn(List.of(ajustes));

        CustomAuthenticatedUser usuario = authenticatedUser();
        usuario.getUsuario().setNombreComercial("Orquesta Prueba");

        mockMvc.perform(get("/listado").with(user(usuario)))
                .andExpect(status().isOk())
                .andExpect(view().name("listados"))
                .andExpect(model().attribute("listadoDto", allOf(
                        hasProperty("solicitadoPara", is("Orquesta Prueba")),
                        hasProperty("idCcaa", is(12L)),
                        hasProperty("idProvincia", is(15L)),
                        hasProperty("idTipoOcupacion", is(1L)),
                        hasProperty("idsTipoArtista", is(Set.of(1L, 2L))),
                        hasProperty("idsAgencias", is(Set.of(3L))),
                        hasProperty("idsComunidades", is(Set.of(11L))))))
                .andExpect(model().attribute("listaCcaa", comunidades))
                .andExpect(model().attribute("listaTiposOcupacion", tiposOcupacion))
                .andExpect(model().attribute("listaAjustes", List.of(ajustes)));

        verify(localizacionService).findAllProvinciasByCcaaId(12L);
        verify(localizacionService).findAllMunicipiosByIdProvincia(15L);
    }

    @Test
    void listado_sinAjustesNiNombreComercial_usaNombreCompletoYFiltrosVacios() throws Exception {
        Ccaa ccaa = new Ccaa();
        ccaa.setId(12L);
        Provincia provincia = new Provincia();
        provincia.setId(15L);
        when(localizacionService.findCcaaByUsuarioId(1L)).thenReturn(ccaa);
        when(localizacionService.findProvinciaByUsuarioId(1L)).thenReturn(provincia);

        mockMvc.perform(get("/listado").with(user(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(view().name("listados"))
                .andExpect(model().attribute("listadoDto", allOf(
                        hasProperty("solicitadoPara", is("Usuario Prueba")),
                        hasProperty("idsTipoArtista", is(Set.of())),
                        hasProperty("idsAgencias", is(Set.of())),
                        hasProperty("idsComunidades", is(Set.of())))));
    }

    @Test
    void audienciaListados_cargaUltimosDosMesesPorMesYSoloAgenciasConPermiso() throws Exception {
        List<ListadoRecord> listados = List.of(listadoRecord("Lugo"));
        List<ListadosPorMesDto> porMes = List.of(new ListadosPorMesDto("Enero 2026", 1L));
        when(listadoService.obtenerListadoEntreFechas(any())).thenReturn(listados);
        when(listadoService.obtenerListadosPorPeriodo(listados, false)).thenReturn(porMes);
        when(listadoChartDataFactory.from(porMes)).thenReturn(List.of(chartPoint("Enero 2026", 1L)));
        List<AgenciaRecord> misAgencias = misAgenciasConPermiso();

        mockMvc.perform(get("/listado/audiencia-listados").with(user(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(view().name("listados-generados"))
                .andExpect(model().attribute("chartData", "[{\"mes\":\"Enero 2026\",\"cantidad\":1}]"))
                .andExpect(model().attribute("listadosGenerados", List.of()))
                .andExpect(model().attribute("listaAgencias", misAgencias));

        ArgumentCaptor<ListadoAudienciasDto> filtros = ArgumentCaptor.forClass(ListadoAudienciasDto.class);
        verify(listadoService).obtenerListadoEntreFechas(filtros.capture());
        assertThat(filtros.getValue().getPorDia()).isFalse();
        assertThat(filtros.getValue().getIdAgencia()).isNull();
        assertThat(filtros.getValue().getFechaDesde())
                .isEqualTo(filtros.getValue().getFechaHasta().minusMonths(2).withDayOfMonth(1));
        verify(agenciaService).findMisAgencias(Set.of(4L));
    }

    @Test
    void audiencias_fechasValidas_devuelveGraficoYAgenciasConPermiso() throws Exception {
        List<ListadoRecord> listados = List.of(listadoRecord("Lugo"));
        List<ListadosPorMesDto> porDia = List.of(new ListadosPorMesDto("01/03/2026", 1L));
        when(listadoService.obtenerListadoEntreFechas(any())).thenReturn(listados);
        when(listadoService.obtenerListadosPorPeriodo(listados, true)).thenReturn(porDia);
        when(listadoChartDataFactory.from(porDia)).thenReturn(List.of(chartPoint("01/03/2026", 1L)));
        List<AgenciaRecord> misAgencias = misAgenciasConPermiso();

        mockMvc.perform(post("/listado/audiencias").with(csrf()).with(user(authenticatedUser()))
                        .param("idAgencia", "4")
                        .param("fechaDesde", "01-03-2026")
                        .param("fechaHasta", "31-03-2026")
                        .param("porDia", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("listados-generados"))
                .andExpect(model().attributeDoesNotExist("message"))
                .andExpect(model().attribute("chartData", "[{\"mes\":\"01/03/2026\",\"cantidad\":1}]"))
                .andExpect(model().attribute("listaAgencias", misAgencias));

        verify(listadoService).obtenerListadoEntreFechas(
                new ListadoAudienciasDto(4L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), true));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "sin fechas|||Las fechas inicial y final son obligatorias",
            "sin fecha final|01-03-2026||Las fechas inicial y final son obligatorias",
            "inicial posterior|10-03-2026|01-03-2026|La fecha inicial no puede ser posterior a la fecha final",
            "formato invalido|no-es-fecha|01-03-2026|Error en los datos del formulario"
    })
    void audiencias_fechasInvalidas_muestraErrorSinConsultarListados(String caso, String desde, String hasta, String mensaje) throws Exception {
        List<AgenciaRecord> misAgencias = misAgenciasConPermiso();

        mockMvc.perform(post("/listado/audiencias").with(csrf()).with(user(authenticatedUser()))
                        .param("fechaDesde", desde == null ? "" : desde)
                        .param("fechaHasta", hasta == null ? "" : hasta))
                .andExpect(status().isOk())
                .andExpect(view().name("listados-generados"))
                .andExpect(model().attribute("message", mensaje))
                .andExpect(model().attribute("alertClass", "danger"))
                .andExpect(model().attribute("listadosGenerados", List.of()))
                .andExpect(model().attribute("listaAgencias", misAgencias))
                .andExpect(model().attributeDoesNotExist("chartData"));

        verify(listadoService, never()).obtenerListadoEntreFechas(any());
    }

    @Test
    void audiencias_errorDelServicio_muestraErrorGenerico() throws Exception {
        when(listadoService.obtenerListadoEntreFechas(any())).thenThrow(new IllegalStateException("BD caída"));
        misAgenciasConPermiso();

        mockMvc.perform(post("/listado/audiencias").with(csrf()).with(user(authenticatedUser()))
                        .param("fechaDesde", "01-03-2026")
                        .param("fechaHasta", "31-03-2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("listados-generados"))
                .andExpect(model().attribute("message", "Error al procesar la solicitud"))
                .andExpect(model().attribute("alertClass", "danger"))
                .andExpect(model().attributeDoesNotExist("chartData"));
    }

    @Test
    void audienciasData_paginaYFiltraConOrdenFechaCreacionDesc() throws Exception {
        ListadoRecord listado = listadoRecord("Lugo");
        when(listadoService.obtenerListadoEntreFechasPaginado(any(), any(), any(), any()))
                .thenAnswer(invocation -> new PageImpl<>(Collections.nCopies(5, listado), invocation.getArgument(2), 25));

        mockMvc.perform(post("/listado/audiencias/data").with(csrf()).with(user(authenticatedUser()))
                        .param("draw", "3")
                        .param("start", "20")
                        .param("length", "10")
                        .param("idAgencia", "4")
                        .param("fechaDesde", "01-03-2026")
                        .param("fechaHasta", "31-03-2026")
                        .param("search[value]", "Betanzos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draw").value(3))
                .andExpect(jsonPath("$.recordsTotal").value(25))
                .andExpect(jsonPath("$.recordsFiltered").value(25))
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.data[0].solicitadoPara").value("Concello"))
                .andExpect(jsonPath("$.data[0].provincia").value("Lugo"))
                .andExpect(jsonPath("$.error").doesNotExist());

        ArgumentCaptor<ListadoAudienciasDto> filtros = ArgumentCaptor.forClass(ListadoAudienciasDto.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(listadoService).obtenerListadoEntreFechasPaginado(filtros.capture(), eq("Betanzos"), pageable.capture(), eq(1L));
        assertThat(filtros.getValue().getIdAgencia()).isEqualTo(4L);
        assertThat(filtros.getValue().getFechaDesde()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(filtros.getValue().getFechaHasta()).isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "fechaCreacion"));
    }

    @Test
    void audienciasData_fechaConFormatoInvalido_devuelveRespuestaVaciaConError() throws Exception {
        mockMvc.perform(post("/listado/audiencias/data").with(csrf()).with(user(authenticatedUser()))
                        .param("draw", "5")
                        .param("fechaDesde", "2026-03-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draw").value(5))
                .andExpect(jsonPath("$.recordsTotal").value(0))
                .andExpect(jsonPath("$.recordsFiltered").value(0))
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.error", startsWith("Error al cargar los datos: ")));

        verify(listadoService, never()).obtenerListadoEntreFechasPaginado(any(), any(), any(), any());
    }

    @Test
    void chartData_errorDelServicio_devuelveSuccessFalseYGraficoVacio() throws Exception {
        when(listadoService.obtenerListadoEntreFechas(any())).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(post("/listado/audiencias/chart-data").with(csrf()).with(user(authenticatedUser()))
                        .param("fechaDesde", "01-03-2026")
                        .param("fechaHasta", "31-03-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Error al obtener datos del gráfico: boom"))
                .andExpect(jsonPath("$.chartData", hasSize(0)));
    }

    @Test
    void mapData_cuentaListadosPorProvinciaIgnorandoProvinciasVacias() throws Exception {
        when(listadoService.obtenerListadoEntreFechas(any())).thenReturn(List.of(
                listadoRecord("A Coruña"), listadoRecord("Lugo"), listadoRecord("A Coruña"),
                listadoRecord(" "), listadoRecord(null)));

        mockMvc.perform(post("/listado/audiencias/map-data").with(csrf()).with(user(authenticatedUser()))
                        .param("idAgencia", "4")
                        .param("fechaDesde", "01-03-2026")
                        .param("fechaHasta", "31-03-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.mapData", hasSize(2)))
                .andExpect(jsonPath("$.mapData[?(@.provincia == 'A Coruña')].cantidad").value(contains(2)))
                .andExpect(jsonPath("$.mapData[?(@.provincia == 'Lugo')].cantidad").value(contains(1)));

        verify(listadoService).obtenerListadoEntreFechas(
                new ListadoAudienciasDto(4L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null));
    }

    @Test
    void mapData_errorDelServicio_devuelveSuccessFalseYMapaVacio() throws Exception {
        when(listadoService.obtenerListadoEntreFechas(any())).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(post("/listado/audiencias/map-data").with(csrf()).with(user(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Error al obtener datos del mapa: boom"))
                .andExpect(jsonPath("$.mapData", hasSize(0)));
    }

    @Test
    void aggregatedData_consultaUnaVezYDevuelveGraficoYMapa() throws Exception {
        List<ListadoRecord> listados = List.of(listadoRecord("Lugo"), listadoRecord("Lugo"));
        List<ListadosPorMesDto> porDia = List.of(new ListadosPorMesDto("01/03/2026", 2L));
        when(listadoService.obtenerListadoEntreFechas(any())).thenReturn(listados);
        when(listadoService.obtenerListadosPorPeriodo(listados, true)).thenReturn(porDia);
        when(listadoChartDataFactory.from(porDia)).thenReturn(List.of(chartPoint("01/03/2026", 2L)));

        mockMvc.perform(post("/listado/audiencias/aggregated-data").with(csrf()).with(user(authenticatedUser()))
                        .param("fechaDesde", "01-03-2026")
                        .param("fechaHasta", "31-03-2026")
                        .param("porDia", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.chartData[0].mes").value("01/03/2026"))
                .andExpect(jsonPath("$.chartData[0].cantidad").value(2))
                .andExpect(jsonPath("$.mapData", hasSize(1)))
                .andExpect(jsonPath("$.mapData[0].provincia").value("Lugo"))
                .andExpect(jsonPath("$.mapData[0].cantidad").value(2));

        verify(listadoService).obtenerListadoEntreFechas(
                new ListadoAudienciasDto(null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), true));
    }

    @Test
    void aggregatedData_errorDelServicio_devuelveSuccessFalseYDatosVacios() throws Exception {
        when(listadoService.obtenerListadoEntreFechas(any())).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(post("/listado/audiencias/aggregated-data").with(csrf()).with(user(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Error al obtener datos: boom"))
                .andExpect(jsonPath("$.chartData", hasSize(0)))
                .andExpect(jsonPath("$.mapData", hasSize(0)));
    }

    private MockHttpServletRequestBuilder generarValido() {
        return post("/listado/generar")
                .with(csrf())
                .with(user(authenticatedUser()))
                .param("solicitadoPara", "Cliente")
                .param("idCcaa", "12")
                .param("idProvincia", "15")
                .param("idTipoOcupacion", "2")
                .param("idsAgencias", "3")
                .param("idsTipoArtista", "1", "2")
                .param("idsComunidades", "11");
    }

    private List<AgenciaRecord> misAgenciasConPermiso() {
        List<AgenciaRecord> misAgencias = List.of(new AgenciaRecord(4L, "Agencia Norte", null, null, null, null, null, null, null, null));
        when(permisoService.obtenerMapPermisosAgencia(1L)).thenReturn(Map.of(4L, Set.of("LISTADOS")));
        when(agenciaService.findMisAgencias(Set.of(4L))).thenReturn(misAgencias);
        return misAgencias;
    }

    private static Map<String, Object> chartPoint(String mes, long cantidad) {
        Map<String, Object> punto = new LinkedHashMap<>();
        punto.put("mes", mes);
        punto.put("cantidad", cantidad);
        return punto;
    }

    private static ListadoRecord listadoRecord(String provincia) {
        return new ListadoRecord(1L, "Concello", "Orquesta", null, null, null, provincia, null, null, null,
                LocalDateTime.of(2026, 3, 1, 10, 0), null, null, null, null, null, null, null, null, null);
    }

    private CustomAuthenticatedUser authenticatedUser() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Usuario");
        usuario.setApellidos("Prueba");
        usuario.setUsername("usuario.prueba");
        usuario.setPassword("{noop}password");

        return new CustomAuthenticatedUser(usuario, true, true, true, true, List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of(), Map.of());
    }
}
