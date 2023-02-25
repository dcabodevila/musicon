package es.musicalia.gestmusica.tarifa;

import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.usuario.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TarifaServiceImpl implements TarifaService {

	private TarifaRepository tarifaRepository;
	private ArtistaRepository artistaRepository;
	private UserService userService;

	public TarifaServiceImpl(TarifaRepository tarifaRepository, ArtistaRepository artistaRepository, UserService userService){
		this.tarifaRepository = tarifaRepository;
		this.artistaRepository = artistaRepository;
		this.userService = userService;
	}

	public List<TarifaDto> findByArtistaId(long idArtista , LocalDateTime start, LocalDateTime end){
		return this.tarifaRepository.findTarifasDtoByArtistaIdAndDates(idArtista, start, end);
	}

	@Transactional(readOnly = false)
	public void saveTarifa(TarifaSaveDto tarifaSaveDto){

		//TODO: Crear tarifas por día fecha desde/hasta
		if (tarifaSaveDto.getFechaHasta()!=null){
			Duration duracion = Duration.between(tarifaSaveDto.getFechaDesde(), tarifaSaveDto.getFechaHasta());
			for (int i = 0; i <= duracion.toDays(); i++) {
				LocalDateTime fechaToSave = tarifaSaveDto.getFechaDesde().plusDays(i);
				guardarTarifaFecha(tarifaSaveDto, fechaToSave);
			}
		}
		else {
			guardarTarifaFecha(tarifaSaveDto, tarifaSaveDto.getFechaDesde());
		}

	}

	private void guardarTarifaFecha(TarifaSaveDto tarifaSaveDto, LocalDateTime fecha) {
		Tarifa tarifa = tarifaSaveDto.getId()!=null ? this.tarifaRepository.findById(tarifaSaveDto.getId()).orElse(new Tarifa()) : new Tarifa();

		final List<Tarifa> listaTarifasFecha = this.tarifaRepository.findTarifasByArtistaIdAndDates(tarifaSaveDto.getIdArtista(), fecha.withHour(0).withMinute(0).withSecond(0) , fecha.withHour(23).withMinute(59).withSecond(59));

		if (listaTarifasFecha!=null && !listaTarifasFecha.isEmpty()){
			tarifa =  listaTarifasFecha.get(0);
		}
		tarifa.setArtista(this.artistaRepository.findById(tarifaSaveDto.getIdArtista()).orElseThrow());
		tarifa.setFecha(fecha);
		tarifa.setImporte(tarifaSaveDto.getImporte());
		tarifa.setActivo(tarifaSaveDto.getActivo()!=null? tarifaSaveDto.getActivo() : Boolean.TRUE);





		final String userName = this.userService.obtenerUsuarioAutenticado().getUsername();

		if (tarifa.getFechaCreacion()==null){
			tarifa.setFechaCreacion(LocalDateTime.now());
			tarifa.setUsuarioCreacion(userName);
		}
		else {
			tarifa.setFechaModificacion(LocalDateTime.now());
			tarifa.setUsuarioModificacion(userName);
		}
		this.tarifaRepository.save(tarifa);
	}

}
