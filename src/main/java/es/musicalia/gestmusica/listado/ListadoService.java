package es.musicalia.gestmusica.listado;


import es.musicalia.gestmusica.localizacion.Ccaa;
import es.musicalia.gestmusica.localizacion.CodigoNombreDto;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.Provincia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface ListadoService {
    List<ListadosPorMesDto> obtenerListadosPorMes(List<ListadoRecord> listados);

    List<CodigoNombreDto> findAllTiposOcupacion();

    byte[] generarInformeListado(ListadoDto listadoDto, Long idUsuario);

    List<ListadoRecord> obtenerListadoEntreFechas(ListadoAudienciasDto listadoAudienciasDto);

    Page<ListadoRecord> obtenerListadoEntreFechasPaginado(
            ListadoAudienciasDto filtros,
            String searchValue,
            Pageable pageable,
            Long idUsuario);


}
