package es.musicalia.gestmusica.tarifa;


import java.time.LocalDateTime;
import java.util.List;

public interface TarifaService {
	List<TarifaDto> findByArtistaId(long idArtista, LocalDateTime start, LocalDateTime end);
	void saveTarifa(TarifaSaveDto tarifaSaveDto);

}
