package es.musicalia.gestmusica.ajustes;

import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.contacto.Contacto;
import es.musicalia.gestmusica.localizacion.Ccaa;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.permiso.Permiso;
import es.musicalia.gestmusica.tipoartista.TipoArtista;
import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.Set;

@Entity
@Table(name = "ajustes", schema="gestmusica")
@Getter
@Setter
public class Ajustes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            schema = "gestmusica",
            name = "ajustes_tipo_artista",
            joinColumns = @JoinColumn(name = "ajuste_id"),
            inverseJoinColumns = @JoinColumn(name = "tipo_artista_id"))
    private Set<TipoArtista> tipoArtistas;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            schema = "gestmusica",
            name = "ajustes_agencias",
            joinColumns = @JoinColumn(name = "ajuste_id"),
            inverseJoinColumns = @JoinColumn(name = "agencia_id"))
    private Set<Agencia> agencias;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            schema = "gestmusica",
            name = "ajustes_comunidades",
            joinColumns = @JoinColumn(name = "ajuste_id"),
            inverseJoinColumns = @JoinColumn(name = "ccaa_id"))
    private Set<Ccaa> ccaa;

}
