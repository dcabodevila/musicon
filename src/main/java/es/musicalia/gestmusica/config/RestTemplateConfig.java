package es.musicalia.gestmusica.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class RestTemplateConfig {

    @Value("${mailgun.api-key}")
    private String mailgunApiKey;

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

    @Bean(name = "nominatimRestTemplate")
    public RestTemplate nominatimRestTemplate(RestTemplateBuilder builder) {
        ClientHttpRequestInterceptor userAgentInterceptor = (request, body, execution) -> {
            request.getHeaders().set("User-Agent", "Festia/1.0 (info@festia.es)");
            return execution.execute(request, body);
        };
        return builder
                .additionalInterceptors(userAgentInterceptor)
                .build();
    }
}