package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.localizacion.CodigoNombreDto;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface OcupacionService {

    List<CodigoNombreDto> listarTiposOcupacion(Long idArtista);

    OcupacionEditDto findOcupacionEditDtoByArtistaIdAndDates(long id);
    @Transactional(readOnly = false)
    Void anularOcupacion(long id);
    @Transactional(readOnly = false)
    Void confirmarOcupacion(long id);

    @Transactional(readOnly = false)
    Ocupacion saveOcupacion(OcupacionSaveDto ocupacionSaveDto) throws ModificacionOcupacionException;

    boolean existeOcupacionFecha(OcupacionSaveDto ocupacionSaveDto);

    List<OcupacionDto> findOcupacionesDtoByAgenciaPendientes(Set<Long> idsAgencia);
}
