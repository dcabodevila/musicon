package es.musicalia.gestmusica.localizacion;


import java.util.List;

public interface LocalizacionService {
    List<Municipio> findMunicipioByProvinciaId(long idProvincia);
    List<Provincia> findAllProvincias();
    List<Ccaa> findAllComunidades();

}
