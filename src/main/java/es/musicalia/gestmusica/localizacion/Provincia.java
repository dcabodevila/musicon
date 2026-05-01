package es.musicalia.gestmusica.localizacion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "provincia", schema="gestmusica")
@Getter
@Setter
public class Provincia {
    @Id
    @Column(name = "id")
    private long id;
    @Column(name = "nombre")
    private String nombre;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ccaa")
    private Ccaa ccaa;

    @Column(name = "abreviatura")
    private String abreviatura;

    @Column(name = "nombre_orquestasdegalicia")
    private String nombreOrquestasdegalicia;
    @Column(name = "ID_PROVINCIA_LEGACY")
    private Integer idProvinciaLegacy;

    @Column(name = "latitud_capital", precision = 10, scale = 8)
    private BigDecimal latitudCapital;

    @Column(name = "longitud_capital", precision = 11, scale = 8)
    private BigDecimal longitudCapital;
}
