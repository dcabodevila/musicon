package es.musicalia.gestmusica.releasenotes;

import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad para trackear qué usuarios han leído qué versiones de release notes
 */
@Entity
@Table(name = "release_notes_read", schema = "gestmusica",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "version"}))
@Getter
@Setter
@NoArgsConstructor
public class ReleaseNotesRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "fecha_lectura", nullable = false)
    private LocalDateTime fechaLectura;

    public ReleaseNotesRead(Usuario usuario, String version) {
        this.usuario = usuario;
        this.version = version;
        this.fechaLectura = LocalDateTime.now();
    }
}
