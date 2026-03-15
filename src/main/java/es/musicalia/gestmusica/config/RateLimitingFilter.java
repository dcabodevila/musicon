package es.musicalia.gestmusica.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        Integer max = resolveMax(path);
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
            response.getWriter().write("{\"error\":\"too_many_requests\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Integer resolveMax(String path) {
        if (path == null) {
            return null;
        }
        if (path.startsWith("/eventos/api/")) {
            return API_MAX_PER_WINDOW;
        }
        return null;
    }

    private String pathCategory(String path) {
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
