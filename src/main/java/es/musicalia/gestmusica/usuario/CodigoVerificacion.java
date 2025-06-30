package es.musicalia.gestmusica.usuario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "codigo_verificacion", schema="gestmusica")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodigoVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 4)
    private String codigo;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean usado = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVerificacion tipo;

    public enum TipoVerificacion {
        REGISTRO,
        RECUPERACION_PASSWORD,
        CAMBIO_EMAIL

    }

    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(fechaExpiracion);
    }

    public boolean isValido() {
        return activo && !usado && !isExpirado();
    }
}