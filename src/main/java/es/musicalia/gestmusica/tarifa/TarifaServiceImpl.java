package es.musicalia.gestmusica.tarifa;

import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.usuario.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TarifaServiceImpl implements TarifaService {


	private final TarifaRepository tarifaRepository;
	private final ArtistaRepository artistaRepository;
	private final UserService userService;

	public TarifaServiceImpl(TarifaRepository tarifaRepository, ArtistaRepository artistaRepository, UserService userService){
		this.tarifaRepository = tarifaRepository;
		this.artistaRepository = artistaRepository;
		this.userService = userService;
	}
	@Override
	public List<TarifaDto> findByArtistaId(long idArtista , LocalDateTime start, LocalDateTime end){
		return this.tarifaRepository.findTarifasDtoByArtistaIdAndDates(idArtista, start, end).orElse(new ArrayList<>());
	}

	@Override
	public TarifaDto findByArtistaIdAndDate(long idArtista, LocalDate fecha){
		// 1. Convertir el LocalDate en LocalDateTime a medianoche
		LocalDateTime startOfDay = fecha.atStartOfDay();
		// 2. endOfDay podría ser 23:59:59.999..., según tu preferencia
		LocalDateTime endOfDay = fecha.atTime(LocalTime.of(23, 59, 59));
		final Optional<List<TarifaDto>> optionalTarifas = this.tarifaRepository.findTarifasDtoByArtistaIdAndDates(idArtista, startOfDay, endOfDay);
		return optionalTarifas.isPresent() && !optionalTarifas.get().isEmpty()? optionalTarifas.get().get(0) : null;

	}

	@Override
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

		if (listaTarifasFecha != null && !listaTarifasFecha.isEmpty()){
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
