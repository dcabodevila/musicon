package es.musicalia.gestmusica.localizacion;

import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocalizacionServiceImpl implements LocalizacionService {

	private final CcaaRepository ccaaRepository;
	private final ProvinciaRepository provinciaRepository;
	private final MunicipioRepository municipioRepository;

	public LocalizacionServiceImpl(ProvinciaRepository provinciaRepository, MunicipioRepository municipioRepository, CcaaRepository ccaaRepository){
		this.municipioRepository  = municipioRepository;
		this.provinciaRepository = provinciaRepository;
		this.ccaaRepository = ccaaRepository;
	}

	@Override
	@Cacheable(cacheNames  = "provincias")
	public List<CodigoNombreRecord> findAllProvincias(){
		return this.provinciaRepository.findProvinciasOrderByName();
	}
	
	@Override
	@Cacheable(cacheNames  = "provinciasPorCcaa", key = "#idCcaa")
	public List<CodigoNombreRecord> findAllProvinciasByCcaaId(Long idCcaa){
		// Llamada al repositorio que devuelve List<Provincia>
		return this.provinciaRepository.findProvinciaByIdCcaa(idCcaa);
	}

	@Override
	@Cacheable(cacheNames  = "municipiosPorProvincia", key = "#idProvincia")
	public List<CodigoNombreRecord> findAllMunicipiosByIdProvincia(Long idProvincia){
		// Llamada al repositorio que devuelve List<Provincia>
		return this.municipioRepository.findMunicipioByProvinciaId(idProvincia);
	}

	@Override
	@Cacheable(cacheNames  = "comunidades")
	public List<CodigoNombreRecord> findAllComunidades(){
		return this.ccaaRepository.findAllCcaaOrderedByName();
	}
}