package es.musicalia.gestmusica.listado;


import es.musicalia.gestmusica.localizacion.Ccaa;
import es.musicalia.gestmusica.localizacion.CodigoNombreDto;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.Provincia;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ListadoService {
    List<ListadosPorMesDto> obtenerListadosPorMes(List<ListadoRecord> listados);

    List<CodigoNombreDto> findAllTiposOcupacion();

    byte[] generarInformeListado(ListadoDto listadoDto, Long idUsuario);

    List<ListadoRecord> obtenerListadoEntreFechas(ListadoAudienciasDto listadoAudienciasDto);
}
