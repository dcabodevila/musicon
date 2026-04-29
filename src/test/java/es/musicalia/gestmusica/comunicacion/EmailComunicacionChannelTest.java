package es.musicalia.gestmusica.comunicacion;

import es.musicalia.gestmusica.mail.EmailService;
import es.musicalia.gestmusica.usuario.EnvioEmailException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailComunicacionChannelTest {

    @Mock
    private EmailService emailService;

    @Mock
    private SpringTemplateEngine templateEngine;

    private EmailComunicacionChannel emailChannel;

    @BeforeEach
    void setUp() {
        emailChannel = new EmailComunicacionChannel(emailService, templateEngine);
    }

    @Test
    void enviar_conParametrosValidos_debeLlamarEmailService() throws Exception {
        // Arrange
        String destinatario = "test@festia.es";
        String asunto = "Asunto de prueba";
        String contenido = "<p>Contenido HTML</p>";
        String htmlProcesado = "<html>" + contenido + "</html>";

        when(templateEngine.process(eq("comunicacion-email"), any(Context.class)))
                .thenReturn(htmlProcesado);

        // Act
        emailChannel.enviar(destinatario, asunto, contenido);

        // Assert
        ArgumentCaptor<String> contenidoCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarCorreoHtmlConCc(
                eq(destinatario),
                eq(null),
                eq(asunto),
                contenidoCaptor.capture()
        );
        assertThat(contenidoCaptor.getValue()).isEqualTo(htmlProcesado);
    }

    @Test
    void enviar_conContenidoHtmlEnvuelto_debePreservarContenido() throws Exception {
        // Arrange
        String destinatario = "user@example.com";
        String asunto = "Promoción";
        String contenido = "<h1>Título</h1><p>Texto con <strong>negrita</strong></p>";
        String htmlProcesado = "<html>" + contenido + "</html>";

        when(templateEngine.process(eq("comunicacion-email"), any(Context.class)))
                .thenReturn(htmlProcesado);

        // Act
        emailChannel.enviar(destinatario, asunto, contenido);

        // Assert
        ArgumentCaptor<String> contenidoCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarCorreoHtmlConCc(
                eq(destinatario),
                eq(null),
                eq(asunto),
                contenidoCaptor.capture()
        );
        assertThat(contenidoCaptor.getValue()).contains("<h1>Título</h1>");
        assertThat(contenidoCaptor.getValue()).contains("<strong>negrita</strong>");
    }

    @Test
    void enviar_cuandoEmailServiceFalla_debePropagarExcepcion() throws Exception {
        // Arrange
        String destinatario = "test@festia.es";
        String asunto = "Asunto";
        String contenido = "<p>Contenido</p>";
        String htmlProcesado = "<html>" + contenido + "</html>";

        when(templateEngine.process(eq("comunicacion-email"), any(Context.class)))
                .thenReturn(htmlProcesado);

        doThrow(new EnvioEmailException("Error de conexión"))
                .when(emailService).enviarCorreoHtmlConCc(eq(destinatario), eq(null), eq(asunto), any());

        // Act & Assert
        assertThatThrownBy(() -> emailChannel.enviar(destinatario, asunto, contenido))
                .isInstanceOf(EnvioEmailException.class);
    }
}
