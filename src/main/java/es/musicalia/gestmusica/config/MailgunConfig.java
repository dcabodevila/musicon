package es.musicalia.gestmusica.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MailgunConfig {

    @Value("${mailgun.api-key}")
    private String apiKey;


    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .basicAuthentication("api", apiKey)
                .build();
    }
}
