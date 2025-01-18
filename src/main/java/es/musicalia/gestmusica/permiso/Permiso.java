package es.musicalia.gestmusica.permiso;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author sir
 */
@Entity
@Table(name = "permiso", schema="gestmusica")
@Getter
@Setter
public class Permiso {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	@Column(name = "CODIGO")
	private String codigo;
	@Column(name = "DESCRIPCION")
	private String descripcion;
	@Column(name = "TIPO_PERMISO")
	private Integer tipoPermiso;


}
