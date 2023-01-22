package es.musicalia.gestmusica.auth.model;

import java.io.Serializable;

public class RememberForm implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3599486650172460059L;
	private String email;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

}
