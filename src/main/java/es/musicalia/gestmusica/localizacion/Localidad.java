package es.musicalia.gestmusica.localizacion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "localidad", schema="gestmusica")
@Getter
@Setter
public class Localidad {
    @Id
    @Column(name = "id")
    private long id;
    
    @Column(name = "nombre", length = 50, nullable = false)
    private String nombre;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_municipio")
    private Municipio municipio;
}