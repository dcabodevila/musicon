package es.musicalia.gestmusica.tarifa;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TarifaServiceImpl implements TarifaService {

	private TarifaRepository tarifaRepository;

	public TarifaServiceImpl(TarifaRepository tarifaRepository){
		this.tarifaRepository = tarifaRepository;
	}

	public List<TarifaDto> findByArtistaId(long idArtista){
		return this.tarifaRepository.findTarifasByArtistaId(idArtista);
	}
}
