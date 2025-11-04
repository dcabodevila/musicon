package es.musicalia.gestmusica.listado;

import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.ocupacion.TipoOcupacion;
import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "listado", schema = "gestmusica")
@Getter
@Setter
public class Listado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitado_para")
    @NotNull
    private String solicitadoPara;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @NotNull
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "municipio_id")
    private Municipio municipio;

    @Column(name = "localidad")
    private String localidad;

    @Column(name = "comentario")
    private String comentario;

    @Column(name = "tipo_ocupacion")
    @Enumerated(EnumType.STRING)
    private TipoOcupacionEnum tipoOcupacion;

    @Column(name = "fecha_desde")
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta")
    private LocalDate fechaHasta;

    @Column(name = "fecha1")
    private LocalDate fecha1;

    @Column(name = "fecha2")
    private LocalDate fecha2;

    @Column(name = "fecha3")
    private LocalDate fecha3;

    @Column(name = "fecha4")
    private LocalDate fecha4;

    @Column(name = "fecha5")
    private LocalDate fecha5;

    @Column(name = "fecha6")
    private LocalDate fecha6;

    @Column(name = "fecha7")
    private LocalDate fecha7;

    @Column(name = "ids_tipo_artista")
    private String idsTipoArtista;

    @Column(name = "ids_comunidades")
    private String idsComunidades;

    @Column(name = "fecha_creacion")
    @NotNull
    private LocalDateTime fechaCreacion;
    @Column(name = "activo")
    private boolean activo;

    @ManyToMany
    @JoinTable(
            name = "listado_agencia",
            schema = "gestmusica",
            joinColumns = @JoinColumn(name = "listado_id"),
            inverseJoinColumns = @JoinColumn(name = "agencia_id")
    )
    private Set<Agencia> agencias;

    @ManyToMany
    @JoinTable(
            name = "listado_artista",
            schema = "gestmusica",
            joinColumns = @JoinColumn(name = "listado_id"),
            inverseJoinColumns = @JoinColumn(name = "artista_id")
    )
    private Set<Artista> artistas;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }
}