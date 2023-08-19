package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.tarifa.Tarifa;
import jakarta.persistence.*;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ocupacion", schema="gestmusica")
public class Ocupacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artista_id")
    @NotNull
    private Artista artista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provincia_id")
    @NotNull
    private Provincia provincia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "municipio_id")
    @NotNull
    private Municipio municipio;

    @Column(name = "poblacion")
    private String poblacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarifa_id")
    @NotNull
    private Tarifa tarifa;

    @Column(name = "fecha")
    @NotNull
    private LocalDateTime fecha;

    @Column(name = "importe")
    private BigDecimal importe;
    @Column(name = "activo")
    private boolean activo;

    @Column(name = "fechacreacion")
    @NotNull
    private LocalDateTime fechaCreacion;
    @Column(name = "fechaultimamodificacion")
    private LocalDateTime fechaModificacion;

    @Column(name = "usuariocreacion")
    @NotNull
    private String usuarioCreacion;

    @Column(name = "usuarioultimamodificacion")
    private String usuarioModificacion;

    @Column(name = "matinal")
    private boolean matinal;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Artista getArtista() {
        return artista;
    }

    public void setArtista(Artista artista) {
        this.artista = artista;
    }

    public Provincia getProvincia() {
        return provincia;
    }

    public void setProvincia(Provincia provincia) {
        this.provincia = provincia;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(LocalDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }

    public String getUsuarioCreacion() {
        return usuarioCreacion;
    }

    public void setUsuarioCreacion(String usuarioCreacion) {
        this.usuarioCreacion = usuarioCreacion;
    }

    public String getUsuarioModificacion() {
        return usuarioModificacion;
    }

    public void setUsuarioModificacion(String usuarioModificacion) {
        this.usuarioModificacion = usuarioModificacion;
    }

    public boolean isMatinal() {
        return matinal;
    }

    public void setMatinal(boolean matinal) {
        this.matinal = matinal;
    }
}

