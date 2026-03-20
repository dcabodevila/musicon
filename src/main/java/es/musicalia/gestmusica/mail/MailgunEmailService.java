package es.musicalia.gestmusica.mail;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class MailgunEmailService {


    @Value("${mailgun.domain}")
    private String domain;
    @Value("${spring.mail.username}")
    private String fromEmail;
    @Value("${spring.mail.sender.name:festia}")
    private String senderName;

    private final RestTemplate restTemplate;

    public MailgunEmailService(@Qualifier("mailgunRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public MailgunResponse sendSimpleEmail(String to, String subject, String text) {
        return sendSimpleEmail(to, subject, text, null);
    }

    public MailgunResponse sendSimpleEmail(String to, String subject, String text, List<String> cc) {
        String mailgunUrl = String.format("https://api.eu.mailgun.net/v3/%s/messages", domain);

        MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
        request.add("from", String.format("%s <info@festia.es>", senderName));
        request.add("to", to);
        request.add("subject", subject);
        request.add("html", text);
        if (cc != null && !cc.isEmpty()) {
            request.add("cc", String.join(",", cc));
        }

        return restTemplate.postForObject(mailgunUrl, request, MailgunResponse.class);
    }

}
