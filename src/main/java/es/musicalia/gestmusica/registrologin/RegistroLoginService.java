package es.musicalia.gestmusica.registrologin;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * Servicio para la gestión de registros de login
 * @author sir
 */
public interface RegistroLoginService {
	
	/**
	 * Registra un nuevo login del usuario
	 */
	RegistroLogin registrarLogin(Long usuarioId);


    /**
	 * Obtiene indicadores de actividad de un usuario
	 */
	IndicadorActividadDTO obtenerIndicadorActividad(Long usuarioId);
	
	/**
	 * Obtiene el histórico de logins de un usuario
	 */
	List<RegistroLogin> obtenerHistoricoLogins(Long usuarioId);
	
	/**
	 * Obtiene los logins de un usuario en un período específico
	 */
	List<RegistroLogin> obtenerLoginsPorPeriodo(Long usuarioId, Timestamp fechaInicio, Timestamp fechaFin);
	
	/**
	 * Cuenta los logins de un usuario en un período específico
	 */
	Long contarLoginsEnPeriodo(Long usuarioId, Timestamp fechaInicio, Timestamp fechaFin);
	
	/**
	 * Obtiene el último login de un usuario
	 */
	RegistroLogin obtenerUltimoLogin(Long usuarioId);
	
	/**
	 * Elimina los registros de login de un usuario
	 */
	void eliminarLoginsPorUsuario(Long usuarioId);

    List<AccesoPorDiaDTO> obtenerAccesosPorDia(Long usuarioId, Timestamp fechaInicio, Timestamp fechaFin);

}
