package es.musicalia.gestmusica.usuario;

import es.musicalia.gestmusica.mail.EmailTemplateEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

	// Buscar código específico con ENUM, no String
	@Query("SELECT c FROM CodigoVerificacion c WHERE c.email = :email AND c.codigo = :codigo " +
			"AND c.tipo = :tipo AND c.activo = true AND c.usado = :usado")
	Optional<CodigoVerificacion> findByEmailAndCodigoAndTipoAndActivoTrueAndUsado(
			@Param("email") String email, @Param("codigo") String codigo,
			@Param("tipo") EmailTemplateEnum tipo, @Param("usado") boolean usado);

	// Query personalizada con ENUM
	@Query("SELECT c FROM CodigoVerificacion c WHERE c.email = :email AND c.tipo = :tipo " +
			"AND c.activo = true AND c.usado = false AND c.fechaExpiracion > :now " +
			"ORDER BY c.fechaCreacion DESC")
	Optional<CodigoVerificacion> findCodigoValidoByEmailAndTipo(
			@Param("email") String email,
			@Param("tipo") EmailTemplateEnum tipo,
			@Param("now") LocalDateTime now);

	@Modifying
	@Query("UPDATE CodigoVerificacion c SET c.activo = false WHERE c.email = :email AND c.tipo = :tipo")
	void desactivarCodigosPrevios(@Param("email") String email, @Param("tipo") String tipo);

	@Modifying
	@Query("DELETE FROM CodigoVerificacion c WHERE c.fechaExpiracion < :fecha")
	void eliminarCodigosExpirados(@Param("fecha") LocalDateTime fecha);

	// Método adicional para buscar por email y tipo sin fecha
	Optional<CodigoVerificacion> findFirstByEmailAndTipoAndActivoTrueAndUsadoFalseOrderByFechaCreacionDesc(
			String email, String tipo);

	Optional<CodigoVerificacion> findByEmailAndCodigoAndActivoTrue(String email, String codigo);

}
