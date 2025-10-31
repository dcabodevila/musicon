package es.musicalia.gestmusica.agencia;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgenciaDto {

    private Long id;
    @NotNull
    @NotEmpty
    private String nombre;

    private String descripcion;
    private String direccion;
    private String cif;
    private String localidad;
    private String logo;
    private Long idMunicipio;
    private Long idProvincia;

    private String nombreProvincia;
    @NotNull
    private Long idUsuario;
    private String nombreUsuario;
    private String email;
    private String web;
    private String instagram;
    private String telefono;
    private String telefono2;
    private String telefono3;
    private String facebook;
    private String youtube;
    private String tiktok;
    private String musica;
    private Boolean activo;
    private Boolean tarifasPublicas = true;

    private String codigoPostal;


    public AgenciaDto(){

        activo=true;
    }
}
