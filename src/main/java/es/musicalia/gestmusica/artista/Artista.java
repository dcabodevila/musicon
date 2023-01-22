package es.musicalia.gestmusica.artista;

import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.contacto.Contacto;
import es.musicalia.gestmusica.localizacion.Ccaa;
import es.musicalia.gestmusica.tipoartista.TipoArtista;
import es.musicalia.gestmusica.tipoescenario.TipoEscenario;
import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.persistence.*;

@Entity
@Table(name = "artista", schema="gestmusica")
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

    public Ccaa getCcaa() {
        return ccaa;
    }

    public void setCcaa(Ccaa ccaa) {
        this.ccaa = ccaa;
    }

    public int getComponentes() {
        return componentes;
    }

    public void setComponentes(int componentes) {
        this.componentes = componentes;
    }

    public int getViento() {
        return viento;
    }

    public void setViento(int viento) {
        this.viento = viento;
    }

    public String getCif() {
        return cif;
    }

    public void setCif(String cif) {
        this.cif = cif;
    }

    public int getBailarinas() {
        return bailarinas;
    }

    public void setBailarinas(int bailarinas) {
        this.bailarinas = bailarinas;
    }

    public Contacto getContacto() {
        return contacto;
    }

    public void setContacto(Contacto contacto) {
        this.contacto = contacto;
    }

    public boolean isEscenario() {
        return escenario;
    }

    public void setEscenario(boolean escenario) {
        this.escenario = escenario;
    }

    public String getMedidasEscenario() {
        return medidasEscenario;
    }

    public void setMedidasEscenario(String medidasEscenario) {
        this.medidasEscenario = medidasEscenario;
    }

    public int getRitmo() {
        return ritmo;
    }

    public void setRitmo(int ritmo) {
        this.ritmo = ritmo;
    }

    public int getSolistas() {
        return solistas;
    }

    public void setSolistas(int solistas) {
        this.solistas = solistas;
    }

    public int getLuz() {
        return luz;
    }

    public void setLuz(int luz) {
        this.luz = luz;
    }

    public int getSonido() {
        return sonido;
    }

    public void setSonido(int sonido) {
        this.sonido = sonido;
    }

    public TipoEscenario getTipoEscenario() {
        return tipoEscenario;
    }

    public void setTipoEscenario(TipoEscenario tipoEscenario) {
        this.tipoEscenario = tipoEscenario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public TipoArtista getTipoArtista() {
        return tipoArtista;
    }

    public void setTipoArtista(TipoArtista tipoArtista) {
        this.tipoArtista = tipoArtista;
    }

    public Agencia getAgencia() {
        return agencia;
    }

    public void setAgencia(Agencia agencia) {
        this.agencia = agencia;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
