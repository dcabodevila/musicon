package es.musicalia.gestmusica.usuario;

import java.sql.Timestamp;
import java.util.Set;

import es.musicalia.gestmusica.acceso.Acceso;
import es.musicalia.gestmusica.rol.Rol;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 *
 * @author sir
 */
@Entity
@Table(name = "usuario", schema="gestmusica")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(name = "USERNAME")
	private String username;
	@Column(name = "NOMBRE")
	@NotEmpty
	private String nombre;
	@Column(name = "APELLIDOS")
	private String apellidos;
	@Column(name = "APODO")
	private String apodo;
	@Column(name = "PASS")
	@NotEmpty
	private String password;
	@Column(name = "EMAIL")
	@Email
	private String email;
	@Column(name = "ACTIVATE_KEY")
	private String activateKey;
	@Column(name = "FECHA_ULTIMO_ACCESO")
	private Timestamp fechaUltimoAcceso;
	@Column(name = "FECHA_REGISTRO")
	private Timestamp fechaRegistro;
	@Column(name = "ACTIVO")
	private boolean activo;
	@Column(name = "RECOVER")
	private String recover;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_rol")
	private Rol rolGeneral;
	@Column(name = "VALIDADO")
	private boolean validado;
	@OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private Set<Acceso> accesos;


	public Usuario(String nombre, String password) {
		this.nombre = nombre;
		this.password = password;
	}
	public String getNombreCompleto(){
		return this.getApellidos()!=null ? this.getNombre().concat(" ").concat(this.getApellidos()) : this.getNombre();
	}

}
