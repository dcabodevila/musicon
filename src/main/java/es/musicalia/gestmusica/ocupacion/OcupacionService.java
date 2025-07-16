package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.localizacion.CodigoNombreDto;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    boolean existeOcupacionFecha(OcupacionSaveDto ocupacionSaveDto);

    List<OcupacionRecord> findOcupacionesDtoByAgenciaPendientes(Set<Long> idsAgencia);

    List<OcupacionListRecord> findOcupacionesByArtistasListAndDatesActivo(CustomAuthenticatedUser user, OcupacionListFilterDto ocupacionListFilterDto);
}
