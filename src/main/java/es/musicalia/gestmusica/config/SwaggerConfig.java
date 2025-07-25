package es.musicalia.gestmusica.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GestMusica API")
                        .version("0.0.13")
                        .description("API para la gestión de agrupaciones musicales")
                        .contact(new Contact()
                                .name("Gestmusica")
                                .email("gestmusica@gestmusica.com")))
                .servers(List.of(
                        new Server().url(System.getenv("SERVER_URL") != null ? System.getenv("SERVER_URL") : "http://localhost:8081").description("Servidor local")
                ));
    }

    @Bean
    public GroupedOpenApi apiGroup() {
        return GroupedOpenApi.builder()
                .group("api")
                .displayName("GestMusica API")
                .pathsToMatch("/api/**")
                .build();
    }

}

