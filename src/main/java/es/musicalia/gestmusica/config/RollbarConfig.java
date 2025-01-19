package es.musicalia.gestmusica.config;


import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.Config;
import com.rollbar.spring.webmvc.RollbarSpringConfigBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration()
@EnableWebMvc
@ComponentScan({
        // ADD YOUR PROJECT PACKAGE HERE
        "es.musicalia.gestmusica"
})
public class RollbarConfig {

    /**
     * Register a Rollbar bean to configure App with Rollbar.
     */
    @Bean
    public Rollbar rollbar() {
        return new Rollbar(getRollbarConfigs("9144bb0cd1204ab9bbced63423505364"));
    }

    private Config getRollbarConfigs(String accessToken) {

        // Reference ConfigBuilder.java for all the properties you can set for Rollbar
        return RollbarSpringConfigBuilder.withAccessToken(accessToken)
                .environment("development")
                .codeVersion("1.0.0")
                .build();
    }
}