package es.musicalia.gestmusica.registrologin;

import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de registros de login
 * @author sir
 */
@Service
@Transactional
public class RegistroLoginServiceImpl implements RegistroLoginService {
	
	@Autowired
	private RegistroLoginRepository registroLoginRepository;
	
	@Autowired
	private UsuarioRepository usuarioRepository;
	
	@Override
    public RegistroLogin registrarLogin(Long usuarioId) {
		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));
		
		RegistroLogin registro = new RegistroLogin(
				usuario,
				Timestamp.valueOf(LocalDateTime.now())
		);
		
		usuario.setFechaUltimoAcceso(registro.getFechaLogin());
		usuarioRepository.save(usuario);
		
		return registroLoginRepository.save(registro);
	}
	
	@Override
	@Transactional(readOnly = true)
	public IndicadorActividadDTO obtenerIndicadorActividad(Long usuarioId) {
		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));
		
		LocalDateTime ahora = LocalDateTime.now();
		LocalDateTime hace7Dias = ahora.minusDays(7);
		LocalDateTime inicioMes = ahora.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
		LocalDateTime inicioAno = ahora.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);
		
		Long loginsUltimos7Dias = registroLoginRepository.contarLoginsEnPeriodo(
				usuarioId,
				Timestamp.valueOf(hace7Dias),
				Timestamp.valueOf(ahora)
		);
		
		Long loginsEsteMes = registroLoginRepository.contarLoginsEnPeriodo(
				usuarioId,
				Timestamp.valueOf(inicioMes),
				Timestamp.valueOf(ahora)
		);
		
		Long loginsEsteAno = registroLoginRepository.contarLoginsEnPeriodo(
				usuarioId,
				Timestamp.valueOf(inicioAno),
				Timestamp.valueOf(ahora)
		);
		
		List<RegistroLogin> ultimosLogins = registroLoginRepository
				.findByUsuarioIdOrderByFechaLoginDesc(usuarioId);
		
		Timestamp ultimoLogin = ultimosLogins.isEmpty() ? null : ultimosLogins.get(0).getFechaLogin();
		Timestamp primerLogin = ultimosLogins.isEmpty() ? null : ultimosLogins.get(ultimosLogins.size() - 1).getFechaLogin();
		
		Double promedioLogins = calcularPromedioLogines(usuarioId, primerLogin, ultimoLogin);
		
		return new IndicadorActividadDTO(
				usuarioId,
				usuario.getUsername(),
				loginsUltimos7Dias,
				loginsEsteMes,
				loginsEsteAno,
				ultimoLogin,
				primerLogin,
				promedioLogins
		);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<RegistroLogin> obtenerHistoricoLogins(Long usuarioId) {
		usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));
		return registroLoginRepository.findByUsuarioIdOrderByFechaLoginDesc(usuarioId);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<RegistroLogin> obtenerLoginsPorPeriodo(Long usuarioId, Timestamp fechaInicio, Timestamp fechaFin) {
		usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));
		return registroLoginRepository.findLoginsPorPeriodo(usuarioId, fechaInicio, fechaFin);
	}
	
	@Override
	@Transactional(readOnly = true)
	public Long contarLoginsEnPeriodo(Long usuarioId, Timestamp fechaInicio, Timestamp fechaFin) {
		usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));
		return registroLoginRepository.contarLoginsEnPeriodo(usuarioId, fechaInicio, fechaFin);
	}
	
	@Override
	@Transactional(readOnly = true)
	public RegistroLogin obtenerUltimoLogin(Long usuarioId) {
		usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));
		return registroLoginRepository.findUltimoLoginPorUsuario(usuarioId);
	}
	
	@Override
	public void eliminarLoginsPorUsuario(Long usuarioId) {
		usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));
		List<RegistroLogin> logins = registroLoginRepository.findByUsuarioIdOrderByFechaLoginDesc(usuarioId);
		registroLoginRepository.deleteAll(logins);
	}
	
	/**
	 * Calcula el promedio de logins por día
	 */
	private Double calcularPromedioLogines(Long usuarioId, Timestamp primerLogin, Timestamp ultimoLogin) {
		if (primerLogin == null || ultimoLogin == null) {
			return 0.0;
		}
		
		long diasTranscurridos = (ultimoLogin.getTime() - primerLogin.getTime()) / (1000 * 60 * 60 * 24);
		if (diasTranscurridos == 0) {
			return 1.0;
		}
		
		Long totalLogins = registroLoginRepository.contarLoginsEnPeriodo(
				usuarioId,
				primerLogin,
				ultimoLogin
		);
		
		return (double) totalLogins / diasTranscurridos;
	}



	@Override
	@Transactional(readOnly = true)
	public List<AccesoPorDiaDTO> obtenerAccesosPorDia(Long usuarioId, Timestamp fechaInicio, Timestamp fechaFin) {
		List<RegistroLogin> logins;
		
		if (usuarioId == null) {
			// Obtener accesos de TODOS los usuarios
			logins = registroLoginRepository.findLoginsPorPeriodoTodosUsuarios(fechaInicio, fechaFin);
		} else {
			// Validar que el usuario existe
			usuarioRepository.findById(usuarioId)
					.orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));
			// Obtener accesos solo de ese usuario
			logins = registroLoginRepository.findLoginsPorPeriodo(usuarioId, fechaInicio, fechaFin);
		}
		
		if (logins.isEmpty()) {
			return List.of();
		}
		
		// Agrupar por día
		Map<LocalDate, Long> accesosPorDia = logins.stream()
				.collect(Collectors.groupingBy(
						rl -> rl.getFechaLogin().toLocalDateTime().toLocalDate(),
						Collectors.counting()
				));
		
		// Convertir a lista de DTOs ordenada por fecha
		java.time.format.DateTimeFormatter formatter = 
				java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
		
		return accesosPorDia.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(entry -> new AccesoPorDiaDTO(
						entry.getKey().toString(),
						entry.getKey().format(formatter),
						entry.getValue()
				))
				.collect(Collectors.toList());
	}
}
