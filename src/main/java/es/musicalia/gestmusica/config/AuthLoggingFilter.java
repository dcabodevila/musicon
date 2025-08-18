package es.musicalia.gestmusica.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuthLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String requestURI = httpRequest.getRequestURI();
            if (requestURI.startsWith("/auth")) { // Registrar solo las peticiones bajo /auth
                logger.info("Solicitud recibida para {} con método {}", requestURI, httpRequest.getMethod());
            }
        }
        chain.doFilter(request, response); // Continuar la cadena de filtros
    }
}
