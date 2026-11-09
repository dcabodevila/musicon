package es.musicalia.gestmusica.passkey;

import es.musicalia.gestmusica.accesoartista.AccesoArtistaService;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.auth.model.CustomUserDetailsServiceImpl;
import es.musicalia.gestmusica.config.UsuarioIdRememberMeServices;
import es.musicalia.gestmusica.permiso.PermisoService;
import es.musicalia.gestmusica.registrologin.RegistroLoginService;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationRequestToken;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Alice (id 1) sets her username to Bob's email. Every password-less path (remember-me, passkey login,
 * passkey registration) must keep Alice as Alice and never resolve Bob (id 2).
 */
class PasskeyIdentityRegressionTest {

    private static final String BOB_EMAIL = "bob@example.test";

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final WebauthnUsuarioRepository handles = mock(WebauthnUsuarioRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private CustomUserDetailsServiceImpl details;
    private PasskeyUserEntityRepository adapter;
    private Usuario alice;

    @BeforeEach
    void preparar() {
        alice = usuario(1L, BOB_EMAIL, "alice@example.test", "alice");
        Usuario bob = usuario(2L, "bob", BOB_EMAIL, "bob");
        when(usuarios.findById(1L)).thenReturn(Optional.of(alice));
        when(usuarios.findById(2L)).thenReturn(Optional.of(bob));
        when(usuarios.findUsuarioActivoByMail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(usuarios.findUsuarioActivoByMail(BOB_EMAIL)).thenReturn(Optional.of(bob));
        when(usuarios.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(usuarios.findByUsername(BOB_EMAIL)).thenReturn(Optional.of(alice));

        WebauthnUsuario handleAlice = handle("AQID", alice);
        WebauthnUsuario handleBob = handle("BAUG", bob);
        when(handles.findById("AQID")).thenReturn(Optional.of(handleAlice));
        when(handles.findById("BAUG")).thenReturn(Optional.of(handleBob));
        when(handles.findByUsuarioId(1L)).thenReturn(Optional.of(handleAlice));
        when(handles.findByUsuarioId(2L)).thenReturn(Optional.of(handleBob));

        details = new CustomUserDetailsServiceImpl(mock(PermisoService.class), usuarios,
                mock(AccesoArtistaService.class), mock(RegistroLoginService.class));
        adapter = new PasskeyUserEntityRepository(handles, usuarios);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginConPasskey_debeAutenticarAlPropietarioDeLaCredencial() {
        var credencialDeAlice = adapter.findById(Bytes.fromBase64("AQID"));
        WebAuthnRelyingPartyOperations rp = mock(WebAuthnRelyingPartyOperations.class);
        when(rp.authenticate(any())).thenReturn(credencialDeAlice);
        PasskeyAuthenticationProvider provider =
                new PasskeyAuthenticationProvider(rp, new PasskeyOwnerLoader(handles, details)::loadOwner);

        Authentication result = provider.authenticate(mock(WebAuthnAuthenticationRequestToken.class));

        assertThat(((CustomAuthenticatedUser) result.getPrincipal()).getUserId()).isEqualTo(1L);
    }

    @Test
    void registroDePasskey_debeUsarElHandleDelUsuarioAutenticado() {
        Authentication auth = entrarComoAlice();
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(adapter.findByUsername(auth.getName()).getId().toBase64UrlString()).isEqualTo("AQID");
    }

    @Test
    void registroDePasskey_nuevoUsuario_debeVincularseAlUsuarioAutenticado() {
        Authentication auth = entrarComoAlice();
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(handles.findByUsuarioId(1L)).thenReturn(Optional.empty());
        when(handles.findById("CQoL")).thenReturn(Optional.empty());

        adapter.save(ImmutablePublicKeyCredentialUserEntity.builder()
                .id(Bytes.fromBase64("CQoL")).name(auth.getName()).displayName("Alice").build());

        org.mockito.ArgumentCaptor<WebauthnUsuario> saved = org.mockito.ArgumentCaptor.forClass(WebauthnUsuario.class);
        org.mockito.Mockito.verify(handles).save(saved.capture());
        assertThat(saved.getValue().getUsuario().getId()).isEqualTo(1L);
    }

    @Test
    void registroDePasskey_sinUsuarioAutenticado_noDebeResolverNingunUsuario() {
        assertThat(adapter.findByUsername(BOB_EMAIL)).isNull();
    }

    @Test
    void recordarme_debeRestaurarLaMismaCuenta() {
        UsuarioIdRememberMeServices remember = new UsuarioIdRememberMeServices("clave-solo-test", details);
        Authentication restored = autoLogin(remember, cookieTrasLogin(remember, entrarComoAlice()));

        assertThat(restored).isNotNull();
        assertThat(((CustomAuthenticatedUser) restored.getPrincipal()).getUserId()).isEqualTo(1L);
    }

    @Test
    void recordarme_usuarioDesactivado_noDebeAutenticar() {
        UsuarioIdRememberMeServices remember = new UsuarioIdRememberMeServices("clave-solo-test", details);
        jakarta.servlet.http.Cookie cookie = cookieTrasLogin(remember, entrarComoAlice());
        alice.setActivo(false);

        assertThat(autoLogin(remember, cookie)).isNull();
    }

    @Test
    void loginConEmail_debeSeguirResolviendoAlDuenoDelEmail() {
        assertThat(((CustomAuthenticatedUser) details.loadUserByUsername(BOB_EMAIL)).getUserId()).isEqualTo(2L);
    }

    private Authentication entrarComoAlice() {
        DaoAuthenticationProvider dao = new DaoAuthenticationProvider(details);
        dao.setPasswordEncoder(encoder);
        return new ProviderManager(dao).authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("alice@example.test", "alice"));
    }

    private static jakarta.servlet.http.Cookie cookieTrasLogin(UsuarioIdRememberMeServices remember, Authentication auth) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("remember-me", "on");
        MockHttpServletResponse response = new MockHttpServletResponse();
        remember.loginSuccess(request, response, auth);
        return response.getCookie("remember-me");
    }

    private static Authentication autoLogin(UsuarioIdRememberMeServices remember, jakarta.servlet.http.Cookie cookie) {
        MockHttpServletRequest siguiente = new MockHttpServletRequest();
        siguiente.setCookies(cookie);
        return remember.autoLogin(siguiente, new MockHttpServletResponse());
    }

    private Usuario usuario(long id, String login, String email, String password) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername(login);
        u.setEmail(email);
        u.setNombre("Usuario " + id);
        u.setActivo(true);
        u.setPassword(encoder.encode(password));
        return u;
    }

    private static WebauthnUsuario handle(String id, Usuario usuario) {
        WebauthnUsuario h = new WebauthnUsuario();
        h.setId(id);
        h.setUsuario(usuario);
        h.setNombreVisible("Dispositivo");
        return h;
    }
}
