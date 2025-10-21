package es.musicalia.gestmusica.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    @Value("${fixie.socks.host:}")
    private String fixieSocksHost;

    // Configuración PostgreSQL (Principal)
    @Primary
    @Bean(name = "primaryDataSource")
    @ConfigurationProperties("spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Primary
    @Bean(name = "primaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
            @Qualifier("primaryDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("es.musicalia.gestmusica");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "validate");
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.show_sql", false);
        properties.put("hibernate.format_sql", true);
        properties.put("hibernate.jdbc.batch_size", 15);
        properties.put("hibernate.jdbc.fetch_size", 50);
        properties.put("hibernate.naming.physical-strategy",
                      "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        properties.put("hibernate.naming.implicit-strategy",
                      "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy");
        properties.put("hibernate.keyword_auto_quoting_enabled", true);

        em.setJpaPropertyMap(properties);
        em.setPersistenceUnitName("primary");

        return em;
    }

    @Primary
    @Bean(name = "primaryTransactionManager")
    public PlatformTransactionManager primaryTransactionManager(
            @Qualifier("primaryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
    @PostConstruct
    public void configureFixieSocksProxy() {
        if (fixieSocksHost != null && !fixieSocksHost.isEmpty()) {
            configureSocketsProxy(fixieSocksHost);
        }
    }

    private void configureSocketsProxy(String fixieSocksHost) {
        try {
            URL fixie = new URL(fixieSocksHost);
            String[] fixieUserInfo = fixie.getUserInfo().split(":");
            String fixieUser = fixieUserInfo[0];
            String fixiePassword = fixieUserInfo[1];

            // Configurar SOCKS proxy
            System.setProperty("socksProxyHost", fixie.getHost());
            System.setProperty("socksProxyPort", String.valueOf(fixie.getPort()));

            // Configurar autenticación del proxy
            Authenticator.setDefault(new ProxyAuthenticator(fixieUser, fixiePassword));

        } catch (Exception e) {
            System.err.println("Error configurando SOCKS proxy Fixie: " + e.getMessage());
        }
    }

    private static class ProxyAuthenticator extends Authenticator {
        private final PasswordAuthentication passwordAuthentication;

        private ProxyAuthenticator(String user, String password) {
            passwordAuthentication = new PasswordAuthentication(user, password.toCharArray());
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return passwordAuthentication;
        }
    }

    // Configuración MariaDB (Secundaria) - CONDICIONAL
    @Bean(name = "mariadbDataSource")
    @ConfigurationProperties("spring.datasource.mariadb")
    @ConditionalOnProperty(
            name = "mariadb.datasource.enabled",
            havingValue = "true",
            matchIfMissing = false
    )
    public DataSource mariadbDataSource() {
        HikariDataSource dataSource = (HikariDataSource) DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
        // Configurar SOCKS5 proxy para Fixie
        String fixieSocksUrl = System.getenv("FIXIE_SOCKS_URL");
        if (fixieSocksUrl != null && !fixieSocksUrl.isEmpty()) {
            // Parsear la URL de Fixie Socks (formato: socks5://user:pass@host:port)
            // y configurar las propiedades del sistema para SOCKS
            configureSocksProxy(fixieSocksUrl);
        }

        return dataSource;
    }

    private void configureSocksProxy(String socksUrl) {
        // Implementar parsing de la URL y configuración del proxy SOCKS
        // System.setProperty("socksProxyHost", host);
        // System.setProperty("socksProxyPort", port);
        // System.setProperty("java.net.socks.username", username);
        // System.setProperty("java.net.socks.password", password);
    }

    @Bean(name = "mariadbEntityManagerFactory")
    @ConditionalOnProperty(
            name = "mariadb.datasource.enabled",
            havingValue = "true",
            matchIfMissing = false
    )
    public LocalContainerEntityManagerFactoryBean mariadbEntityManagerFactory(
            @Qualifier("mariadbDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("es.musicalia.gestmusicalegacy");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "validate");
        properties.put("hibernate.dialect", "org.hibernate.dialect.MariaDBDialect");
        properties.put("hibernate.show_sql", false);
        properties.put("hibernate.format_sql", true);
        properties.put("hibernate.jdbc.batch_size", 15);
        properties.put("hibernate.jdbc.fetch_size", 50);
        properties.put("hibernate.naming.physical-strategy",
                      "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        properties.put("hibernate.naming.implicit-strategy",
                      "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy");

        em.setJpaPropertyMap(properties);
        em.setPersistenceUnitName("mariadb");

        return em;
    }

    @Bean(name = "mariadbTransactionManager")
    @ConditionalOnProperty(
            name = "mariadb.datasource.enabled",
            havingValue = "true",
            matchIfMissing = false
    )
    public PlatformTransactionManager mariadbTransactionManager(
            @Qualifier("mariadbEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    /**
     * Bean manual para OcupacionLegacyService 
     * SOLUCIÓN AL PROBLEMA: Crear el bean manualmente - CONDICIONAL
     */
    @Bean("ocupacionLegacyService")
    @ConditionalOnProperty(
            name = "mariadb.datasource.enabled",
            havingValue = "true",
            matchIfMissing = false
    )
    public es.musicalia.gestmusicalegacy.ocupacion.OcupacionLegacyService ocupacionLegacyService(
            es.musicalia.gestmusicalegacy.ocupacion.OcupacionLegacyRepository repository) {
        return new es.musicalia.gestmusicalegacy.ocupacion.OcupacionLegacyServiceImpl(repository);
    }
}