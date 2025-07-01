package es.musicalia.gestmusica.incremento;

import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.localizacion.Provincia;
import jakarta.persistence.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
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

}
