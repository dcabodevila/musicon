package es.musicalia.gestmusica.mail;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EmailTemplateEnum {
    REGISTRO("Código de verificación - Nuevo usuario en festia.es", "festia.es","Código de verificación", "Gracias por registrarte en festia.es. Para completar tu registro, utiliza el siguiente código:","codigo-confirmacion-email"),
    RECUPERACION_PASSWORD("Código de verificación - Recuperación de contraseña", "festia.es","Código de verificación", "Has solicitado restablecer tu contraseña. Utiliza el siguiente código:","codigo-confirmacion-email"),
    CAMBIO_EMAIL("Código de verificación - Cambio de email", "festia.es","Código de verificación", "Has solicitado cambiar tu email. Utiliza el siguiente código:","codigo-confirmacion-email"),
    EMAIL_NOTIFICACION_CONFIRMACION_PENDIENTE("Nueva ocupación de fecha", "festia.es", "Confirmación de fecha solicitada", "Se ha recibido una nueva ocupación de fecha, es necesario que confirmes para que se haga efectiva. Accede a festia.es para autorizar.","base-email"),
    EMAIL_NOTIFICACION_CONFIRMACION("Confirmación de fecha", "festia.es", "Fecha confirmada", "Se ha confirmado una solicitud de ocupación realizada por ti. Podrás ver la información en festia.es.","base-email"),
    EMAIL_NOTIFICACION_ANULACION("Anulación de fecha", "festia.es", "Fecha anulada", "Se ha recibido una anulación de una fecha ocupada por ti. Podrás ver la información en festia.es.","base-email"),
    VALIDAR_USUARIO("Nuevo usuario registrado en festia.es", "festia.es","Nuevo usuario", "Se ha registrado un nuevo usuario, entre en la sección de Usuarios para validarlo.","base-email");


    private final String asunto;
    private final String titulo;
    private final String subtitulo;
    private final String mensaje;
    private final String template;

}
