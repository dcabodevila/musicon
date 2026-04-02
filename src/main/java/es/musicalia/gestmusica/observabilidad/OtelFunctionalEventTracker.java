package es.musicalia.gestmusica.observabilidad;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class OtelFunctionalEventTracker implements FunctionalEventTracker {

    private static final String INSTRUMENTATION_SCOPE = "es.musicalia.gestmusica.functional-events";
    private static final String ATTR_EVENT_NAME = "functional.event.name";
    private static final String ATTR_EVENT_OUTCOME = "functional.event.outcome";
    private static final String ATTR_USER_KEY = "functional.user.key";
    private static final String ATTR_ERROR = "functional.error";

    private final Tracer tracer;
    private final boolean functionalEventsEnabled;
    private final String userKeySalt;

    public OtelFunctionalEventTracker(
            @Value("${app.observability.functional.enabled:false}") boolean functionalEventsEnabled,
            @Value("${app.observability.functional.user-key-salt:musicon}") String userKeySalt) {
        this.tracer = GlobalOpenTelemetry.getTracer(INSTRUMENTATION_SCOPE);
        this.functionalEventsEnabled = functionalEventsEnabled;
        this.userKeySalt = userKeySalt;
    }

    @Override
    public void track(String eventName, String outcome, Long userId, String username, Map<String, ?> attributes) {
        if (!functionalEventsEnabled) {
            return;
        }

        Span span = null;
        try {
            span = tracer.spanBuilder("functional.event")
                    .setSpanKind(SpanKind.INTERNAL)
                    .startSpan();

            span.setAttribute(ATTR_EVENT_NAME, eventName);
            span.setAttribute(ATTR_EVENT_OUTCOME, outcome);

            String userKey = buildStableUserKey(userId, username);
            if (userKey != null) {
                span.setAttribute(ATTR_USER_KEY, userKey);
            }

            if (attributes != null) {
                for (Map.Entry<String, ?> entry : attributes.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        span.setAttribute("functional." + entry.getKey(), String.valueOf(entry.getValue()));
                    }
                }
            }

            if (Objects.equals(FunctionalEventOutcome.FAILURE, outcome)) {
                span.setStatus(StatusCode.ERROR);
            }
        } catch (Exception e) {
            log.warn("Error registrando evento funcional {}", eventName, e);
            if (span != null) {
                span.setAttribute(ATTR_ERROR, e.getClass().getSimpleName());
                span.setStatus(StatusCode.ERROR);
            }
        } finally {
            if (span != null) {
                span.end();
            }
        }
    }

    private String buildStableUserKey(Long userId, String username) {
        String base = null;
        if (userId != null) {
            base = "id:" + userId;
        } else if (username != null && !username.isBlank()) {
            base = "username:" + username.trim().toLowerCase();
        }

        if (base == null) {
            return null;
        }

        return hash(base + "|" + userKeySalt);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            log.warn("No se pudo generar hash para user key", e);
            return null;
        }
    }
}
