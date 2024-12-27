package es.musicalia.gestmusica.rol;

public enum RolEnum {

    ROL_ADMINISTRADOR(1L),
    ROL_REPRESENTANTE(2L),
    ROL_AGENTE(3L);

    private Long id;

    RolEnum (Long id){
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
