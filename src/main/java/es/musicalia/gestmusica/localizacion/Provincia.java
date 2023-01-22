package es.musicalia.gestmusica.localizacion;

import jakarta.persistence.*;

@Entity
@Table(name = "provincia", schema="gestmusica")
public class Provincia {
    @Id
    @Column(name = "id")
    private long id;
    @Column(name = "nombre")
    private String nombre;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ccaa")
    private Ccaa ccaa;

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
}
