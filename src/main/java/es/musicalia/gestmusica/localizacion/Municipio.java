package es.musicalia.gestmusica.localizacion;

import jakarta.persistence.*;
import jdk.jfr.DataAmount;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "municipio", schema="gestmusica")
@Getter
@Setter
public class Municipio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name = "nombre")
    private String nombre;

    @Column(name = "cod_municipio")
    private Integer codMunicipio;
    @Column(name = "dc")
    private Integer dc;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_provincia")
    private Provincia provincia;

    @Column(name = "latitud", precision = 10, scale = 8)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 11, scale = 8)
    private BigDecimal longitud;

}
