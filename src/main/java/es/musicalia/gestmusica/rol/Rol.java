package es.musicalia.gestmusica.rol;

import es.musicalia.gestmusica.permiso.Permiso;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 *
 * @author sir
 */
@Entity
@Table(name = "rol", schema="gestmusica")
@Getter
@Setter
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

	@Column(name = "TIPO_ROL")
	private Integer tipoRol;
	@Column(name= "CODIGO")
	private String codigo;
}
