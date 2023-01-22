package es.musicalia.gestmusica.auth.model;

import java.io.Serializable;

public class ChangePasswordForm implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3599486650172460059L;
	private String userName;
	private String pwd;

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

}
