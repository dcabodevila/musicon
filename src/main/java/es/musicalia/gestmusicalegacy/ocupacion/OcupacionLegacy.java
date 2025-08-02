package es.musicalia.gestmusicalegacy.ocupacion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gw_netmanager_ocupaciones")
@Getter
@Setter
public class OcupacionLegacy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ocupacion")
    private Integer idOcupacion;

    @Column(name = "id_artista", nullable = false)
    private Integer idArtista;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "id_provincia")
    private Integer idProvincia;

    @Column(name = "observaciones", columnDefinition = "LONGTEXT")
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoOcupacion estado;

    @Column(name = "ayuntamiento")
    private String ayuntamiento;

    @Column(name = "lugar")
    private String lugar;

    @Column(name = "poblacion")
    private String poblacion;

    @Column(name = "pais")
    private String pais;

    @Column(name = "mt", nullable = false, columnDefinition = "TINYINT(2) DEFAULT 0")
    private Boolean mt = false;

    @Column(name = "smt", nullable = false, columnDefinition = "TINYINT(2) DEFAULT 0")
    private Boolean smt = false;

    @Column(name = "sf", nullable = false, columnDefinition = "TINYINT(2) DEFAULT 0")
    private Boolean sf = false;

    @Column(name = "fecha_modificacion", nullable = false)
    private LocalDateTime fechaModificacion;

    // Enum para el estado
    public enum EstadoOcupacion {
        ocupado, reservado, otro
    }
}