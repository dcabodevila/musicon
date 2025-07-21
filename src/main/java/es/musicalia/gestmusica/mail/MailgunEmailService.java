package es.musicalia.gestmusica.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class MailgunEmailService {


    @Value("${mailgun.domain}")
    private String domain;
    @Value("${spring.mail.username}")
    private String fromEmail;
    @Value("${spring.mail.sender.name:Gestmusica}")
    private String senderName;

    private final RestTemplate restTemplate;

    public MailgunEmailService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public MailgunResponse sendSimpleEmail(String to, String subject, String text) {
        String mailgunUrl = String.format("https://api.eu.mailgun.net/v3/%s/messages", domain);

        MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
        request.add("from", String.format("%s <gestmusica@%s>", senderName, domain));
        request.add("to", to);
        request.add("subject", subject);
        request.add("html", text);

        return restTemplate.postForObject(mailgunUrl, request, MailgunResponse.class);
    }

}
