package es.musicalia.gestmusica.cloudinary;

public class CloudinaryException extends RuntimeException{
    // Constructor sin argumentos
    public CloudinaryException() {
        super();
    }

    // Constructor que recibe un mensaje
    public CloudinaryException(String message) {
        super(message);
    }

    // Constructor que recibe un mensaje y una causa
    public CloudinaryException(String message, Throwable cause) {
        super(message, cause);
    }

    // Constructor que recibe una causa
    public CloudinaryException(Throwable cause) {
        super(cause);
    }

    // Constructor con opciones avanzadas (opcional)
    protected CloudinaryException(String message, Throwable cause,
    boolean enableSuppression,
    boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
