package es.musicalia.gestmusica.localizacion;


import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

public interface LocalizacionService {
    List<CodigoNombreRecord> findAllProvincias();
    List<CodigoNombreRecord> findAllComunidades();
    List<CodigoNombreRecord> findAllProvinciasByCcaaId(Long idCcaa);
    List<CodigoNombreRecord> findAllMunicipiosByIdProvincia(Long idProvincia);

    @Cacheable(cacheNames  = "localidades")
    List<CodigoNombreRecord> findLocalidadByIdMunicipio(long idMunicipio);
}
