package es.musicsoft.musicon.usuario;

import java.sql.Timestamp;

import es.musicsoft.musicon.rol.Rol;
import jakarta.persistence.*;

/**
 *
 * @author sir
 */
@Entity
@Table(name = "usuario", schema="musicon")

public class Usuario {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(name = "USERNAME")
	private String username;
	@Column(name = "NOMBRE")
	private String nombre;
	@Column(name = "APELLIDOS")
	private String apellidos;
	@Column(name = "APODO")
	private String apodo;
	@Column(name = "PASS")
	private String password;
	@Column(name = "EMAIL")

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
	private Rol rol;

	public Usuario() {
		// TODO Auto-generated constructor stub
	}

	public Usuario(String nombre, String password) {
		this.nombre = nombre;
		this.password = password;
//		this.activateKey = "";
	}

	public long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getApellidos() {
		return apellidos;
	}

	public void setApellidos(String apellidos) {
		this.apellidos = apellidos;
	}

	public String getApodo() {
		return apodo;
	}

	public void setApodo(String apodo) {
		this.apodo = apodo;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getActivateKey() {
		return activateKey;
	}

	public void setActivateKey(String activateKey) {
		this.activateKey = activateKey;
	}

	public Timestamp getFechaRegistro() {
		return fechaRegistro;
	}

	public void setFechaRegistro(Timestamp fechaRegistro) {
		this.fechaRegistro = fechaRegistro;
	}

	public Timestamp getFechaUltimoAcceso() {
		return fechaUltimoAcceso;
	}

	public void setFechaUltimoAcceso(Timestamp fechaUltimoAcceso) {
		this.fechaUltimoAcceso = fechaUltimoAcceso;
	}

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public boolean equals(Object o) {
		Usuario usuario = (Usuario) o;
		return (this.id == usuario.id);
	}

	public String getRecover() {
		return recover;
	}

	public void setRecover(String recover) {
		this.recover = recover;
	}

	public Rol getRol() {
		return rol;
	}

	public void setRol(Rol rol) {
		this.rol = rol;
	}

}
