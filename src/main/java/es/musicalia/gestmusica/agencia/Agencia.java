package es.musicalia.gestmusica.agencia;

import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.contacto.Contacto;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.persistence.*;

import javax.validation.constraints.NotNull;
import java.util.Set;

@Entity
@Table(name = "agencia", schema="gestmusica")
public class Agencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name = "nombre")
    @NotNull
    private String nombre;

    @Column(name = "descripcion")
    @NotNull
    private String descripcion;
    @Column(name = "direccion")
    private String direccion;
    @Column(name = "cif")
    private String cif;
    @Column(name = "localidad")
    private String localidad;
    @Column(name = "logo")
    private String logo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_municipio")
    private Municipio municipio;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_provincia")
    private Provincia provincia;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "activo")
    private Boolean activo;

    @Column(name = "tarifas_publicas")
    private Boolean tarifasPublicas;

    @OneToOne
    @JoinColumn(name = "agencia_contacto_id", referencedColumnName = "id")
    private Contacto agenciaContacto;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "agencia")
    private Set<Artista> listaArtistas;



    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCif() {
        return cif;
    }

    public void setCif(String cif) {
        this.cif = cif;
    }

    public String getLocalidad() {
        return localidad;
    }

    public void setLocalidad(String localidad) {
        this.localidad = localidad;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public Municipio getMunicipio() {
        return municipio;
    }

    public void setMunicipio(Municipio municipio) {
        this.municipio = municipio;
    }

    public Provincia getProvincia() {
        return provincia;
    }

    public void setProvincia(Provincia provincia) {
        this.provincia = provincia;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Boolean getTarifasPublicas() {
        return tarifasPublicas;
    }

    public void setTarifasPublicas(Boolean tarifasPublicas) {
        this.tarifasPublicas = tarifasPublicas;
    }

    public Contacto getAgenciaContacto() {
        return agenciaContacto;
    }

    public void setAgenciaContacto(Contacto agenciaContacto) {
        this.agenciaContacto = agenciaContacto;
    }

//    public Set<Artista> getListaArtistas() {
//        return listaArtistas;
//    }
//    public void setListaArtistas(Set<Artista> listaArtistas) {
//        this.listaArtistas = listaArtistas;
//    }
}
