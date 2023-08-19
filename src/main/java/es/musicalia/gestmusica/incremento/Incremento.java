package es.musicalia.gestmusica.incremento;

import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.localizacion.Provincia;
import jakarta.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(name = "incremento_provincial", schema="gestmusica")
public class Incremento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_incremento_id")
    @NotNull
    private TipoIncremento tipoIncremento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artista_id")
    @NotNull
    private Artista artista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provincia_id")
    @NotNull
    private Provincia provincia;

    @NotNull
    private BigDecimal incremento;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }


    public TipoIncremento getTipoIncremento() {
        return tipoIncremento;
    }

    public void setTipoIncremento(TipoIncremento tipoIncremento) {
        this.tipoIncremento = tipoIncremento;
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

    public BigDecimal getIncremento() {
        return incremento;
    }

    public void setIncremento(BigDecimal incremento) {
        this.incremento = incremento;
    }
}
