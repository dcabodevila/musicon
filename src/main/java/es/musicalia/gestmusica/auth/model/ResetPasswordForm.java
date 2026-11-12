package es.musicalia.gestmusica.auth.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class ResetPasswordForm implements Serializable {

	private static final long serialVersionUID = 1L;

	// Display only: the email used for the reset always comes from the session grant
	private String email;

	@NotNull
	@NotEmpty
	@Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
	private String newPassword;

	@NotNull
	@NotEmpty
	private String confirmPassword;


}