package es.musicalia.gestmusica.acceso;

import es.musicalia.gestmusica.agencia.Agencia;
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
@Table(name = "acceso", schema="gestmusica")
@Getter
@Setter
public class Acceso {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "agencia_id")
	private Agencia agencia;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id")
	private Usuario usuario;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "rol_id")
	private Rol rol;
	@Column(name = "activo")
	private Boolean activo;

}
