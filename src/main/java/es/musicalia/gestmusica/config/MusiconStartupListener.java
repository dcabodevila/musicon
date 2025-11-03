package es.musicalia.gestmusica.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MusiconStartupListener implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger logger = LoggerFactory.getLogger(MusiconStartupListener.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String profile = env.getProperty("spring.profiles.active", "default");

        mostrarBanner(port, contextPath, profile);
    }

    private void mostrarBanner(String port, String contextPath, String profile) {
        logger.info("");
        logger.info("* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *");
        logger.info("");
        logger.info("  ███████ ███████ ███████ ████████ ██  █████ ");
        logger.info("  ██      ██      ██         ██    ██ ██   ██");
        logger.info("  █████   █████   ███████    ██    ██ ███████");
        logger.info("  ██      ██           ██    ██    ██ ██   ██");
        logger.info("  ██      ███████ ███████    ██    ██ ██   ██");
        logger.info("");
        logger.info("* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *");
        logger.info("");
        logger.info("===========================================================");
        logger.info("  >> Servidor: http://localhost:{}{}", port, contextPath);
        logger.info("  >> Perfil: {}", profile);
        logger.info("  >> Aplicacion: FESTIA");
        logger.info("  >> Java: {}", System.getProperty("java.version"));
        logger.info("===========================================================");

        logger.info("");
    }


}