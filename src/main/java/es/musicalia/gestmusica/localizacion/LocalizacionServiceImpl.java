package es.musicalia.gestmusica.localizacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

	public List<Municipio> findMunicipioByProvinciaId(long idProvincia){
		return this.municipioRepository.findMunicipioByProvinciaId(idProvincia);
	}

	public List<Provincia> findAllProvincias(){
		return this.provinciaRepository.findAll();
	}

	public List<Ccaa> findAllComunidades(){
		return this.ccaaRepository.findAll();
	}

}
