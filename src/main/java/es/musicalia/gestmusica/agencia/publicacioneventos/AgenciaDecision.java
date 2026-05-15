package es.musicalia.gestmusica.agencia.publicacioneventos;

import es.musicalia.gestmusica.agencia.Agencia;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "agencia_decision", schema = "gestmusica",
        uniqueConstraints = @UniqueConstraint(columnNames = {"agencia_id", "codigo_decision"}))
@Getter
@Setter
@NoArgsConstructor
public class AgenciaDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agencia_id", nullable = false)
    private Agencia agencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "codigo_decision", nullable = false, length = 50)
    private AgenciaDecisionCodigo codigoDecision;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private AgenciaPublicacionEventosEstado estado;

    @Column(name = "fecha_activacion")
    private LocalDateTime fechaActivacion;

    @Column(name = "fecha_rechazo")
    private LocalDateTime fechaRechazo;
}
