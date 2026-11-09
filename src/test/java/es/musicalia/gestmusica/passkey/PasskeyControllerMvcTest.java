package es.musicalia.gestmusica.passkey;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.config.CustomPermissionEvaluator;
import es.musicalia.gestmusica.config.RateLimitingFilter;
import es.musicalia.gestmusica.config.WebSecurityConfig;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasskeyController.class)
@Import(WebSecurityConfig.class)
class PasskeyControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasskeyService passkeyService;
    @MockitoBean
    private FunctionalEventTracker functionalEventTracker;
    @MockitoBean
    private MensajeService mensajeService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private RateLimitingFilter rateLimitingFilter;
    @MockitoBean
    private CustomPermissionEvaluator customPermissionEvaluator;
    @MockitoBean
    private PublicKeyCredentialUserEntityRepository userEntityRepository;
    @MockitoBean
    private UserCredentialRepository userCredentialRepository;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());
    }

    @Test
    void listar_debeDevolverLasPasskeysDelUsuarioConectado() throws Exception {
        when(passkeyService.listarPasskeys(7L)).thenReturn(List.of(new PasskeyDto("cred-1", "iPhone de Juan",
                Instant.parse("2026-11-09T10:00:00Z"), Instant.parse("2026-11-10T08:00:00Z"))));

        mockMvc.perform(get("/passkeys").with(user(juan())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("cred-1"))
                .andExpect(jsonPath("$[0].etiqueta").value("iPhone de Juan"))
                .andExpect(jsonPath("$[0].fechaCreacion").value("2026-11-09T10:00:00Z"))
                .andExpect(jsonPath("$[0].fechaUltimoUso").value("2026-11-10T08:00:00Z"));
    }

    @Test
    void listar_anonimo_debeRedirigirAlLogin() throws Exception {
        mockMvc.perform(get("/passkeys"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));

        verify(passkeyService, never()).listarPasskeys(anyLong());
    }

    @Test
    void eliminar_propia_debeResponder204() throws Exception {
        when(passkeyService.eliminarPasskey(7L, "cred-1")).thenReturn(true);

        mockMvc.perform(delete("/passkeys/cred-1").with(csrf()).with(user(juan())))
                .andExpect(status().isNoContent());

        verify(passkeyService).eliminarPasskey(7L, "cred-1");
    }

    @Test
    void eliminar_ajenaOInexistente_debeResponder404() throws Exception {
        when(passkeyService.eliminarPasskey(7L, "cred-ana")).thenReturn(false);

        mockMvc.perform(delete("/passkeys/cred-ana").with(csrf()).with(user(juan())))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_sinCsrf_debeRechazarseSinBorrar() throws Exception {
        mockMvc.perform(delete("/passkeys/cred-1").with(user(juan())))
                .andExpect(status().isForbidden());

        verify(passkeyService, never()).eliminarPasskey(anyLong(), any());
    }

    private static CustomAuthenticatedUser juan() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setUsername("jperez");
        usuario.setNombre("Juan");
        usuario.setPassword("{noop}x");
        return new CustomAuthenticatedUser(usuario, true, true, true, true, List.of(), Map.of(), Map.of());
    }
}
