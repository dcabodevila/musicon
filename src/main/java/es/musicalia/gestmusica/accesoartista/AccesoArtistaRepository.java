package es.musicalia.gestmusica.accesoartista;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AccesoArtistaRepository extends JpaRepository<AccesoArtista, Long> {

    @Query("select a from AccesoArtista a where a.artista.agencia.id = ?1 and a.activo and a.permiso.tipoPermiso<>0 and a.artista.activo order by a.artista.nombre, a.usuario.nombre, a.usuario.apellidos")
    Optional<List<AccesoArtista>> findAllAccesosByIdAgencia(Long idAgencia);

    @Query("select a from AccesoArtista a where a.artista.id = ?1 and a.usuario.id = ?2 and a.permiso.id = ?3 and a.activo and a.artista.activo")
    Optional<AccesoArtista> findAllAccesosByIdArtistaIdUsuarioIdPermiso(Long idArtista, Long idUsuario, Long idPermiso);

    @Query("select a from AccesoArtista a where a.usuario.id = ?1 and a.activo and a.artista.activo")
    Optional<List<AccesoArtista>> findAllAccesosArtistaByIdUsuario(Long idUsuario);

    @Query("select a from AccesoArtista a where a.artista.id = ?1 and a.usuario.id = ?2 and a.permiso.id = ?3 and a.activo and a.artista.activo")
    Optional<AccesoArtista> findAllAccesosByIdArtistaIdUsuario(Long idArtista, Long idUsuario, Long idPermiso);

}
