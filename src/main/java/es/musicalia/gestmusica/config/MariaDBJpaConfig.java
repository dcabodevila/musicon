package es.musicalia.gestmusica.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ConditionalOnProperty(
        name = "mariadb.datasource.enabled",
        havingValue = "true",
        matchIfMissing = false
)
@EnableJpaRepositories(
        basePackages = "es.musicalia.gestmusicalegacy",
        entityManagerFactoryRef = "mariadbEntityManagerFactory",
        transactionManagerRef = "mariadbTransactionManager"
)
public class MariaDBJpaConfig {
}