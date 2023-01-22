package es.musicalia.gestmusica.usuario;

public record UsuarioRecord (long id, String nombreApellidos){
    public UsuarioRecord(long id, String nombreApellidos){
        this.id = id;
        this.nombreApellidos = nombreApellidos;
    }
}
