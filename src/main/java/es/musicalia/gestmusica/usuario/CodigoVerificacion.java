package es.musicalia.gestmusica.usuario;

import es.musicalia.gestmusica.mail.EmailTemplateEnum;
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

    @Column(name="fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name="fecha_expiracion",nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean usado = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailTemplateEnum tipo;


    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(fechaExpiracion);
    }

    public boolean isValido() {
        return activo && !usado && !isExpirado();
    }
}