package es.musicalia.gestmusica.ocupacion;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ocupacion_estado", schema="gestmusica")
@Getter
@Setter
public class OcupacionEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "nombre")
    private String nombre;

}

