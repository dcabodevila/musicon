package es.musicalia.gestmusica.comunicacion;

/**
 * Abstracción para canales de comunicación (email, WhatsApp, etc.)
 * Permite extender el sistema con nuevos canales sin modificar el código existente.
 */
public interface ComunicacionChannel {

    /**
     * Envía una comunicación al destinatario.
     *
     * @param destinatario el destino (email, teléfono, etc.)
     * @param asunto el asunto/título del mensaje
     * @param contenidoHtml el contenido en formato HTML
     * @throws Exception si ocurre un error durante el envío
     */
    void enviar(String destinatario, String asunto, String contenidoHtml) throws Exception;
}
