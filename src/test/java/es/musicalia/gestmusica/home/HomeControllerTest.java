package es.musicalia.gestmusica.home;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.config.CustomPermissionEvaluator;
import es.musicalia.gestmusica.config.RateLimitingFilter;
import es.musicalia.gestmusica.config.ThymeleafConfig;
import es.musicalia.gestmusica.config.WebSecurityConfig;
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
import org.springframework.boot.test.mock.mockito.MockBean;
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

    @MockBean
    private OcupacionService ocupacionService;

    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private RateLimitingFilter rateLimitingFilter;
    @MockBean
    private CustomPermissionEvaluator customPermissionEvaluator;
    @MockBean
    private MensajeService mensajeService;
    @MockBean
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
                .andExpect(model().attribute("listaOcupacionPendiente", List.of()))
                .andExpect(content().string(not(containsString("carrousel-lista-ocupaciones-pendientes"))));

        verifyNoInteractions(ocupacionService);
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
                .andExpect(content().string(containsString("ocp-card--pendiente")))
                .andExpect(content().string(containsString(">Pendiente<")))
                .andExpect(content().string(containsString(">Reservado<")))
                .andExpect(content().string(not(containsString(">SIN_ESTADO<"))));

        verify(ocupacionService).findOcupacionesDtoByAgenciaPendientes(Set.of(7L));
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
