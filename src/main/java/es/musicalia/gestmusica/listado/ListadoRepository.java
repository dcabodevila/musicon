package es.musicalia.gestmusica.listado;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ListadoRepository extends JpaRepository<Listado, Long>, JpaSpecificationExecutor<Listado> {

//    @Query("SELECT new es.musicalia.gestmusica.listado.ListadoRecord(" +
//            "l.id, " +
//            "l.solicitadoPara, " +
//            "l.localidad, " +
//            "l.municipio.nombre, " +
//            "l.usuario.nombre, " +
//            "l.usuario.apellidos, " +
//            "CAST(l.tipoOcupacion AS string), " +
//            "l.fechaCreacion, " +
//            "l.fecha1, " +
//            "l.fecha2, " +
//            "l.fecha3, " +
//            "l.fecha4, " +
//            "l.fecha5, " +
//            "l.fecha6, " +
//            "l.fecha7, " +
//            "l.fechaDesde, " +
//            "l.fechaHasta) " +
//            "FROM Listado l " +
//            "JOIN l.agencias a " +
//            "WHERE a.id = :idAgencia " +
//            "AND l.fechaCreacion >= :fechaDesde " +
//            "AND l.fechaCreacion <= :fechaHasta " +
//            "ORDER BY l.fechaCreacion DESC")
//    List<ListadoRecord> findListadosByAgenciaAndFechasOpcionales(
//            @Param("idAgencia") Long idAgencia,
//            @Param("fechaDesde") LocalDateTime fechaDesde,
//            @Param("fechaHasta") LocalDateTime fechaHasta
//    );
}