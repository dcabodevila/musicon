package es.musicalia.gestmusica.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsEndpointAutoConfiguration;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.support.ResourcePropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorAccessTest {

    @Test
    void soloHealthDebeEstarDisponible() throws Exception {
        var properties = new ResourcePropertySource("classpath:application.properties");

        new ApplicationContextRunner()
                .withInitializer(context -> {
                    context.getEnvironment().setConversionService(new ApplicationConversionService());
                    context.getEnvironment().getPropertySources().addLast(properties);
                })
                .withConfiguration(AutoConfigurations.of(
                        EndpointAutoConfiguration.class,
                        HealthEndpointAutoConfiguration.class,
                        InfoEndpointAutoConfiguration.class,
                        MetricsEndpointAutoConfiguration.class))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(HealthEndpoint.class);
                    assertThat(context).doesNotHaveBean(InfoEndpoint.class);
                    assertThat(context).doesNotHaveBean(MetricsEndpoint.class);
                });
    }
}
