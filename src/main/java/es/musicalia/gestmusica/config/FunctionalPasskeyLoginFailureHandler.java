package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.observabilidad.FunctionalEventNames;
import es.musicalia.gestmusica.observabilidad.FunctionalEventOutcome;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks a failed passkey login and answers 401 to the browser (the login page shows the message).
 */
public class FunctionalPasskeyLoginFailureHandler implements AuthenticationFailureHandler {

    private final FunctionalEventTracker functionalEventTracker;

    public FunctionalPasskeyLoginFailureHandler(FunctionalEventTracker functionalEventTracker) {
        this.functionalEventTracker = functionalEventTracker;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("source", "passkey");
        if (exception != null) {
            attributes.put("reason", exception.getClass().getSimpleName());
        }
        functionalEventTracker.track(
                FunctionalEventNames.AUTH_LOGIN,
                FunctionalEventOutcome.FAILURE,
                null,
                null,
                attributes
        );
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
