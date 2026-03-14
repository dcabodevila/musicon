package es.musicalia.gestmusica.orquestasdegalicia;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "odg_sincronizacion_tracking", schema = "gestmusica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OdgSincronizacionTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_ejecucion", nullable = false, length = 36)
    private String idEjecucion;

    @Column(name = "fecha_ejecucion", nullable = false)
    private LocalDateTime fechaEjecucion;

    @Column(name = "ocupacion_id", nullable = false)
    private Long ocupacionId;

    @Column(name = "artista_id", nullable = false)
    private Long artistaId;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime fechaEvento;

    @Column(nullable = false, length = 20)
    private String accion;

    @Column(nullable = false, length = 20)
    private String resultado;

    @Column(name = "message_type", length = 20)
    private String messageType;

    @Column(length = 1000)
    private String mensaje;
}
