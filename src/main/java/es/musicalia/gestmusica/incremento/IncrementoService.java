package es.musicalia.gestmusica.incremento;


import java.util.List;

public interface IncrementoService {
	List<IncrementoListDto> findByIncrementosByAgencia(long idArtista);
	Incremento saveIncremento(IncrementoSaveDto incrementoSaveDto);

	List<TipoIncremento> listTipoIncremento();

}
