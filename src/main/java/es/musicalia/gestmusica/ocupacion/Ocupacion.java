package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.tarifa.Tarifa;
import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ocupacion", schema="gestmusica")
@Getter
@Setter
public class Ocupacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artista_id")
    @NotNull
    private Artista artista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provincia_id")
    @NotNull
    private Provincia provincia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "municipio_id")
    @NotNull
    private Municipio municipio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @NotNull
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_conf_id")
    private Usuario usuarioConfirmacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_ocupacion_id")
    @NotNull
    private TipoOcupacion tipoOcupacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id")
    @NotNull
    private OcupacionEstado ocupacionEstado;

    @Column(name = "poblacion")
    private String poblacion;

    @Column(name = "lugar")
    private String lugar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarifa_id")
    @NotNull
    private Tarifa tarifa;

    @Column(name = "fecha")
    @NotNull
    private LocalDateTime fecha;

    @NotNull
    @Column(name = "importe")
    private BigDecimal importe;

    @NotNull
    @Column(name = "porcentaje_repre")
    private BigDecimal porcentajeRepre;

    @NotNull
    @Column(name = "iva")
    private BigDecimal iva;


    @Column(name = "activo")
    private boolean activo;

    @Column(name = "fechacreacion")
    @NotNull
    private LocalDateTime fechaCreacion;
    @Column(name = "fechaultimamodificacion")
    private LocalDateTime fechaModificacion;

    @Column(name = "usuariocreacion")
    @NotNull
    private String usuarioCreacion;

    @Column(name = "usuarioultimamodificacion")
    private String usuarioModificacion;

    @Column(name = "matinal")
    private boolean matinal;

    @Column(name = "observaciones")
    private String observaciones;

    @Column(name = "solo_matinal")
    private boolean soloMatinal;

    @Column(name = "id_actuacion_externa")
    private Long idActuacionExterna;

    @Column(name = "provisional")
    private boolean provisional;

    @Column(name = "texto_orquestasdegalicia")
    private String textoOrquestasDeGalicia;

    @Column(name = "id_ocupacion_legacy")
    private Integer idOcupacionLegacy;

    @Column(name = "publicado_odg")
    private boolean publicadoOdg;

    @Column(name = "fecha_publicacion_odg")
    private LocalDateTime fechaPublicacionOdg;

}

