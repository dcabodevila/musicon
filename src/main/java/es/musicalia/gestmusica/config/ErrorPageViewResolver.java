package es.musicalia.gestmusica.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Component
public class ErrorPageViewResolver implements ErrorViewResolver {

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        if (status == null) {
            return new ModelAndView("error/error", model);
        }

        return switch (status.value()) {
            case 403 -> new ModelAndView("error/403", model, status);
            case 404 -> new ModelAndView("error/404", model, status);
            case 500 -> new ModelAndView("error/500", model, status);
            default -> new ModelAndView("error/error", model, status);
        };
    }
}
