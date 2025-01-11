package es.musicalia.gestmusica.agencia;

import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.contacto.Contacto;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotNull;
import java.util.Set;

@Entity
@Table(name = "agencia", schema="gestmusica")
@Getter
@Setter
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

}
