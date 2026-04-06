package es.musicalia.gestmusica.reactivacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface EmailReactivacionLogRepository extends JpaRepository<EmailReactivacionLog, Long> {

    /**
     * Devuelve los usuarios que NO han recibido email de reactivación
     * en los últimos {@code diasCooldown} días, filtrando por rol general
     * (REPRE o AGENTE) y por ventana de inactividad.
     *
     * <p>La segmentación TIBIO/FRIO se calcula en Java para evitar lógica
     * de fechas duplicada en SQL. La query devuelve todos los candidatos
     * elegibles dentro del rango completo (60–365 días).
     */
    @Query("""
            SELECT u FROM Usuario u
            JOIN u.rolGeneral r
            WHERE u.activo = true
              AND u.emailVerified = true
              AND u.validado = true
              AND u.emailBaja = false
              AND u.fechaUltimoAcceso IS NOT NULL
              AND u.fechaUltimoAcceso < :limite60dias
              AND u.fechaUltimoAcceso > :limite365dias
              AND r.codigo = 'AGENTE'
              AND u.id NOT IN (
                  SELECT log.usuario.id
                  FROM EmailReactivacionLog log
                  WHERE log.fechaEnvio > :limiteCooldown
              )
            ORDER BY u.fechaUltimoAcceso ASC
            """)
    List<es.musicalia.gestmusica.usuario.Usuario> findUsuariosElegibles(
            @Param("limite60dias") OffsetDateTime limite60dias,
            @Param("limite365dias") OffsetDateTime limite365dias,
            @Param("limiteCooldown") OffsetDateTime limiteCooldown
    );
}
