package es.musicalia.gestmusica.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Value("${mailgun.api-key}")
    private String mailgunApiKey;

    @Value("${orquestas.galicia.api.token}")
    private String orquestasApiToken;

    @Bean(name = "mailgunRestTemplate")
    public RestTemplate mailgunRestTemplate(RestTemplateBuilder builder) {
        return builder
                .basicAuthentication("api", mailgunApiKey)
                .build();
    }

    @Bean(name = "orquestasRestTemplate")
    public RestTemplate orquestasRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}