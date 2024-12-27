package es.musicalia.gestmusica.localizacion;

import jakarta.persistence.*;
import jdk.jfr.DataAmount;

@Entity
@Table(name = "municipio", schema="gestmusica")

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

    public long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
