package es.musicalia.gestmusica.artista;

import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.contacto.Contacto;
import es.musicalia.gestmusica.localizacion.Ccaa;
import es.musicalia.gestmusica.tipoartista.TipoArtista;
import es.musicalia.gestmusica.tipoescenario.TipoEscenario;
import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "artista", schema="gestmusica")
@Getter
@Setter
public class Artista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "nombre")
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ccaa")
    private Ccaa ccaa;

    @Column(name = "componentes")
    private int componentes;

    @Column(name = "viento")
    private int viento;

    @Column(name = "cif")
    private String cif;

    @Column(name = "bailarinas")
    private int bailarinas;
    @OneToOne
    @JoinColumn(name = "contacto_id", referencedColumnName = "id")
    private Contacto contacto;

    @Column(name = "escenario")
    private boolean escenario;

    @Column(name = "medidas_escenario")
    private String medidasEscenario;

    @Column(name = "ritmo")
    private int ritmo;

    @Column(name = "solistas")
    private int solistas;

    @Column(name = "luz")
    private int luz;

    @Column(name = "sonido")
    private int sonido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_escenario_id")
    private TipoEscenario tipoEscenario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_artista_id")
    private TipoArtista tipoArtista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agencia_id")
    private Agencia agencia;

    @Column(name = "logo")
    private String logo;

    @Column(name = "activo")
    private boolean activo;

}
