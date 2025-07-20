package es.musicalia.gestmusica.mail;

import lombok.Data;

@Data
public class MailgunResponse {
    private String id;
    private String message;
    private String status;
}

