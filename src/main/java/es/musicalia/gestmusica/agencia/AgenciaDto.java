package es.musicalia.gestmusica.agencia;

import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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
    @NotEmpty
    private Long idUsuario;
    private String nombreUsuario;
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
    private Boolean tarifasPublicas;

    private String codigoPostal;


    public AgenciaDto(){

        activo=true;
    }
}
