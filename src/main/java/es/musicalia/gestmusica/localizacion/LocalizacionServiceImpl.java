package es.musicalia.gestmusica.localizacion;

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
	public List<Municipio> findMunicipioByProvinciaId(long idProvincia){
		return this.municipioRepository.findMunicipioByProvinciaId(idProvincia);
	}
	@Override
	public List<Provincia> findAllProvincias(){
		return this.provinciaRepository.findProvinciasOrderByName();
	}
	@Override
	public List<CodigoNombreDto> findAllProvinciasByCcaaId(Long idCcaa){
		// Llamada al repositorio que devuelve List<Provincia>
		List<Provincia> provincias = this.provinciaRepository.findProvinciaByIdCcaa(idCcaa);

		// Mapeo de Provincia a ProvinciaDto
		return provincias.stream()
				.map(provincia -> {
					return new CodigoNombreDto(provincia.getId(), provincia.getNombre());
				})
				.collect(Collectors.toList());
	}

	@Override
	public List<CodigoNombreDto> findAllMunicipiosByIdProvincia(Long idProvincia){
		// Llamada al repositorio que devuelve List<Provincia>
		List<Municipio> municipios = this.municipioRepository.findMunicipioByProvinciaId(idProvincia);
		// Mapeo de Provincia a ProvinciaDto
		return municipios.stream()
				.map(municipio -> {
					return new CodigoNombreDto(municipio.getId(), municipio.getNombre());

				})
				.collect(Collectors.toList());
	}

	@Override
	public List<Ccaa> findAllComunidades(){
		return this.ccaaRepository.findAll();
	}

}
