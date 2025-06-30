package es.musicalia.gestmusica.auth.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Getter
@Setter
public class ResetPasswordForm implements Serializable {

	private static final long serialVersionUID = 1L;

	private String email;
	private String codigo;

	@NotNull
	@NotEmpty
	@Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
	private String newPassword;

	@NotNull
	@NotEmpty
	private String confirmPassword;


}