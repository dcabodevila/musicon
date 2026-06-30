package es.musicalia.gestmusica.actividad;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.config.CustomPermissionEvaluator;
import es.musicalia.gestmusica.config.RateLimitingFilter;
import es.musicalia.gestmusica.config.ThymeleafConfig;
import es.musicalia.gestmusica.config.WebSecurityConfig;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRecord;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = ActividadController.class)
@Import({WebSecurityConfig.class, ThymeleafConfig.class})
class ActividadControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActividadService actividadService;

    @MockBean
    private SessionRegistry sessionRegistry;

    @MockBean
    private UserService userService;

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

        when(sessionRegistry.getAllPrincipals()).thenReturn(Collections.emptyList());
        when(mensajeService.obtenerMensajesRecibidos(any())).thenReturn(Collections.emptyList());
        when(actividadService.findActividadTarifas()).thenReturn(List.of());
        when(actividadService.findActividadOcupaciones()).thenReturn(List.of());
        when(userService.findAllUsuarioRecords()).thenReturn(List.of(new UsuarioRecord(1L, "Admin User", "Festia")));
    }

    @Test
    void actividad_adminRenderizaSelectorSinAutocarga() throws Exception {
        when(actividadService.findActiveArtistOptions()).thenReturn(List.of(
            new ActividadArtistaOptionRecord(7L, "Los Satélites"),
            new ActividadArtistaOptionRecord(8L, "La Verbena")
        ));

        mockMvc.perform(get("/actividad").with(user(adminUser())))
            .andExpect(status().isOk())
            .andExpect(view().name("actividad"))
            .andExpect(model().attributeExists("activeArtists"))
            .andExpect(content().string(containsString("artistHeatmapSelect")))
            .andExpect(content().string(containsString("-- Selecciona un artista --")))
            .andExpect(content().string(containsString("Los Satélites")))
            .andExpect(content().string(containsString("data-heatmap-url=\"/actividad/ocupaciones-heatmap\"")));
    }

    @Test
    void actividad_incluyeBundleAdminKitConApexChartsAntesDeActividadJs() throws Exception {
        when(actividadService.findActiveArtistOptions()).thenReturn(List.of());

        String html = mockMvc.perform(get("/actividad").with(user(adminUser())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String adminkitScriptPrefix = "/adminkit/js/app";
        String actividadScriptPrefix = "/js/actividad";

        assertThat(html).contains(adminkitScriptPrefix);
        assertThat(html).contains(actividadScriptPrefix);
        assertThat(html.indexOf(adminkitScriptPrefix))
            .isLessThan(html.indexOf(actividadScriptPrefix));
    }

    @Test
    void ocupacionesHeatmap_adminRecibeContratoJson() throws Exception {
        when(actividadService.findOcupacionesHeatmap(7L)).thenReturn(
            new ActividadOcupacionesHeatmapResponse(
                7L,
                LocalDate.of(2025, 7, 1),
                LocalDate.of(2026, 6, 30),
                java.util.stream.IntStream.rangeClosed(1, 31).boxed().toList(),
                List.of(new ActividadHeatmapMonthRowRecord(
                    "2025-07",
                    "Julio",
                    List.of(
                        new ActividadHeatmapCellRecord(1, 0),
                        new ActividadHeatmapCellRecord(2, 2)
                    )
                ))
            )
        );

        mockMvc.perform(get("/actividad/ocupaciones-heatmap").param("artistId", "7").with(user(adminUser())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.artistId").value(7))
            .andExpect(jsonPath("$.from").value("2025-07-01"))
            .andExpect(jsonPath("$.to").value("2026-06-30"))
            .andExpect(jsonPath("$.days[0]").value(1))
            .andExpect(jsonPath("$.days[30]").value(31))
            .andExpect(jsonPath("$.series[0].month").value("2025-07"))
            .andExpect(jsonPath("$.series[0].label").value("Julio"))
            .andExpect(jsonPath("$.series[0].data[1].day").value(2))
            .andExpect(jsonPath("$.series[0].data[1].count").value(2));
    }

    @Test
    void ocupacionesHeatmap_adminRecibe404ParaArtistaInexistenteOInactivo() throws Exception {
        when(actividadService.findOcupacionesHeatmap(99L)).thenThrow(new ResponseStatusException(NOT_FOUND));

        mockMvc.perform(get("/actividad/ocupaciones-heatmap").param("artistId", "99").with(user(adminUser())))
            .andExpect(status().isNotFound());
    }

    @Test
    void actividad_yHeatmap_denieganUsuariosSinPermisoAdmin() throws Exception {
        when(actividadService.findActiveArtistOptions()).thenReturn(List.of());

        mockMvc.perform(get("/actividad").with(user(nonAdminUser())))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/actividad/ocupaciones-heatmap").param("artistId", "7").with(user(nonAdminUser())))
            .andExpect(status().isForbidden());
    }

    private CustomAuthenticatedUser adminUser() {
        return authenticatedUser(Set.of(new SimpleGrantedAuthority("ACCESO_PANEL_ADMIN")));
    }

    private CustomAuthenticatedUser nonAdminUser() {
        return authenticatedUser(Set.of(new SimpleGrantedAuthority("USER")));
    }

    private CustomAuthenticatedUser authenticatedUser(Set<SimpleGrantedAuthority> authorities) {
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setNombre("Admin");
        usuario.setApellidos("User");
        usuario.setPassword("secret");
        usuario.setValidado(true);

        return new CustomAuthenticatedUser(usuario, true, true, true, true, authorities, Map.of(), Map.of());
    }
}
