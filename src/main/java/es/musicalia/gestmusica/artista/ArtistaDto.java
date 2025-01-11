package es.musicalia.gestmusica.artista;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class ArtistaDto {

    private Long id;
    @NotNull
    @NotEmpty
    private String nombre;
    private String cif;
    private String logo;
    @NotNull
    @NotEmpty
    private Long idUsuario;

    private String nombreUsuario;
    @NotNull
    private Long idCcaa;

    private String nombreCcaa;

    private int componentes;
    private int bailarinas;
    private boolean escenario;
    private String medidasEscenario;
    private int ritmo;
    private int viento;
    private int solistas;
    private int luz;
    private int sonido;
    private Long idTipoEscenario;
    private String nombreTipoEscenario;

    @NotNull
    private Long idTipoArtista;
    @NotNull
    @NotEmpty
    private Long idAgencia;

    private String nombreAgencia;
    private String email;
    private String fax;
    private String web;
    private String instagram;
    private String telefono;
    private String telefono2;
    private String telefono3;

    private String facebook;
    private String youtube;
    private Boolean activo;

    public ArtistaDto(){
        activo=true;
    }

 }
