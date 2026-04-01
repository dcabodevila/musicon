package es.musicalia.gestmusica.mail;

import es.musicalia.gestmusica.reactivacion.ReactivacionTokenService;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EmailServiceTest {

    private final EmailService emailService = new EmailService(
            mock(JavaMailSender.class),
            mock(SpringTemplateEngine.class),
            mock(MailgunEmailService.class),
            mock(UsuarioRepository.class),
            mock(ReactivacionTokenService.class)
    );

    @Test
    void generarUrlGraficoQuickChart_debeIncluirBeginAtZeroYMinCero() {
        List<Map<String, Object>> chartData = List.of(
                Map.of("mes", "Enero", "cantidad", 2L),
                Map.of("mes", "Febrero", "cantidad", 5L)
        );

        String url = ReflectionTestUtils.invokeMethod(emailService, "generarUrlGraficoQuickChart", chartData);

        assertThat(url).contains("https://quickchart.io/chart?");
        String encodedConfig = url.substring(url.indexOf("&c=") + 3);
        String decodedConfig = URLDecoder.decode(encodedConfig, StandardCharsets.UTF_8);

        assertThat(decodedConfig).contains("beginAtZero:true");
        assertThat(decodedConfig).contains("min:0");
    }
}
