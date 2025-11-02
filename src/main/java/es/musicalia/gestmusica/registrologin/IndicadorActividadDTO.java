
package es.musicalia.gestmusica.registrologin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * DTO para los indicadores de actividad de un usuario
 * @author sir
 */
@Getter
@Setter
@AllArgsConstructor
public class IndicadorActividadDTO {
	
	private Long usuarioId;
	private String username;
	private Long totalLoginsUltimosDias;
	private Long totalLoginsEsteMes;
	private Long totalLoginsEsteAno;
	private Timestamp ultimoLogin;
	private Timestamp primerLogin;
	private Double promedioLoginesPorDia;
}
