package es.musicalia.gestmusica.tipoartista;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_artista", schema="gestmusica")
public class TipoArtista {
    @Id
    @Column(name = "id")
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
