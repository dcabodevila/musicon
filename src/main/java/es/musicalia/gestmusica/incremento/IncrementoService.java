package es.musicalia.gestmusica.incremento;


import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

public interface IncrementoService {
	List<IncrementoListDto> findByIncrementosByArtista(long idArtista);
	Incremento saveIncremento(IncrementoSaveDto incrementoSaveDto);

	List<TipoIncremento> listTipoIncremento();

	BigDecimal findIncrementoByAgenciaIdAndProvinciaId(long idArtista, long idProvincia);
}
