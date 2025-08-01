package es.musicalia.gestmusica.sincronizacion;

public class SincronizacionException extends Exception {
    public SincronizacionException(String message) {
        super(message); // Pasar el mensaje al constructor padre
    }
    
    // Opcional: Constructor adicional para incluir causa
    public SincronizacionException(String message, Throwable cause) {
        super(message, cause);
    }
}