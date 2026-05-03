package es.musicalia.gestmusica.auth.model;

import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private CustomUserDetailsServiceImpl userDetailsService;
    @Mock
    private UserService userService;
    @Mock
    private SessionRegistry sessionRegistry;

    @InjectMocks
    private SecurityServiceImpl securityService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== findLoggedInUsername ====================

    @Test
    void findLoggedInUsername_conUserDetails_devuelveUsername() {
        UserDetails userDetails = User.withUsername("testuser").password("pass").authorities("USER").build();
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(userDetails);
        SecurityContextHolder.getContext().setAuthentication(auth);

        String result = securityService.findLoggedInUsername();

        assertEquals("testuser", result);
    }

    @Test
    void findLoggedInUsername_sinUserDetails_devuelveNull() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn("not a UserDetails");
        SecurityContextHolder.getContext().setAuthentication(auth);

        String result = securityService.findLoggedInUsername();

        assertNull(result);
    }

    // ==================== reloadUserAuthorities ====================

    @Test
    void reloadUserAuthorities_actualizaSecurityContext() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("testuser");

        UserDetails freshUserDetails = new User("testuser", "pass",
            Collections.singletonList(new SimpleGrantedAuthority("ADMIN")));

        when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuario));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(freshUserDetails);

        UsernamePasswordAuthenticationToken currentAuth = new UsernamePasswordAuthenticationToken(
            "testuser", "pass", Collections.singletonList(new SimpleGrantedAuthority("USER"))
        );
        currentAuth.setDetails("someDetails");
        SecurityContextHolder.getContext().setAuthentication(currentAuth);

        securityService.reloadUserAuthorities();

        Authentication updatedAuth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(updatedAuth);
        assertTrue(updatedAuth.isAuthenticated());
        assertEquals(freshUserDetails, updatedAuth.getPrincipal());
        assertEquals("someDetails", updatedAuth.getDetails());
    }

    // ==================== invalidarSesionDeUsuario ====================

    @Test
    void invalidarSesionDeUsuario_conSesiones_expiraSesiones() {
        CustomAuthenticatedUser user = mock(CustomAuthenticatedUser.class);
        when(user.getUserId()).thenReturn(1L);

        SessionInformation sessionInfo = mock(SessionInformation.class);

        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(user));
        when(sessionRegistry.getAllSessions(user, false)).thenReturn(List.of(sessionInfo));

        securityService.invalidarSesionDeUsuario(1L);

        verify(sessionInfo).expireNow();
    }

    @Test
    void invalidarSesionDeUsuario_sinCoincidencias_noHaceNada() {
        CustomAuthenticatedUser user = mock(CustomAuthenticatedUser.class);
        when(user.getUserId()).thenReturn(2L);

        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(user));

        securityService.invalidarSesionDeUsuario(1L);

        verify(sessionRegistry, never()).getAllSessions(any(), anyBoolean());
    }

    @Test
    void invalidarSesionDeUsuario_idNull_warningLog() {
        securityService.invalidarSesionDeUsuario(null);

        verifyNoInteractions(sessionRegistry);
    }

    // ==================== recargarOInvalidarSesion ====================

    @Test
    void recargarOInvalidarSesion_mismoUsuario_recargaAuthorities() {
        Usuario usuarioActual = new Usuario();
        usuarioActual.setId(1L);
        usuarioActual.setUsername("testuser");

        UserDetails freshUserDetails = new User("testuser", "pass",
            Collections.singletonList(new SimpleGrantedAuthority("ADMIN")));

        when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuarioActual));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(freshUserDetails);

        UsernamePasswordAuthenticationToken currentAuth = new UsernamePasswordAuthenticationToken(
            "testuser", "pass", Collections.singletonList(new SimpleGrantedAuthority("USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(currentAuth);

        securityService.recargarOInvalidarSesion(1L);

        Authentication updatedAuth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(updatedAuth);
        assertTrue(updatedAuth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ADMIN")));
    }

    @Test
    void recargarOInvalidarSesion_otroUsuario_invalidaSesion() {
        Usuario usuarioActual = new Usuario();
        usuarioActual.setId(1L);

        when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuarioActual));
        when(sessionRegistry.getAllPrincipals()).thenReturn(Collections.emptyList());

        securityService.recargarOInvalidarSesion(2L);

        verify(sessionRegistry).getAllPrincipals();
    }

    @Test
    void recargarOInvalidarSesion_idNull_warning() {
        securityService.recargarOInvalidarSesion(null);

        verifyNoInteractions(userService);
    }

    // ==================== autologin ====================

    @Test
    void autologin_exitoso_registraSesion() {
        UserDetails userDetails = new User("test@test.com", "pass",
            Collections.singletonList(new SimpleGrantedAuthority("USER")));

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);

        when(userDetailsService.loadUserByUsername("test@test.com")).thenReturn(userDetails);
        when(request.getSession(true)).thenReturn(session);
        when(session.getId()).thenReturn("session-id-123");

        securityService.autologin("test@test.com", request);

        verify(session).setAttribute(eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY), any());
        verify(sessionRegistry).registerNewSession("session-id-123", userDetails);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void autologin_fallido_lanzaRuntimeException() {
        when(userDetailsService.loadUserByUsername("test@test.com"))
            .thenThrow(new RuntimeException("Usuario no encontrado"));

        HttpServletRequest request = mock(HttpServletRequest.class);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            securityService.autologin("test@test.com", request)
        );

        assertTrue(exception.getMessage().contains("Error al realizar autologin"));
    }
}
