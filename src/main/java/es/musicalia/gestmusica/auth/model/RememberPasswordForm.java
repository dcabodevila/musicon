package es.musicalia.gestmusica.auth.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;


@Getter
@Setter
public class RememberPasswordForm implements Serializable {

	private static final long serialVersionUID = 1L;

	@NotNull
	@NotEmpty
	@Email
	private String email;

}