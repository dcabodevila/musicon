package es.musicalia.gestmusica.fecha;

import es.musicalia.gestmusica.ocupacion.OcupacionDto;
import es.musicalia.gestmusica.ocupacion.OcupacionRepository;
import es.musicalia.gestmusica.tarifa.TarifaDto;
import es.musicalia.gestmusica.tarifa.TarifaRepository;
import es.musicalia.gestmusica.usuario.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FechaServiceImpl implements FechaService {


	private final TarifaRepository tarifaRepository;
	private final OcupacionRepository ocupacionRepository;
	private final UserService userService;

	public FechaServiceImpl(TarifaRepository tarifaRepository, OcupacionRepository ocupacionRepository, UserService userService){
		this.tarifaRepository = tarifaRepository;
        this.ocupacionRepository = ocupacionRepository;
        this.userService = userService;
	}
	@Override
	public List<FechaDto> findFechaDtoByArtistaId(long idArtista , LocalDateTime start, LocalDateTime end){

		final List<FechaDto> listaFechas = getFechasTarifas(idArtista, start, end);

		Map<LocalDate, FechaDto> fechaFechaDtoMap = listaFechas.stream()
				.collect(Collectors.toMap(
						fecha -> fecha.start().toLocalDate(), // Clave: fecha sin hora
						fecha -> fecha, // Valor: el objeto FechaDto
						(existing, replacement) -> existing // Manejar duplicados, mantener el primero
				));


		final List<FechaDto> listaOcupaciones = getFechasOcupaciones(idArtista, start, end);

		// Sobrescribir con listaOcupaciones usando lambda
		listaOcupaciones.forEach(ocupacion -> {
			LocalDate fecha = ocupacion.start().toLocalDate();
			fechaFechaDtoMap.put(fecha, ocupacion); // Sobrescribe si la fecha ya existe
		});

		// Actualizar listaTarifas con los datos del mapa
		return fechaFechaDtoMap.values().stream().collect(Collectors.toList());

	}

	private List<FechaDto> getFechasTarifas(long idArtista, LocalDateTime start, LocalDateTime end) {
		final List<TarifaDto> listaTarifas = this.tarifaRepository.findTarifasDtoByArtistaIdAndDates(idArtista, start, end).orElse(new ArrayList<>());
		List<FechaDto> listaFechas = listaTarifas.stream()
				.map(tarifaDto -> new FechaDto(
						tarifaDto.id(),
						tarifaDto.start(),
						tarifaDto.idArtista(),
						tarifaDto.title(),
						tarifaDto.allDay(),
						TipoFechaEnum.TARIFA.getDescripcion(),
						null,
						TipoFechaEnum.TARIFA.getDescripcion(),
						Boolean.FALSE, ""
				))
				.collect(Collectors.toList());
		return listaFechas;
	}

	private List<FechaDto> getFechasOcupaciones(long idArtista, LocalDateTime start, LocalDateTime end) {
		List<OcupacionDto> listaOcupaciones = this.ocupacionRepository.findOcupacionesDtoByArtistaIdAndDates(idArtista, start, end);

		List<FechaDto> listaFechas = listaOcupaciones.stream()
				.map(ocupacionDto -> new FechaDto(
						ocupacionDto.id(),
						ocupacionDto.start(),
						ocupacionDto.idArtista(),
						ocupacionDto.provincia(),
						ocupacionDto.allDay(),
						TipoFechaEnum.OCUPACION.getDescripcion(),
						ocupacionDto.tipoOcupacion(),
						ocupacionDto.estado() + "<br>" +ocupacionDto.localidad() + "<br>"+ ocupacionDto.municipio() + ", " + ocupacionDto.provincia() +  (ocupacionDto.matinal()? "<br>"+ "Matinal" : "") ,
						ocupacionDto.matinal(),
						ocupacionDto.estado()
				))
				.collect(Collectors.toList());
		return listaFechas;
	}

}
