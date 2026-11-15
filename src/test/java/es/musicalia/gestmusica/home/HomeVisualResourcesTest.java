package es.musicalia.gestmusica.home;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeVisualResourcesTest {

    private static final Path RESOURCES = Path.of("src/main/resources");

    @Test
    void homeUsaElSimboloDeMarcaYEtiquetaLaSeccionDeCifras() throws Exception {
        String html = Files.readString(RESOURCES.resolve("templates/main.html"));

        assertTrue(html.contains("src=\"/logo/logo-simbolo.png\""), "el hero debe usar el símbolo sin letras");
        assertTrue(html.contains("aria-label=\"festia en cifras\""), "la sección de cifras necesita nombre accesible");
    }

    @Test
    void homeNoReferenciaSelectoresRetirados() throws Exception {
        String recursos = Files.readString(RESOURCES.resolve("templates/main.html"))
                + Files.readString(RESOURCES.resolve("static/css/main.css"))
                + Files.readString(RESOURCES.resolve("static/js/main.js"));

        for (String selector : new String[]{
                "home-hero__link", "js-gsap-hero-link", "home-hero__stats",
                "js-gsap-section-header", "section-header__badge", "home-figures-title"}) {
            assertFalse(recursos.contains(selector), "selector retirado aún referenciado: " + selector);
        }
    }

    @Test
    void simboloDeMarcaEsCuadradoYTransparente() throws Exception {
        BufferedImage image = ImageIO.read(RESOURCES.resolve("static/logo/logo-simbolo.png").toFile());

        assertEquals(512, image.getWidth());
        assertEquals(512, image.getHeight());
        assertTrue(image.getColorModel().hasAlpha());
        for (int x : new int[]{0, 511}) {
            for (int y : new int[]{0, 511}) {
                assertEquals(0, image.getRGB(x, y) >>> 24, "esquina no transparente en " + x + "," + y);
            }
        }
    }
}
