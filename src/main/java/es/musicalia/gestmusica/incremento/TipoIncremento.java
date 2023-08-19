package es.musicalia.gestmusica.incremento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_incremento", schema="gestmusica")
public class TipoIncremento {
    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "NOMBRE")
    private String nombre;
    public TipoIncremento() {
    }
    public TipoIncremento(long id) {
        this.id = id;
    }

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
