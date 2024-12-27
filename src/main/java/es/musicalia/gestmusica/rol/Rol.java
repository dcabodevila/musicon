package es.musicalia.gestmusica.rol;

import es.musicalia.gestmusica.permiso.Permiso;
import jakarta.persistence.*;

import java.util.Set;

/**
 *
 * @author sir
 */
@Entity
@Table(name = "rol", schema="gestmusica")

public class Rol {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	@Column(name = "NOMBRE")
	private String nombre;
	@Column(name = "DESCRIPCION")
	private String descripcion;
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			schema = "gestmusica",
			name = "rol_permisos",
			joinColumns = @JoinColumn(name = "rol_id"),
			inverseJoinColumns = @JoinColumn(name = "permiso_id"))
	private Set<Permiso> permisos;

	public long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public Set<Permiso> getPermisos() {
		return permisos;
	}

	public void setPermisos(Set<Permiso> permisos) {
		this.permisos = permisos;
	}
}
