package es.musicalia.gestmusica.mensaje;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    @Query("""
                SELECT new es.musicalia.gestmusica.mensaje.MensajeRecord(
                    m.id, 
                    m.usuarioRemite.nombre || ' ' || m.usuarioRemite.apellidos, 
                    m.usuarioReceptor.nombre || ' ' || m.usuarioReceptor.apellidos, 
                    m.asunto, 
                    m.mensaje, 
                    m.imagen, 
                    m.urlEnlace
                )
                FROM Mensaje m 
                WHERE m.usuarioReceptor.id = :idUsuarioReceptor 
                AND m.activo = true  
                AND m.leido = :leido
            """)
    List<MensajeRecord> findAllByUsuarioReceptorIdAndActivoTrue(Long idUsuarioReceptor, boolean leido);

    @Query("""
                SELECT new es.musicalia.gestmusica.mensaje.MensajeRecord(
                    m.id, 
                    m.usuarioRemite.nombre || ' ' || m.usuarioRemite.apellidos, 
                    m.usuarioReceptor.nombre || ' ' || m.usuarioReceptor.apellidos, 
                    m.asunto, 
                    m.mensaje, 
                    m.imagen, 
                    m.urlEnlace
                )
                FROM Mensaje m 
                WHERE m.usuarioRemite.id = :idUsuarioRemite 
                AND m.activo = true  
                AND m.leido = :leido
            """)
    List<MensajeRecord> findAllByUsuarioRemiteIdAndActivoTrue(Long idUsuarioRemite, boolean leido);
}