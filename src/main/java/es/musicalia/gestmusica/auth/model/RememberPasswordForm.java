package es.musicalia.gestmusica.auth.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Getter
@Setter
public class RememberPasswordForm implements Serializable {

	private static final long serialVersionUID = 1L;

	@NotNull
	@NotEmpty
	@Email
	private String email;

}