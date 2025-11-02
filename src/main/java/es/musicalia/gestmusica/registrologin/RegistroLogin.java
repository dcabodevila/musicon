
package es.musicalia.gestmusica.registrologin;

import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * Entidad que registra los accesos/logins de los usuarios
 * @author sir
 */
@Entity
@Table(name = "registro_login", schema = "gestmusica", 
       indexes = @Index(name = "idx_usuario_fecha", columnList = "usuario_id, fecha_login"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistroLogin {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;
	
	@Column(name = "fecha_login", nullable = false)
	private Timestamp fechaLogin;
	

	/**
	 * Constructor con parámetros principales
	 */
	public RegistroLogin(Usuario usuario, Timestamp fechaLogin) {
		this.usuario = usuario;
		this.fechaLogin = fechaLogin;
	}
}
