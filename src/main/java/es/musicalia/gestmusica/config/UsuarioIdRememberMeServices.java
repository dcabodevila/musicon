package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.auth.model.UsuarioUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

/**
 * Remember-me cookie bound to the user id instead of the login name.
 * <p>
 * A login name can be ambiguous (a username may look like another user's email) and may contain characters that
 * do not survive the cookie format; the id cannot. The cookie stays signed with the user's password hash, so a
 * password change still invalidates it, and a deactivated user is not signed in.
 */
public class UsuarioIdRememberMeServices extends TokenBasedRememberMeServices {

    /** User id read from the cookie being checked; Spring re-signs with UserDetails#getUsername() instead. */
    private static final ThreadLocal<String> COOKIE_USUARIO_ID = new ThreadLocal<>();

    public UsuarioIdRememberMeServices(String key, UsuarioUserDetailsService usuarioUserDetailsService) {
        super(key, porId(usuarioUserDetailsService), RememberMeTokenAlgorithm.SHA256);
    }

    @Override
    protected String retrieveUserName(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomAuthenticatedUser user) {
            return String.valueOf(user.getUserId());
        }
        // Without the application principal there is no stable id: issue no cookie.
        return null;
    }

    @Override
    protected UserDetails processAutoLoginCookie(String[] cookieTokens, HttpServletRequest request,
                                                 HttpServletResponse response) {
        COOKIE_USUARIO_ID.set(cookieTokens.length > 0 ? cookieTokens[0] : null);
        try {
            return super.processAutoLoginCookie(cookieTokens, request, response);
        } finally {
            COOKIE_USUARIO_ID.remove();
        }
    }

    /** Signs and checks with the user id, both when issuing and when validating the cookie. */
    @Override
    protected String makeTokenSignature(long tokenExpiryTime, String username, String password,
                                        RememberMeTokenAlgorithm algorithm) {
        String usuarioId = COOKIE_USUARIO_ID.get();
        return super.makeTokenSignature(tokenExpiryTime, usuarioId != null ? usuarioId : username, password, algorithm);
    }

    private static UserDetailsService porId(UsuarioUserDetailsService usuarioUserDetailsService) {
        return id -> {
            final long usuarioId;
            try {
                usuarioId = Long.parseLong(id);
            } catch (NumberFormatException ex) {
                throw new UsernameNotFoundException("Remember-me cookie without a user id");
            }
            return usuarioUserDetailsService.loadUserByUsuarioId(usuarioId, false);
        };
    }
}
