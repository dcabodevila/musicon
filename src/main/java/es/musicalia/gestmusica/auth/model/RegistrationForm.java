package es.musicalia.gestmusica.auth.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

	@NotNull
	@NotEmpty
	private String nombreComercial;

	@NotNull
	@NotEmpty
	private String telefono;

	@NotNull
	private Long idProvincia;



}
