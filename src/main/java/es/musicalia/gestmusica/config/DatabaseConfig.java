package es.musicalia.gestmusica.config;

import com.zaxxer.hikari.HikariDataSource;
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
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    // Inyección de propiedades via @Value
    @Value("${mariadb.proxy.enabled:false}")
    private boolean proxyEnabled;
    
    @Value("${fixie.socks.host:}")
    private String fixieHost;
    
    @Value("${fixie.username:}")
    private String fixieUsername;
    
    @Value("${fixie.password:}")
    private String fixiePassword;
    
    @Value("${spring.datasource.mariadb.url:}")
    private String mariadbUrl;
    
    @Value("${spring.datasource.mariadb.username:}")
    private String mariadbUsername;
    
    @Value("${spring.datasource.mariadb.password:}")
    private String mariadbPassword;

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

    // Configuración MariaDB (Secundaria) - CONDICIONAL
    @Bean(name = "mariadbDataSource")
    @ConfigurationProperties("spring.datasource.mariadb")
    @ConditionalOnProperty(
            name = "mariadb.datasource.enabled",
            havingValue = "true",
            matchIfMissing = false
    )
    public DataSource mariadbDataSource() {
        System.out.println("🔧 Configurando DataSource MariaDB...");
        
        HikariDataSource dataSource = new HikariDataSource();
        
        // Construir URL con parámetros de proxy específicos para MariaDB
        String finalUrl = buildMariaDBUrlWithProxy();
        
        dataSource.setJdbcUrl(finalUrl);
        dataSource.setUsername(mariadbUsername);
        dataSource.setPassword(mariadbPassword);
        dataSource.setDriverClassName("org.mariadb.jdbc.Driver");
        
        // Configuraciones optimizadas para conexiones proxy
        dataSource.setMaximumPoolSize(3);  // Reducido para proxy
        dataSource.setMinimumIdle(1);
        dataSource.setConnectionTimeout(60000); // 60 segundos para proxy
        dataSource.setIdleTimeout(300000);
        dataSource.setMaxLifetime(1800000);
        dataSource.setLeakDetectionThreshold(60000);
        dataSource.setValidationTimeout(10000);  // Aumentado para proxy
        dataSource.setConnectionTestQuery("SELECT 1");
        
        // Propiedades adicionales para conexiones proxy
        dataSource.addDataSourceProperty("connectTimeout", "60000");
        dataSource.addDataSourceProperty("socketTimeout", "60000");
        
        System.out.println("✅ DataSource MariaDB configurado con URL: " + finalUrl);
        return dataSource;
    }

    private String buildMariaDBUrlWithProxy() {
        String baseUrl = mariadbUrl;
        
        if (proxyEnabled && fixieHost != null && !fixieHost.trim().isEmpty()) {
            String[] hostPort = fixieHost.split(":");
            String proxyHostOnly = hostPort[0];
            String proxyPortOnly = hostPort.length > 1 ? hostPort[1] : "1080";
            
            // Usar parámetros específicos de MariaDB para proxy SOCKS
            StringBuilder urlBuilder = new StringBuilder(baseUrl);
            
            // Agregar separador correcto
            if (baseUrl.contains("?")) {
                urlBuilder.append("&");
            } else {
                urlBuilder.append("?");
            }
            
            // Parámetros específicos para MariaDB con proxy SOCKS
            urlBuilder.append("useSSL=false")
                     .append("&allowPublicKeyRetrieval=true")
                     .append("&useCompression=false")
                     .append("&socketFactory=").append(FixieSocketFactory.class.getName())
                     .append("&socksProxyHost=").append(proxyHostOnly)
                     .append("&socksProxyPort=").append(proxyPortOnly);
            
            if (fixieUsername != null && !fixieUsername.trim().isEmpty()) {
                urlBuilder.append("&socksProxyUsername=").append(fixieUsername);
            }
            
            System.out.println("🌐 URL con proxy configurada: " + urlBuilder.toString());
            return urlBuilder.toString();
        }
        
        return baseUrl;
    }

    // Remover el método configureFixieProxy() ya que ahora usamos parámetros URL

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