package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.observabilidad.FunctionalEventNames;
import es.musicalia.gestmusica.observabilidad.FunctionalEventOutcome;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FunctionalLoginFailureHandler implements AuthenticationFailureHandler {

    private final FunctionalEventTracker functionalEventTracker;
    private final SimpleUrlAuthenticationFailureHandler delegate;

    public FunctionalLoginFailureHandler(FunctionalEventTracker functionalEventTracker) {
        this.functionalEventTracker = functionalEventTracker;
        this.delegate = new SimpleUrlAuthenticationFailureHandler("/auth/login?error");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String attemptedUsername = request.getParameter("username");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("source", "form_login");
        if (exception != null) {
            attributes.put("reason", exception.getClass().getSimpleName());
        }

        functionalEventTracker.track(
                FunctionalEventNames.AUTH_LOGIN,
                FunctionalEventOutcome.FAILURE,
                null,
                attemptedUsername,
                attributes
        );

        delegate.onAuthenticationFailure(request, response, exception);
    }
}
