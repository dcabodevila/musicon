package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.localizacion.CodigoNombreDto;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OcupacionService {

    List<CodigoNombreDto> listarTiposOcupacion(Long idArtista);

    OcupacionEditDto findOcupacionEditDtoByArtistaIdAndDates(long id);
    @Transactional(readOnly = false)
    DefaultResponseBody anularOcupacion(long id);
    @Transactional(readOnly = false)
    DefaultResponseBody confirmarOcupacion(long id);

    @Transactional(readOnly = false)
    DefaultResponseBody saveOcupacion(OcupacionSaveDto ocupacionSaveDto) throws ModificacionOcupacionException;

    Ocupacion guardarOcupacion(OcupacionSaveDto ocupacionSaveDto, boolean permisoConfirmarOcupacionAgencia, boolean isSincronizacion) throws ModificacionOcupacionException;

    boolean existeOcupacionFecha(OcupacionSaveDto ocupacionSaveDto);

    List<OcupacionRecord> findOcupacionesDtoByAgenciaPendientes(Set<Long> idsAgencia);

    Page<OcupacionListRecord> findOcupacionesByArtistasListAndDatesActivo(CustomAuthenticatedUser user, OcupacionListFilterDto ocupacionListFilterDto, Pageable pageable);

    OcupacionSaveDto getOcupacionSaveDto(Long idOcupacion);

    Optional<Ocupacion> buscarPorIdOcupacionLegacy(Integer idOcupacionLegacy);
}
