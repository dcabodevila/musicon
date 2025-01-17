package es.musicalia.gestmusica.localizacion;


import es.musicalia.gestmusica.generic.CodigoNombreRecord;

import java.util.List;

public interface LocalizacionService {
    List<CodigoNombreRecord> findAllProvincias();
    List<CodigoNombreRecord> findAllComunidades();
    List<CodigoNombreRecord> findAllProvinciasByCcaaId(Long idCcaa);
    List<CodigoNombreRecord> findAllMunicipiosByIdProvincia(Long idProvincia);
    }
