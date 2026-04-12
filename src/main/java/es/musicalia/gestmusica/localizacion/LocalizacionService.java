package es.musicalia.gestmusica.localizacion;


import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Optional;

public interface LocalizacionService {
    Provincia findProvinciaByUsuarioId(Long idUsuario);

    Ccaa findCcaaByUsuarioId(Long idUsuario);

    List<CodigoNombreRecord> findAllProvincias();
    List<CodigoNombreRecord> findAllComunidades();
    List<CodigoNombreRecord> findAllProvinciasByCcaaId(Long idCcaa);
    List<CodigoNombreRecord> findAllMunicipiosByIdProvincia(Long idProvincia);

    @Cacheable(cacheNames  = "localidades")
    List<CodigoNombreRecord> findLocalidadByIdMunicipio(long idMunicipio);

    /**
     * Busca municipios por nombre de provincia (case-insensitive).
     * Resuelve la provincia por nombre ignorando mayúsculas/minúsculas
     * y devuelve sus municipios.
     *
     * @param nombreProvincia Nombre de la provincia (case-insensitive)
     * @return Lista de municipios de la provincia, vacía si no existe
     */
    @Cacheable(cacheNames = "municipiosPorProvinciaNombre", key = "#nombreProvincia.toUpperCase()")
    List<CodigoNombreRecord> findMunicipiosByProvinciaNombre(String nombreProvincia);

    /**
     * Busca una provincia por nombre ignorando mayúsculas/minúsculas.
     *
     * @param nombre Nombre de la provincia (case-insensitive)
     * @return Optional con la provincia encontrada
     */
    Optional<Provincia> findProvinciaByNombreUpperCase(String nombre);
}
