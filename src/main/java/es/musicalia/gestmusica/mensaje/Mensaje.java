package es.musicalia.gestmusica.mensaje;

import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensaje", schema = "gestmusica")
@Getter
@Setter
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_remite", nullable = false)
    private Usuario usuarioRemite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_receptor", nullable = false)
    private Usuario usuarioReceptor;

    @Column(name = "asunto", nullable = false, length = 255)
    private String asunto;

    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "url_enlace", length = 500)
    private String urlEnlace;

    @Column(name = "leido", nullable = false)
    private boolean leido = false;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_leido")
    private LocalDateTime fechaLeido;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "destacado", nullable = false)
    private boolean destacado = false;

    @Column(name = "imagen", length = 255)
    private String imagen;
}