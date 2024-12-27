package es.musicalia.gestmusica.localizacion;


import java.util.List;

public interface LocalizacionService {
    List<Municipio> findMunicipioByProvinciaId(long idProvincia);
    List<Provincia> findAllProvincias();
    List<Ccaa> findAllComunidades();
    List<CodigoNombreDto> findAllProvinciasByCcaaId(Long idCcaa);
    List<CodigoNombreDto> findAllMunicipiosByIdProvincia(Long idProvincia);
    }
