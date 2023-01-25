package es.musicalia.gestmusica.tarifa;


import java.util.List;

public interface TarifaService {
	List<TarifaDto> findByArtistaId(long idArtista);

}
