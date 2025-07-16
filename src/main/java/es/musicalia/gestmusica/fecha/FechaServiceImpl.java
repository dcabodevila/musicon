package es.musicalia.gestmusica.fecha;

import es.musicalia.gestmusica.ocupacion.OcupacionRecord;
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
import java.util.Set;
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
	public List<FechaDto> findFechaDtoByArtistaId(long idArtista, LocalDateTime start, LocalDateTime end) {

		List<FechaDto> listaFechas = getFechasTarifas(idArtista, start, end);
		List<FechaDto> listaOcupaciones = getFechasOcupaciones(idArtista, start, end);

		// Obtener los días que tienen ocupaciones
		Set<LocalDate> fechasOcupadas = listaOcupaciones.stream()
				.map(fecha -> fecha.start().toLocalDate())
				.collect(Collectors.toSet());

		// Filtrar tarifas: solo aquellas que NO tienen ocupaciones en su día
		List<FechaDto> tarifasFiltradas = listaFechas.stream()
				.filter(fecha -> !fechasOcupadas.contains(fecha.start().toLocalDate()))
				.toList();

		// Resultado final: todas las ocupaciones + tarifas sin ocupar
		List<FechaDto> resultado = new ArrayList<>();
		resultado.addAll(listaOcupaciones);
		resultado.addAll(tarifasFiltradas);

		return resultado;
	}



	private List<FechaDto> getFechasTarifas(long idArtista, LocalDateTime start, LocalDateTime end) {
		final List<TarifaDto> listaTarifas = this.tarifaRepository.findTarifasDtoByArtistaIdAndDates(idArtista, start, end).orElse(new ArrayList<>());
		return listaTarifas.stream()
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
	}

	private List<FechaDto> getFechasOcupaciones(long idArtista, LocalDateTime start, LocalDateTime end) {
		List<OcupacionRecord> listaOcupaciones = this.ocupacionRepository.findOcupacionesDtoByArtistaIdAndDates(idArtista, start, end);

		return listaOcupaciones.stream()
				.map(ocupacionRecord -> new FechaDto(
						ocupacionRecord.id(),
						ocupacionRecord.start(),
						ocupacionRecord.idArtista(),
						ocupacionRecord.provincia(),
						true,
						TipoFechaEnum.OCUPACION.getDescripcion(),
						ocupacionRecord.tipoOcupacion(),
						ocupacionRecord.estado() + "<br>" + ocupacionRecord.localidad() + "<br>"+ ocupacionRecord.municipio() + ", " + ocupacionRecord.provincia() +  (ocupacionRecord.soloMatinal() ? "<br>Solo matinal" : ocupacionRecord.matinal()? "<br>"+ "Matinal" : "") ,
						ocupacionRecord.matinal(),
						ocupacionRecord.estado()
				))
				.collect(Collectors.toList());

	}

}
