package es.musicalia.gestmusica.accesoartista;

import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.permiso.Permiso;
import es.musicalia.gestmusica.rol.Rol;
import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author sir
 */
@Entity
@Table(name = "acceso_artista", schema="gestmusica")
@Getter
@Setter
public class AccesoArtista {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "artista_id")
	private Artista artista;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id")
	private Usuario usuario;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "permiso_id")
	private Permiso permiso;
	@Column(name = "activo")
	private Boolean activo;

}
