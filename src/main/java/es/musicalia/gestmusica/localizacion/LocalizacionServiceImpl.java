package es.musicalia.gestmusica.localizacion;

import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LocalizacionServiceImpl implements LocalizacionService {

	private CcaaRepository ccaaRepository;
	private ProvinciaRepository provinciaRepository;
	private MunicipioRepository municipioRepository;


	public LocalizacionServiceImpl(ProvinciaRepository provinciaRepository, MunicipioRepository municipioRepository, CcaaRepository ccaaRepository){
		this.municipioRepository  = municipioRepository;
		this.provinciaRepository = provinciaRepository;
		this.ccaaRepository = ccaaRepository;
	}

	@Override
	public List<CodigoNombreRecord> findAllProvincias(){
		return this.provinciaRepository.findProvinciasOrderByName();
	}
	@Override
	public List<CodigoNombreRecord> findAllProvinciasByCcaaId(Long idCcaa){
		// Llamada al repositorio que devuelve List<Provincia>
		return this.provinciaRepository.findProvinciaByIdCcaa(idCcaa);
	}

	@Override
	public List<CodigoNombreRecord> findAllMunicipiosByIdProvincia(Long idProvincia){
		// Llamada al repositorio que devuelve List<Provincia>
		return this.municipioRepository.findMunicipioByProvinciaId(idProvincia);
	}

	@Override
	public List<CodigoNombreRecord> findAllComunidades(){
		return this.ccaaRepository.findAllCcaaOrderedByName();
	}

}
