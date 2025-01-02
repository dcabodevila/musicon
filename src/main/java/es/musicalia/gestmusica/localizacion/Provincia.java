package es.musicalia.gestmusica.localizacion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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


}
