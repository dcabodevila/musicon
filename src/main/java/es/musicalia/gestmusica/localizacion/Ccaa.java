package es.musicalia.gestmusica.localizacion;

import jakarta.persistence.*;

@Entity
@Table(name = "ccaa", schema="gestmusica")
public class Ccaa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "NOMBRE")
    private String nombre;

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
}
