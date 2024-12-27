package es.musicalia.gestmusica.ocupacion;

public class OcupacionExisteException extends RuntimeException{
    // Constructor sin argumentos
    public OcupacionExisteException() {
        super();
    }

    // Constructor que recibe un mensaje
    public OcupacionExisteException(String message) {
        super(message);
    }

    // Constructor que recibe un mensaje y una causa
    public OcupacionExisteException(String message, Throwable cause) {
        super(message, cause);
    }

    // Constructor que recibe una causa
    public OcupacionExisteException(Throwable cause) {
        super(cause);
    }

    // Constructor con opciones avanzadas (opcional)
    protected OcupacionExisteException(String message, Throwable cause,
                                 boolean enableSuppression,
                                 boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
