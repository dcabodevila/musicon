package es.musicalia.gestmusica.observabilidad;

public final class FunctionalEventNames {

    private FunctionalEventNames() {
    }

    public static final String AUTH_LOGIN = "auth.login";
    public static final String AUTH_LOGOUT = "auth.logout";
    public static final String AUTH_REGISTRATION_SUBMITTED = "auth.registration_submitted";

    public static final String AGENCIA_CREATED = "agencia.created";

    public static final String ARTISTA_CREATED = "artista.created";
    public static final String ARTISTA_UPDATED = "artista.updated";

    public static final String OCUPACION_CREATED = "ocupacion.created";
    public static final String OCUPACION_CONFIRMED = "ocupacion.confirmed";
    public static final String OCUPACION_CANCELLED = "ocupacion.cancelled";

    public static final String LISTADO_GENERATED = "listado.generated";
}
