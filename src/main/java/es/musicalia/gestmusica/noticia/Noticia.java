package es.musicalia.gestmusica.noticia;

import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "noticia", schema="gestmusica")
@Getter
@Setter
public class Noticia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    
    @Column(name = "titulo", nullable = false)
    private String titulo;
    
    @Column(name = "contenido", columnDefinition = "TEXT")
    private String contenido;
    
    @Column(name = "url")
    private String url;
    
    @Column(name = "imagen")
    private String imagen;
    
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;
    
    @Column(name = "activo", nullable = false)
    private Boolean activo;
    
    @Column(name = "destacada", nullable = false)
    private Boolean destacada;

    @Column(name = "leida", nullable = false)
    private Boolean leida;
}