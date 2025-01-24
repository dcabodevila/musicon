package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.util.DateUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThymeleafConfig {
    @Bean
    public DateUtils dateUtils() {
        return new DateUtils();
    }
}