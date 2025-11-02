package es.musicalia.gestmusica.registrologin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

/**
 * Repository para la entidad RegistroLogin
 * @author sir
 */
@Repository
public interface RegistroLoginRepository extends JpaRepository<RegistroLogin, Long> {
	
	/**
	 * Obtiene todos los registros de login de un usuario ordenados por fecha descendente
	 */
	List<RegistroLogin> findByUsuarioIdOrderByFechaLoginDesc(Long usuarioId);
	
	/**
	 * Obtiene los registros de login de un usuario en un período específico
	 */
	@Query("SELECT rl FROM RegistroLogin rl WHERE rl.usuario.id = :usuarioId " +
	       "AND rl.fechaLogin >= :fechaInicio AND rl.fechaLogin <= :fechaFin " +
	       "ORDER BY rl.fechaLogin DESC")
	List<RegistroLogin> findLoginsPorPeriodo(@Param("usuarioId") Long usuarioId,
	                                          @Param("fechaInicio") Timestamp fechaInicio,
	                                          @Param("fechaFin") Timestamp fechaFin);
	
	/**
	 * Cuenta los logins de un usuario en un período específico
	 */
	@Query("SELECT COUNT(rl) FROM RegistroLogin rl WHERE rl.usuario.id = :usuarioId " +
	       "AND rl.fechaLogin >= :fechaInicio AND rl.fechaLogin <= :fechaFin")
	Long contarLoginsEnPeriodo(@Param("usuarioId") Long usuarioId,
	                            @Param("fechaInicio") Timestamp fechaInicio,
	                            @Param("fechaFin") Timestamp fechaFin);
	
	/**
	 * Obtiene el último login de un usuario
	 */
	@Query("SELECT rl FROM RegistroLogin rl WHERE rl.usuario.id = :usuarioId " +
	       "ORDER BY rl.fechaLogin DESC LIMIT 1")
	RegistroLogin findUltimoLoginPorUsuario(@Param("usuarioId") Long usuarioId);


    /**
     * Obtiene los registros de login de TODOS los usuarios en un período específico
     */
    @Query("SELECT rl FROM RegistroLogin rl WHERE rl.fechaLogin >= :fechaInicio AND rl.fechaLogin <= :fechaFin " +
            "ORDER BY rl.fechaLogin DESC")
    List<RegistroLogin> findLoginsPorPeriodoTodosUsuarios(@Param("fechaInicio") Timestamp fechaInicio,
                                                          @Param("fechaFin") Timestamp fechaFin);
	
}
