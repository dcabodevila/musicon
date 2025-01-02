package es.musicalia.gestmusica.incremento;

import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.localizacion.ProvinciaRepository;
import es.musicalia.gestmusica.usuario.UserService;
import org.modelmapper.internal.util.Assert;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class IncrementoServiceImpl implements IncrementoService {

	private IncrementoRepository incrementoRepository;
	private ArtistaRepository artistaRepository;
	private ProvinciaRepository provinciaRepository;
	private TipoIncrementoRepository tipoIncrementoRepository;
	private UserService userService;

	public IncrementoServiceImpl(IncrementoRepository incrementoRepository, ArtistaRepository artistaRepository, UserService userService, ProvinciaRepository provinciaRepository, TipoIncrementoRepository tipoIncrementoRepository){
		this.incrementoRepository = incrementoRepository;
		this.artistaRepository = artistaRepository;
		this.provinciaRepository =  provinciaRepository;
		this.tipoIncrementoRepository = tipoIncrementoRepository;
		this.userService = userService;

	}

	public List<IncrementoListDto> findByIncrementosByArtista(long idArtista){
		return this.incrementoRepository.findIncrementosByArtistaId(idArtista);
	}

	@Transactional(readOnly = false)
	@Override
	public Incremento saveIncremento(IncrementoSaveDto incrementoSaveDto){
		Assert.notNull(incrementoSaveDto.getIdArtista());
		Assert.notNull(incrementoSaveDto.getIdProvincia());
		Assert.notNull(incrementoSaveDto.getIdTipoIncremento());

		Incremento incremento = this.incrementoRepository.findIncrementoByAgenciaIdAndProvinciaId(incrementoSaveDto.getIdArtista(), incrementoSaveDto.getIdProvincia());

		if (incremento==null){
			incremento = new Incremento();
		}

		incremento.setTipoIncremento(this.tipoIncrementoRepository.findById(incrementoSaveDto.getIdTipoIncremento()).orElseThrow());
		incremento.setProvincia(this.provinciaRepository.findById(incrementoSaveDto.getIdProvincia()).orElseThrow());
		incremento.setArtista(this.artistaRepository.findById(incrementoSaveDto.getIdArtista()).orElseThrow());
		incremento.setIncremento(incrementoSaveDto.getIncremento());

		return this.incrementoRepository.save(incremento);

	}

	@Override
	public List<TipoIncremento> listTipoIncremento(){
		return this.tipoIncrementoRepository.findAll();

	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal findIncrementoByAgenciaIdAndProvinciaId(long idArtista, long idProvincia){
		final Incremento incremento = this.incrementoRepository.findIncrementoByAgenciaIdAndProvinciaId(idArtista, idProvincia);
		return incremento!=null? incremento.getIncremento() : BigDecimal.ZERO;
	}

}
