package es.musicalia.gestmusica.listado;


import es.musicalia.gestmusica.localizacion.Ccaa;
import es.musicalia.gestmusica.localizacion.CodigoNombreDto;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.Provincia;

import java.util.List;

public interface ListadoService {
    List<ListadosPorMesDto> obtenerListadosPorMes(List<ListadoRecord> listados);

    List<CodigoNombreDto> findAllTiposOcupacion();

    byte[] generarInformeListado(ListadoDto listadoDto);

    List<ListadoRecord> obtenerListadoEntreFechas(ListadoAudienciasDto listadoAudienciasDto);
}
