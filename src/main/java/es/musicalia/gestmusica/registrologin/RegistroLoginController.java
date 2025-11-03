
package es.musicalia.gestmusica.registrologin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controlador para la gestión de registros de login
 * @author sir
 */
@RestController
@RequestMapping("/registro-login")
public class RegistroLoginController {
	
	@Autowired
	private RegistroLoginService registroLoginService;
	
	/**
	 * Registra un nuevo login del usuario
	 */
	@PostMapping("/registrar/{usuarioId}")
	public ResponseEntity<RegistroLogin> registrarLogin(
			@PathVariable Long usuarioId) {
		try {
			RegistroLogin registro = registroLoginService.registrarLogin(usuarioId);
			return ResponseEntity.status(HttpStatus.CREATED).body(registro);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}
	
	/**
	 * Obtiene indicadores de actividad de un usuario
	 */
	@GetMapping("/indicador-actividad/{usuarioId}")
	public ResponseEntity<IndicadorActividadDTO> obtenerIndicadorActividad(@PathVariable Long usuarioId) {
		try {
			IndicadorActividadDTO indicador = registroLoginService.obtenerIndicadorActividad(usuarioId);
			return ResponseEntity.ok(indicador);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}
	
	/**
	 * Obtiene el histórico de logins de un usuario
	 */
	@GetMapping("/historico/{usuarioId}")
	public ResponseEntity<List<RegistroLogin>> obtenerHistoricoLogins(@PathVariable Long usuarioId) {
		try {
			List<RegistroLogin> historico = registroLoginService.obtenerHistoricoLogins(usuarioId);
			return ResponseEntity.ok(historico);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}
	
	/**
	 * Obtiene los logins de un usuario en un período específico
	 */
	@GetMapping("/periodo/{usuarioId}")
	public ResponseEntity<List<RegistroLogin>> obtenerLoginsPorPeriodo(
			@PathVariable Long usuarioId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
		try {
			List<RegistroLogin> logins = registroLoginService.obtenerLoginsPorPeriodo(
					usuarioId,
					Timestamp.valueOf(fechaInicio),
					Timestamp.valueOf(fechaFin)
			);
			return ResponseEntity.ok(logins);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}
	
	/**
	 * Cuenta los logins de un usuario en un período específico
	 */
	@GetMapping("/contar/{usuarioId}")
	public ResponseEntity<Long> contarLoginsEnPeriodo(
			@PathVariable Long usuarioId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
		try {
			Long cantidad = registroLoginService.contarLoginsEnPeriodo(
					usuarioId,
					Timestamp.valueOf(fechaInicio),
					Timestamp.valueOf(fechaFin)
			);
			return ResponseEntity.ok(cantidad);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}
	
	/**
	 * Obtiene el último login de un usuario
	 */
	@GetMapping("/ultimo/{usuarioId}")
	public ResponseEntity<RegistroLogin> obtenerUltimoLogin(@PathVariable Long usuarioId) {
		try {
			RegistroLogin ultimoLogin = registroLoginService.obtenerUltimoLogin(usuarioId);
			if (ultimoLogin == null) {
				return ResponseEntity.noContent().build();
			}
			return ResponseEntity.ok(ultimoLogin);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}
	
	/**
	 * Elimina los registros de login de un usuario
	 */
	@DeleteMapping("/{usuarioId}")
	public ResponseEntity<Void> eliminarLoginsPorUsuario(@PathVariable Long usuarioId) {
		try {
			registroLoginService.eliminarLoginsPorUsuario(usuarioId);
			return ResponseEntity.noContent().build();
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}




	/**
	 * Obtiene datos de accesos por día para el gráfico.
	 * Si usuarioId es null, obtiene accesos de todos los usuarios.
	 */
	@GetMapping("/chart-data")
	public ResponseEntity<?> obtenerDatosGrafico(
			@RequestParam(required = false) Long usuarioId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
		try {
			List<AccesoPorDiaDTO> chartData = registroLoginService.obtenerAccesosPorDia(
					usuarioId,
					Timestamp.valueOf(fechaInicio),
					Timestamp.valueOf(fechaFin)
			);
			return ResponseEntity.ok(Map.of(
					"success", true,
					"chartData", chartData
			));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("success", false, "error", e.getMessage()));
		}
	}
}
