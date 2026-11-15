package es.musicalia.gestmusica.home;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.passkey.PasskeyOffer;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import es.musicalia.gestmusica.config.CustomPermissionEvaluator;
import es.musicalia.gestmusica.config.RateLimitingFilter;
import es.musicalia.gestmusica.config.ThymeleafConfig;
import es.musicalia.gestmusica.config.WebSecurityConfig;
import es.musicalia.gestmusica.agencia.publicacioneventos.AgenciaPublicacionEventosService;
import es.musicalia.gestmusica.ocupacion.OcupacionRecord;
import es.musicalia.gestmusica.ocupacion.OcupacionService;
import es.musicalia.gestmusica.permiso.PermisoAgenciaEnum;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.usuario.Usuario;
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

import jakarta.servlet.FilterChain;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = HomeController.class)
@Import({WebSecurityConfig.class, ThymeleafConfig.class})
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OcupacionService ocupacionService;
    @MockitoBean
    private AgenciaPublicacionEventosService agenciaPublicacionEventosService;
    @MockitoBean
    private HomeEstadisticasService homeEstadisticasService;

    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private RateLimitingFilter rateLimitingFilter;
    @MockitoBean
    private CustomPermissionEvaluator customPermissionEvaluator;
    @MockitoBean
    private MensajeService mensajeService;
    @MockitoBean
    private FunctionalEventTracker functionalEventTracker;

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
        when(agenciaPublicacionEventosService.debeMostrarModal(any())).thenReturn(false);
    }

    @Test
    void home_adminNoDebeConsultarPendientesNiMostrarCarrusel() throws Exception {
        CustomAuthenticatedUser adminUser = authenticatedUser(
                Set.of(new SimpleGrantedAuthority("ACCESO_PANEL_ADMIN")),
                Map.of(1L, Set.of(PermisoAgenciaEnum.CONFIRMAR_OCUPACION.name()))
        );

        mockMvc.perform(get("/").with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("main.html"))
                .andExpect(model().attribute("isAdminHome", true))
                .andExpect(model().attribute("mostrarModalPublicacionEventosAgencia", false))
                .andExpect(model().attribute("listaOcupacionPendiente", List.of()))
                .andExpect(content().string(not(containsString("carrousel-lista-ocupaciones-pendientes"))));

        verifyNoInteractions(ocupacionService);
    }

    @Test
    void home_muestraContadoresDeLaPlataformaFueraDelHero() throws Exception {
        HomeEstadisticasDto estadisticas = new HomeEstadisticasDto(80L, 1234L, 410L, 96L);
        when(homeEstadisticasService.obtenerEstadisticas()).thenReturn(estadisticas);
        CustomAuthenticatedUser usuario = authenticatedUser(Set.of(new SimpleGrantedAuthority("USER")), Map.of());

        mockMvc.perform(get("/").with(user(usuario)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("estadisticasPlataforma", estadisticas))
                .andExpect(content().string(containsString("id=\"home-cifras\"")))
                .andExpect(content().string(matchesPattern("(?s).*>80<.*Artistas activos.*>1.234<.*Usuarios activos.*>410<.*Presupuestos generados.*>96<.*Ocupaciones creadas.*")))
                .andExpect(content().string(containsString("Últimos 30 días")))
                .andExpect(content().string(not(containsString("Agencias activas"))))
                .andExpect(content().string(not(containsString("home-hero__stats"))));
    }

    @Test
    void home_trasLoginConContrasena_debeOfrecerPasskeyUnaVez() throws Exception {
        CustomAuthenticatedUser usuario = authenticatedUser(Set.of(new SimpleGrantedAuthority("USER")), Map.of());
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(PasskeyOffer.SESSION_ATTRIBUTE, true);

        mockMvc.perform(get("/").session(session).accept(MediaType.TEXT_HTML).with(user(usuario)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"passkey-offer\"")))
                .andExpect(content().string(containsString("¿Quieres entrar con huella o Face ID la próxima vez?")))
                .andExpect(content().string(matchesPattern("(?s).*src=\"/js/passkey-offer(-[0-9a-f]+)?[.]js\".*")));

        mockMvc.perform(get("/").session(session).accept(MediaType.TEXT_HTML).with(user(usuario)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("id=\"passkey-offer\""))));
    }

    @Test
    void home_agenciaConFechasPendientesMuestraBannerDeAvisoFueraDelHero() throws Exception {
        CustomAuthenticatedUser agencia = authenticatedUser(
                Set.of(new SimpleGrantedAuthority("USER")),
                Map.of(7L, Set.of(PermisoAgenciaEnum.CONFIRMAR_OCUPACION.name()))
        );
        OcupacionRecord pendiente = new OcupacionRecord(10L, LocalDateTime.now(), 1L, "Artista", "100", false, "BOLO", "Pontevedra", "Vigo", "Centro", false, false, "PENDIENTE", 2L, "Usuario");
        when(ocupacionService.findOcupacionesDtoByAgenciaPendientes(Set.of(7L))).thenReturn(List.of(pendiente, pendiente));

        mockMvc.perform(get("/").with(user(agencia)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"home-aviso-pendientes\"")))
                .andExpect(content().string(containsString("Tienes 2 fechas pendientes de confirmar")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"home-aviso-pendientes\".*href=\"#ocupaciones-pendientes\"[^>]*>\s*Revisar fechas.*")))
                .andExpect(content().string(not(containsString("home-hero__notice-cta"))));
    }

    @Test
    void home_sinFechasPendientesNoMuestraBannerDeAviso() throws Exception {
        CustomAuthenticatedUser agencia = authenticatedUser(
                Set.of(new SimpleGrantedAuthority("USER")),
                Map.of(7L, Set.of(PermisoAgenciaEnum.CONFIRMAR_OCUPACION.name()))
        );
        when(ocupacionService.findOcupacionesDtoByAgenciaPendientes(Set.of(7L))).thenReturn(List.of());

        mockMvc.perform(get("/").with(user(agencia)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("home-aviso-pendientes"))));
    }

    @Test
    void home_noAdminConPermisoConfirmarMantieneFlujoActual() throws Exception {
        CustomAuthenticatedUser nonAdminUser = authenticatedUser(
                Set.of(new SimpleGrantedAuthority("USER")),
                Map.of(7L, Set.of(PermisoAgenciaEnum.CONFIRMAR_OCUPACION.name()))
        );

        List<OcupacionRecord> pendientes = List.of(
                new OcupacionRecord(10L, LocalDateTime.now(), 1L, "Artista", "100", false, "BOLO", "Pontevedra", "Vigo", "Centro", false, false, "PENDIENTE", 2L, "Usuario")
        );
        when(ocupacionService.findOcupacionesDtoByAgenciaPendientes(Set.of(7L))).thenReturn(pendientes);

        mockMvc.perform(get("/").with(user(nonAdminUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("main.html"))
                .andExpect(model().attribute("isAdminHome", false))
                .andExpect(model().attribute("mostrarModalPublicacionEventosAgencia", false))
                .andExpect(model().attribute("listaOcupacionPendiente", pendientes))
                .andExpect(content().string(containsString("carrousel-lista-ocupaciones-pendientes")));

        verify(ocupacionService).findOcupacionesDtoByAgenciaPendientes(Set.of(7L));
    }

    @Test
    void home_noAdminRenderizaPendienteEnGrisYMantieneBadgeReservado() throws Exception {
        CustomAuthenticatedUser nonAdminUser = authenticatedUser(
                Set.of(new SimpleGrantedAuthority("USER")),
                Map.of(7L, Set.of(PermisoAgenciaEnum.CONFIRMAR_OCUPACION.name()))
        );

        List<OcupacionRecord> pendientes = List.of(
                ocupacion(11L, "PENDIENTE"),
                ocupacion(12L, "Reservado"),
                ocupacion(13L, "SIN_ESTADO")
        );
        when(ocupacionService.findOcupacionesDtoByAgenciaPendientes(Set.of(7L))).thenReturn(pendientes);

        mockMvc.perform(get("/").with(user(nonAdminUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("main.html"))
                .andExpect(model().attribute("mostrarModalPublicacionEventosAgencia", false))
                .andExpect(content().string(containsString("ocp-card--pendiente")))
                .andExpect(content().string(containsString(">Pendiente<")))
                .andExpect(content().string(containsString(">Reservado<")))
                .andExpect(content().string(not(containsString(">SIN_ESTADO<"))));

        verify(ocupacionService).findOcupacionesDtoByAgenciaPendientes(Set.of(7L));
    }

    @Test
    void home_muestraModalPublicacionEventosCuandoCorresponde() throws Exception {
        CustomAuthenticatedUser nonAdminUser = authenticatedUser(
                Set.of(new SimpleGrantedAuthority("USER")),
                Map.of()
        );
        when(agenciaPublicacionEventosService.debeMostrarModal(99L)).thenReturn(true);

        mockMvc.perform(get("/").with(user(nonAdminUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("main.html"))
                .andExpect(model().attribute("mostrarModalPublicacionEventosAgencia", true));
    }

    @Test
    void home_noMuestraModalPublicacionEventosSiServicioIndicaFalse() throws Exception {
        CustomAuthenticatedUser nonAdminUser = authenticatedUser(
                Set.of(new SimpleGrantedAuthority("USER")),
                Map.of()
        );
        when(agenciaPublicacionEventosService.debeMostrarModal(99L)).thenReturn(false);

        mockMvc.perform(get("/").with(user(nonAdminUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("main.html"))
                .andExpect(model().attribute("mostrarModalPublicacionEventosAgencia", false));
    }

    private CustomAuthenticatedUser authenticatedUser(Set<SimpleGrantedAuthority> authorities, Map<Long, Set<String>> mapPermisosAgencia) {
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setNombre("Test");
        usuario.setApellidos("User");
        usuario.setPassword("secret");
        usuario.setValidado(true);

        return new CustomAuthenticatedUser(
                usuario,
                true,
                true,
                true,
                true,
                authorities,
                Map.of(),
                mapPermisosAgencia
        );
    }

    private OcupacionRecord ocupacion(Long id, String estado) {
        return new OcupacionRecord(
                id,
                LocalDateTime.now(),
                1L,
                "Artista " + id,
                "100",
                false,
                "BOLO",
                "Pontevedra",
                "Vigo",
                "Centro",
                false,
                false,
                estado,
                2L,
                "Usuario"
        );
    }
}
