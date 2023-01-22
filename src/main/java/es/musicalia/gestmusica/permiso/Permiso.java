package es.musicalia.gestmusica.permiso;

import jakarta.persistence.*;

/**
 *
 * @author sir
 */
@Entity
@Table(name = "permiso", schema="gestmusica")

public class Permiso {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	@Column(name = "CODIGO")
	private String codigo;
	@Column(name = "DESCRIPCION")
	private String descripcion;

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
}
