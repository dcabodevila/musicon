package es.musicalia.gestmusica.eventopublico;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.view.RedirectView;

final class EventoPublicoUrlHelper {

    private EventoPublicoUrlHelper() {
    }

    static String construirBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        String baseUrl = scheme + "://" + serverName;
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            baseUrl += ":" + serverPort;
        }
        return baseUrl + contextPath;
    }

    static String construirUrlAbsoluta(HttpServletRequest request, String path) {
        return construirBaseUrl(request) + path;
    }

    static RedirectView crearRedireccionPermanente(String destinationUrl) {
        RedirectView redirectView = new RedirectView(destinationUrl);
        redirectView.setExposeModelAttributes(false);
        redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        return redirectView;
    }
}
