/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package es.musicalia.gestmusica.usuario;

import java.io.Serializable;
import java.sql.Timestamp;

public class UsuarioAdminListDTO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 536885671599834455L;
	private long id;
	private String username;
	private Timestamp fechaUltimoAcceso;
	private Timestamp fechaRegistro;
	private boolean admin;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Timestamp getFechaUltimoAcceso() {
		return fechaUltimoAcceso;
	}

	public void setFechaUltimoAcceso(Timestamp fechaUltimoAcceso) {
		this.fechaUltimoAcceso = fechaUltimoAcceso;
	}

	public Timestamp getFechaRegistro() {
		return fechaRegistro;
	}

	public void setFechaRegistro(Timestamp fechaRegistro) {
		this.fechaRegistro = fechaRegistro;
	}

	public boolean isAdmin() {
		return admin;
	}

	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

}
