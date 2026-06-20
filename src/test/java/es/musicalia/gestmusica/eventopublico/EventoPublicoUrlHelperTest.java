package es.musicalia.gestmusica.eventopublico;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EventoPublicoUrlHelperTest {

    @Test
    void construirBaseUrl_debeConservarPuertoNoEstandarYContextPath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("festia.es");
        request.setServerPort(8443);
        request.setContextPath("/musicon");

        assertThat(EventoPublicoUrlHelper.construirBaseUrl(request))
            .isEqualTo("https://festia.es:8443/musicon");
        assertThat(EventoPublicoUrlHelper.construirUrlAbsoluta(request, "/eventos/evento/10-slug"))
            .isEqualTo("https://festia.es:8443/musicon/eventos/evento/10-slug");
    }

    @Test
    void construirBaseUrl_debeOmitirPuertoEstandarYCrearRedirect301SinModeloExpuesto() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("festia.es");
        request.setServerPort(443);
        request.setContextPath("");

        assertThat(EventoPublicoUrlHelper.construirBaseUrl(request))
            .isEqualTo("https://festia.es");

        RedirectView redirectView = EventoPublicoUrlHelper.crearRedireccionPermanente(
            EventoPublicoUrlHelper.construirUrlAbsoluta(request, "/eventos/provincia/Coru%C3%B1a")
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        redirectView.render(Map.of("tracking", "secret"), request, response);

        assertThat(redirectView.getUrl()).isEqualTo("https://festia.es/eventos/provincia/Coru%C3%B1a");
        assertThat(response.getStatus()).isEqualTo(HttpStatus.MOVED_PERMANENTLY.value());
        assertThat(response.getRedirectedUrl()).isEqualTo("https://festia.es/eventos/provincia/Coru%C3%B1a");
        assertThat(response.getHeader("Location")).doesNotContain("tracking=secret");
    }
}
