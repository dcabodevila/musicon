package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.observabilidad.FunctionalEventNames;
import es.musicalia.gestmusica.observabilidad.FunctionalEventOutcome;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.io.IOException;
import java.util.Map;

public class FunctionalLogoutSuccessHandler implements LogoutSuccessHandler {

    private final FunctionalEventTracker functionalEventTracker;

    public FunctionalLogoutSuccessHandler(FunctionalEventTracker functionalEventTracker) {
        this.functionalEventTracker = functionalEventTracker;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Long userId = null;
        String username = null;

        if (authentication != null && authentication.getPrincipal() instanceof CustomAuthenticatedUser principal) {
            userId = principal.getUserId();
            username = principal.getUsuario() != null ? principal.getUsuario().getUsername() : null;
        }

        functionalEventTracker.track(
                FunctionalEventNames.AUTH_LOGOUT,
                FunctionalEventOutcome.SUCCESS,
                userId,
                username,
                Map.of("source", "logout")
        );

        response.sendRedirect(request.getContextPath() + "/auth/login?logout");
    }
}
