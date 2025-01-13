package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.localizacion.CodigoNombreDto;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface OcupacionService {

    List<CodigoNombreDto> listarTiposOcupacion();

    OcupacionEditDto findOcupacionEditDtoByArtistaIdAndDates(long id);
    @Transactional(readOnly = false)
    Void anularOcupacion(long id);
    @Transactional(readOnly = false)
    Void confirmarOcupacion(long id);

    @Transactional(readOnly = false)
    Ocupacion saveOcupacion(OcupacionSaveDto ocupacionSaveDto);

    boolean existeOcupacionFecha(OcupacionSaveDto ocupacionSaveDto);
}
