package es.musicalia.gestmusica.info;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping(value = "info")
@RequiredArgsConstructor
public class InfoController {

    private static final String SITE_NAME = "festia.es";
    private static final String OG_IMAGE = "https://res.cloudinary.com/hseoceuyz/image/upload/v1760835633/landing-festia_epbr7a.png";
    private static final String SITE_URL = "https://festia.es";

    private final ObjectMapper objectMapper;

    @GetMapping
    public String info(Model model, HttpServletRequest request) {

        String baseUrl = construirBaseUrl(request);
        String canonicalUrl = baseUrl + "/info";

        // SEO meta
        model.addAttribute("titulo", "Festia — Gestión de Orquestas y Artistas para Fiestas");
        model.addAttribute("descripcion",
            "Plataforma profesional para agencias, músicos y representantes. Tarifas, disponibilidad y presupuestos en tiempo real. Prueba gratis 1 mes.");
        model.addAttribute("canonicalUrl", canonicalUrl);
        model.addAttribute("metaRobots", "index,follow");
        model.addAttribute("ogImage", OG_IMAGE);

        // JSON-LD: Organization + SoftwareApplication + FAQPage + BreadcrumbList
        model.addAttribute("jsonLd", buildJsonLd(baseUrl, canonicalUrl));

        return "info";
    }

    private String construirBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String baseUrl = scheme + "://" + serverName;
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            baseUrl += ":" + serverPort;
        }
        return baseUrl;
    }

    private String serializarJsonLd(Object data) {
        try {
            return objectMapper.writeValueAsString(data).replace("</", "<\\/");
        } catch (JsonProcessingException ex) {
            log.error("Error serializando JSON-LD en /info", ex);
            return "{}";
        }
    }

    private String buildJsonLd(String baseUrl, String canonicalUrl) {
        // 1. Organization
        Map<String, Object> organization = new LinkedHashMap<>();
        organization.put("@context", "https://schema.org");
        organization.put("@type", "Organization");
        organization.put("name", "festia.es");
        organization.put("url", SITE_URL);
        organization.put("logo", SITE_URL + "/logo/logo-name-transparente-alt-color.png");
        organization.put("description",
            "Plataforma profesional de gestión de orquestas, discotecas móviles y artistas para fiestas, verbenas y eventos en España.");
        organization.put("foundingDate", "1998");
        organization.put("sameAs", List.of("https://orquestasdegalicia.es"));

        Map<String, Object> contactPoint = new LinkedHashMap<>();
        contactPoint.put("@type", "ContactPoint");
        contactPoint.put("telephone", "+34-609-307-470");
        contactPoint.put("contactType", "customer service");
        contactPoint.put("email", "info@festia.es");
        contactPoint.put("availableLanguage", List.of("Spanish"));
        organization.put("contactPoint", contactPoint);

        // 2. SoftwareApplication
        Map<String, Object> softwareApp = new LinkedHashMap<>();
        softwareApp.put("@context", "https://schema.org");
        softwareApp.put("@type", "SoftwareApplication");
        softwareApp.put("name", "festia.es");
        softwareApp.put("url", SITE_URL);
        softwareApp.put("applicationCategory", "BusinessApplication");
        softwareApp.put("operatingSystem", "Web");
        softwareApp.put("description",
            "Plataforma SaaS para agencias y representantes de orquestas y artistas. Gestiona tarifas, disponibilidad, " +
            "presupuestos, ocupaciones y publicita tus eventos automáticamente.");
        softwareApp.put("offers", List.of(
            Map.of("@type", "Offer", "name", "Representantes", "price", "0", "priceCurrency", "EUR",
                "description", "Presupuestos inmediatos de todos los artistas, gratuitamente."),
            Map.of("@type", "Offer", "name", "Agencia/Artista", "price", "150", "priceCurrency", "EUR",
                "description", "150 €/artista/año. Hasta 6 usuarios, tarifas, ocupaciones, documentos, publicación en OrquestasDeGalicia.es y Festia Eventos.")));

        Map<String, Object> aggregateRating = new LinkedHashMap<>();
        aggregateRating.put("@type", "AggregateRating");
        aggregateRating.put("ratingValue", "4.8");
        aggregateRating.put("ratingCount", "25");
        softwareApp.put("aggregateRating", aggregateRating);

        // 3. FAQPage
        Map<String, Object> faqPage = new LinkedHashMap<>();
        faqPage.put("@context", "https://schema.org");
        faqPage.put("@type", "FAQPage");
        faqPage.put("mainEntity", List.of(
            Map.of(
                "@type", "Question",
                "name", "¿Hay período de prueba?",
                "acceptedAnswer", Map.of(
                    "@type", "Answer",
                    "text", "Sí, todos nuestros planes tienen un período de prueba de 1 mes. No se te cobrará nada hasta que nos confirmes que quieres continuar con nosotros."
                )
            ),
            Map.of(
                "@type", "Question",
                "name", "Somos una agencia con varios artistas, ¿hay descuentos?",
                "acceptedAnswer", Map.of(
                    "@type", "Answer",
                    "text", "Sí, en función del volumen de artistas aplicamos descuentos. Escríbenos y estudiaremos tu caso. Nuestros profesionales se pondrán en contacto contigo."
                )
            ),
            Map.of(
                "@type", "Question",
                "name", "Soy un particular, ¿puedo consultar tarifas?",
                "acceptedAnswer", Map.of(
                    "@type", "Answer",
                    "text", "Festia.es es una plataforma para profesionales del sector. De momento no está abierta a particulares, pero en un futuro es posible que podamos ofrecer cierta información que las agencias y artistas deseen compartir."
                )
            ),
            Map.of(
                "@type", "Question",
                "name", "¿Puedo cancelar mi suscripción en cualquier momento?",
                "acceptedAnswer", Map.of(
                    "@type", "Answer",
                    "text", "Sí, podrás cancelar en cualquier momento y no se renovará el pago. No se realizarán devoluciones de las cuotas ya pagadas."
                )
            )
        ));

        // 4. BreadcrumbList
        Map<String, Object> breadcrumbList = new LinkedHashMap<>();
        breadcrumbList.put("@context", "https://schema.org");
        breadcrumbList.put("@type", "BreadcrumbList");
        breadcrumbList.put("itemListElement", List.of(
            Map.of("@type", "ListItem", "position", 1, "name", "Festia", "item", SITE_URL),
            Map.of("@type", "ListItem", "position", 2, "name", "Información", "item", canonicalUrl)
        ));

        return serializarJsonLd(List.of(organization, softwareApp, faqPage, breadcrumbList));
    }
}
