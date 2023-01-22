package es.musicalia.gestmusica.auth.model;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class RegistrationForm {



	@NotNull
	@NotEmpty
	private String username;
	@NotNull
	@NotEmpty
	private String nombre;

	@NotEmpty
	private String apellidos;
	private String apodo;
	@NotNull
	@NotEmpty
	private String password;

	@NotNull
	@NotEmpty
	private String retryPassword;
	@NotNull
	@NotEmpty
	@Email
	private String email;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
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

	public String getRetryPassword() {
		return retryPassword;
	}

	public void setRetryPassword(String retryPassword) {
		this.retryPassword = retryPassword;
	}
}
