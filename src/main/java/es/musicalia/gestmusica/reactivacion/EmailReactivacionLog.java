package es.musicalia.gestmusica.reactivacion;

import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "email_reactivacion_log", schema = "gestmusica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailReactivacionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "segmento", nullable = false, length = 20)
    private SegmentoReactivacion segmento;

    @Column(name = "fecha_envio", nullable = false)
    private OffsetDateTime fechaEnvio;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "template", length = 100)
    private String template;
}
