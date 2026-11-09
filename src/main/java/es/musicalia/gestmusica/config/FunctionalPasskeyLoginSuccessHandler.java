package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.observabilidad.FunctionalEventNames;
import es.musicalia.gestmusica.observabilidad.FunctionalEventOutcome;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpMessageConverterAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;

import java.io.IOException;
import java.util.Map;

/**
 * Tracks a successful passkey login and answers the JSON the browser expects
 * ({@code {"authenticated":true,"redirectUrl":"/"}}). Like the form login, it always lands on {@code /}.
 */
public class FunctionalPasskeyLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final FunctionalEventTracker functionalEventTracker;
    private final HttpMessageConverterAuthenticationSuccessHandler delegate;

    public FunctionalPasskeyLoginSuccessHandler(FunctionalEventTracker functionalEventTracker) {
        this.functionalEventTracker = functionalEventTracker;
        this.delegate = new HttpMessageConverterAuthenticationSuccessHandler();
        this.delegate.setRequestCache(new NullRequestCache());
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Long userId = null;
        String username = null;
        if (authentication != null && authentication.getPrincipal() instanceof CustomAuthenticatedUser principal) {
            userId = principal.getUserId();
            username = principal.getUsuario() != null ? principal.getUsuario().getUsername() : null;
        }
        functionalEventTracker.track(
                FunctionalEventNames.AUTH_LOGIN,
                FunctionalEventOutcome.SUCCESS,
                userId,
                username,
                Map.of("source", "passkey")
        );
        delegate.onAuthenticationSuccess(request, response, authentication);
    }
}
