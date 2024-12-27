package es.musicalia.gestmusica.acceso;

import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.rol.Rol;
import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.persistence.*;

import java.util.Set;

/**
 *
 * @author sir
 */
@Entity
@Table(name = "acceso", schema="gestmusica")

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

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Agencia getAgencia() {
		return agencia;
	}

	public void setAgencia(Agencia agencia) {
		this.agencia = agencia;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public Rol getRol() {
		return rol;
	}

	public void setRol(Rol rol) {
		this.rol = rol;
	}
}
