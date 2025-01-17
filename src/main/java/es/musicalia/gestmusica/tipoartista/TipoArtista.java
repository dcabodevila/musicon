package es.musicalia.gestmusica.tipoartista;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tipo_artista", schema="gestmusica")
@Getter
@Setter
public class TipoArtista {
    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "NOMBRE")
    private String nombre;

}
