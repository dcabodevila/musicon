package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.passkey.PasskeyOffer;
import es.musicalia.gestmusica.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalControllerAdvicePasskeyOfferTest {

    private final GlobalControllerAdvice advice = new GlobalControllerAdvice(mock(MensajeService.class));

    @Test
    void primeraPaginaTrasLoginConContrasena_debeOfrecerPasskeyUnaSolaVez() {
        MockHttpSession session = sesionConOferta();

        ExtendedModelMap primera = new ExtendedModelMap();
        advice.addGlobalAttributes(juan(), primera, paginaHtml(session));
        ExtendedModelMap segunda = new ExtendedModelMap();
        advice.addGlobalAttributes(juan(), segunda, paginaHtml(session));

        assertThat(primera.get("ofrecerPasskey")).isEqualTo(true);
        assertThat(segunda.get("ofrecerPasskey")).isEqualTo(false);
        assertThat(session.getAttribute(PasskeyOffer.SESSION_ATTRIBUTE)).isNull();
    }

    @Test
    void peticionAjax_noDebeConsumirLaOferta() {
        MockHttpSession session = sesionConOferta();
        MockHttpServletRequest ajax = new MockHttpServletRequest("GET", "/mensajes/no-leidos");
        ajax.setSession(session);
        ajax.addHeader("Accept", "application/json");

        ExtendedModelMap model = new ExtendedModelMap();
        advice.addGlobalAttributes(juan(), model, ajax);

        assertThat(model.get("ofrecerPasskey")).isEqualTo(false);
        assertThat(session.getAttribute(PasskeyOffer.SESSION_ATTRIBUTE)).isEqualTo(true);
    }

    @Test
    void sinLoginConContrasena_noDebeOfrecerPasskey() {
        ExtendedModelMap model = new ExtendedModelMap();
        advice.addGlobalAttributes(juan(), model, paginaHtml(new MockHttpSession()));

        assertThat(model.get("ofrecerPasskey")).isEqualTo(false);
    }

    private static MockHttpSession sesionConOferta() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(PasskeyOffer.SESSION_ATTRIBUTE, true);
        return session;
    }

    private static MockHttpServletRequest paginaHtml(MockHttpSession session) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setSession(session);
        request.addHeader("Accept", "text/html,application/xhtml+xml,*/*;q=0.8");
        return request;
    }

    private static CustomAuthenticatedUser juan() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setUsername("jperez");
        usuario.setNombre("Juan");
        usuario.setPassword("{noop}x");
        return new CustomAuthenticatedUser(usuario, true, true, true, true, List.of(), Map.of(), Map.of());
    }
}
