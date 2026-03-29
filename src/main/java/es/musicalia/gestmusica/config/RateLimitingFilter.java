package es.musicalia.gestmusica.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int API_MAX_PER_WINDOW = 120;
    private static final int PROGRAMA_CATALOGO_MAX_PER_WINDOW = 90;
    private static final int PROGRAMA_CONSULTA_MAX_PER_WINDOW = 8;
    private static final int AUTH_LOGIN_MAX_PER_WINDOW = 10;
    private static final int AUTH_REMEMBER_PASSWORD_MAX_PER_WINDOW = 5;
    private static final int AUTH_VERIFY_MAX_PER_WINDOW = 15;
    private static final int AUTH_RESEND_MAX_PER_WINDOW = 4;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        Integer max = resolveMax(request, path);
        if (max == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        String key = pathCategory(path) + ":" + clientIp;
        long nowMillis = System.currentTimeMillis();

        WindowCounter updated = counters.compute(key, (k, current) -> {
            if (current == null || nowMillis - current.windowStartMillis >= WINDOW.toMillis()) {
                return new WindowCounter(nowMillis, 1);
            }
            current.count++;
            return current;
        });

        if (updated.count > max) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
            response.getWriter().write("{\"error\":\"too_many_requests\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Integer resolveMax(HttpServletRequest request, String path) {
        if (path == null) {
            return null;
        }

        if (path.startsWith("/eventos/api/")) {
            return API_MAX_PER_WINDOW;
        }
        if (path.startsWith("/programa/api/catalogo")) {
            return PROGRAMA_CATALOGO_MAX_PER_WINDOW;
        }
        if (path.startsWith("/programa/api/consulta")) {
            return PROGRAMA_CONSULTA_MAX_PER_WINDOW;
        }

        if (!HttpMethod.POST.matches(request.getMethod())) {
            return null;
        }

        if ("/auth/login".equals(path)) {
            return AUTH_LOGIN_MAX_PER_WINDOW;
        }
        if ("/auth/remember-password".equals(path)) {
            return AUTH_REMEMBER_PASSWORD_MAX_PER_WINDOW;
        }
        if ("/auth/verify-email".equals(path)
            || "/auth/verify-reset-password".equals(path)) {
            return AUTH_VERIFY_MAX_PER_WINDOW;
        }
        if ("/auth/resend-code".equals(path)
            || "/auth/resend-reset-code".equals(path)) {
            return AUTH_RESEND_MAX_PER_WINDOW;
        }

        return null;
    }

    private String pathCategory(String path) {
        if (path != null) {
            if (path.startsWith("/programa/api/catalogo")) {
                return "programa_catalogo";
            }
            if (path.startsWith("/programa/api/consulta")) {
                return "programa_consulta";
            }
            if ("/auth/login".equals(path)) {
                return "auth_login";
            }
            if ("/auth/remember-password".equals(path)) {
                return "auth_remember_password";
            }
            if ("/auth/verify-email".equals(path)
                || "/auth/verify-reset-password".equals(path)) {
                return "auth_verify_code";
            }
            if ("/auth/resend-code".equals(path)
                || "/auth/resend-reset-code".equals(path)) {
                return "auth_resend_code";
            }
        }
        return "eventos_api";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class WindowCounter {
        private final long windowStartMillis;
        private int count;

        private WindowCounter(long windowStartMillis, int count) {
            this.windowStartMillis = windowStartMillis;
            this.count = count;
        }
    }
}
