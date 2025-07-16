package es.musicalia.gestmusica.mail;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EmailTemplateEnum {
    REGISTRO("Código de verificación - Nuevo usuario en Gestmusica", "Gestmusica","Código de verificación", "Gracias por registrarte en Gestmusica. Para completar tu registro, utiliza el siguiente código:","codigo-confirmacion-email"),
    RECUPERACION_PASSWORD("Código de verificación - Recuperación de contraseña", "Gestmusica","Código de verificación", "Has solicitado restablecer tu contraseña. Utiliza el siguiente código:","codigo-confirmacion-email"),
    CAMBIO_EMAIL("Código de verificación - Cambio de email", "Gestmusica","Código de verificación", "Has solicitado cambiar tu email. Utiliza el siguiente código:","codigo-confirmacion-email"),
    EMAIL_NOTIFICACION_CONFIRMACION_PENDIENTE("Nueva ocupación de fecha", "Gestmusica", "Confirmación de fecha solicitada", "Se ha recibido una nueva ocupación de fecha, es necesario que confirmes para que se haga efectiva. Accede a Gestmusica para autorizar.","base-email"),
    EMAIL_NOTIFICACION_CONFIRMACION("Confirmación de fecha", "Gestmusica", "Fecha confirmada", "Se ha confirmado una solicitud de ocupación realizada por ti. Podrás ver la información en Gestmusica.","base-email"),
    EMAIL_NOTIFICACION_ANULACION("Anulación de fecha", "Gestmusica", "Fecha anulada", "Se ha recibido una anulación de una fecha ocupada por ti. Podrás ver la información en Gestmusica.","base-email");;;


    private final String asunto;
    private final String titulo;
    private final String subtitulo;
    private final String mensaje;
    private final String template;

}
