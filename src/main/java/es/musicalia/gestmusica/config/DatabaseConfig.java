package es.musicalia.gestmusica.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

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

    // Configuración MariaDB (Secundaria)
    @Bean(name = "mariadbDataSource")
    @ConfigurationProperties("spring.datasource.mariadb")
    public DataSource mariadbDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "mariadbEntityManagerFactory")
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
    public PlatformTransactionManager mariadbTransactionManager(
            @Qualifier("mariadbEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    /**
     * Bean manual para OcupacionLegacyService 
     * SOLUCIÓN AL PROBLEMA: Crear el bean manualmente
     */
    @Bean("ocupacionLegacyService")
    public es.musicalia.gestmusicalegacy.ocupacion.OcupacionLegacyService ocupacionLegacyService(
            es.musicalia.gestmusicalegacy.ocupacion.OcupacionLegacyRepository repository) {
        return new es.musicalia.gestmusicalegacy.ocupacion.OcupacionLegacyServiceImpl(repository);
    }
}