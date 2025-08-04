package es.musicalia.gestmusica.localizacion;

import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class LocalizacionServiceImpl implements LocalizacionService {

	private final CcaaRepository ccaaRepository;
	private final ProvinciaRepository provinciaRepository;
	private final MunicipioRepository municipioRepository;
	private final LocalidadRepository localidadRepository;
	private final UsuarioRepository usuarioRepository;

	public LocalizacionServiceImpl(ProvinciaRepository provinciaRepository, MunicipioRepository municipioRepository, CcaaRepository ccaaRepository, LocalidadRepository localidadRepository, UsuarioRepository usuarioRepository){
		this.municipioRepository  = municipioRepository;
		this.provinciaRepository = provinciaRepository;
		this.ccaaRepository = ccaaRepository;
        this.localidadRepository = localidadRepository;
        this.usuarioRepository = usuarioRepository;
    }

	@Override
	public Provincia findProvinciaByUsuarioId(Long idUsuario){
		final Usuario usuario = this.usuarioRepository.findById(idUsuario).orElseThrow();
		return usuario.getProvincia();
	}

	@Override
	public Ccaa findCcaaByUsuarioId(Long idUsuario){
		final Usuario usuario = this.usuarioRepository.findById(idUsuario).orElseThrow();
		return usuario.getProvincia().getCcaa();
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

	@Cacheable(cacheNames = "localidadesPorMunicipio", key = "#idMunicipio")
	@Override
	public List<CodigoNombreRecord> findLocalidadByIdMunicipio(long idMunicipio){
		return this.localidadRepository.findLocalidadByIdMunicipio(idMunicipio);
	}


}