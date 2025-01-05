package es.musicalia.gestmusica.listado;

import es.musicalia.gestmusica.informe.InformeService;
import es.musicalia.gestmusica.localizacion.*;
import es.musicalia.gestmusica.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ListadoServiceImpl implements ListadoService {

	private final InformeService informeService;
	private Logger logger = LoggerFactory.getLogger(ListadoServiceImpl.class);
	private final ProvinciaRepository provinciaRepository;
	private final MunicipioRepository municipioRepository;

	public ListadoServiceImpl(InformeService informeService, ProvinciaRepository provinciaRepository, MunicipioRepository municipioRepository) {
		this.informeService = informeService;

		this.provinciaRepository = provinciaRepository;
		this.municipioRepository = municipioRepository;
	}

	@Override
	public List<CodigoNombreDto> findAllTiposOcupacion() {
		return Arrays.stream(TipoOcupacionEnum.values())
				.map(tipo -> new CodigoNombreDto(tipo.getId(), tipo.getDescripcion()))
				.collect(Collectors.toList());
	}


	@Override
	public byte[] generarInformeListado(ListadoDto listadoDto) {
		Map<String, Object> parametros = new HashMap<String, Object>();
		String fileReport = TipoOcupacionEnum.SIN_OCUPACION.getId().equals(listadoDto.getIdTipoOcupacion()) ? "listado_sin_ocupacion2.jrxml" : "listado_con_ocupacion.jrxml";

		parametros.put("titulo", TipoOcupacionEnum.SIN_OCUPACION.getId().equals(listadoDto.getIdTipoOcupacion()) ? "Listado sin ocupación " : "Listado con ocupación ");


		String fileNameToExport = "Listado_".concat(TipoOcupacionEnum.getDescripcionById(listadoDto.getIdTipoOcupacion())).concat(DateUtils.getDateStr(new Date(), "ddMMyyyyHHmmss")).concat(".pdf");

		List<LocalDate> dateList = sortDates(listadoDto.getFecha1(), listadoDto.getFecha2(), listadoDto.getFecha3(), listadoDto.getFecha4(), listadoDto.getFecha5(), listadoDto.getFecha6(), listadoDto.getFecha7());

		parametros.put("colNames", getColNamesListadoFechas());


		parametros.put("fechaListIn", getFechaListFechas(listadoDto.getFechaDesde(), listadoDto.getFechaHasta(), dateList));

		parametros.put("idProvincia", listadoDto.getIdProvincia().intValue());

		parametros.put("observaciones", listadoDto.getComentario());

		parametros.put("solicitante", listadoDto.getSolicitadoPara());
		parametros.put("provincia", this.provinciaRepository.findById(listadoDto.getIdProvincia()).get().getNombre());
		parametros.put("municipio", listadoDto.getIdMunicipio() != null ? this.municipioRepository.findById(listadoDto.getIdMunicipio()).get().getNombre() : "");
		parametros.put("lugar", listadoDto.getLocalidad() != null ? listadoDto.getLocalidad() : "");


		List<Map.Entry<String, String>> diaList = generateDiaListFechas(listadoDto.getFechaDesde(), listadoDto.getFechaHasta(), dateList);
		// Agregar cada par clave-valor de la lista al mapa de parámetros
		for (Map.Entry<String, String> entry : diaList) {
			parametros.put(entry.getKey(), entry.getValue());
		}
		List<Map.Entry<String, String>> listFechas = generateListFechas(listadoDto.getFechaDesde(), listadoDto.getFechaHasta(), dateList);
		for (Map.Entry<String, String> entry : listFechas) {
			parametros.put(entry.getKey(), entry.getValue());
		}
		return this.informeService.imprimirInforme(parametros, fileNameToExport, fileReport);
	}

	private String getFechaRangoDesde(final LocalDate fechaDesde, List<LocalDate> dateList) {
		if (fechaDesde != null) {
			return formatDateForSQL(fechaDesde);
		} else {
			if (!dateList.isEmpty()) {
				return formatDateForSQL(dateList.get(0));
			}
		}
		return null;

	}

	private String getFechaRangoHasta(final LocalDate fechaDesde, List<LocalDate> dateList) {
		if (fechaDesde != null) {
			return formatDateForSQL(fechaDesde.plusDays(15));
		} else {
			if (!dateList.isEmpty()) {
				return formatDateForSQL(dateList.get(dateList.size() - 1));
			}
		}
		return null;

	}

	private String getColNamesListadoFechaDesdeHasta(final LocalDate fechaIni, final LocalDate fechaFin) {
		if (fechaFin.isBefore(fechaIni)) {
			throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio.");
		}

		long daysBetween = ChronoUnit.DAYS.between(fechaIni, fechaFin) + 1; // +1 para incluir ambos días
		StringBuilder colNamesBuilder = new StringBuilder();
		colNamesBuilder.append("\"Artista\" text, \"Agencia\" text, \"Componentes\" text,\"Escenario\" text,");

		for (int i = 1; i <= daysBetween; i++) {
			colNamesBuilder.append("\"dia")
					.append(i)
					.append("val\" text");

			if (i < daysBetween) {
				colNamesBuilder.append(", ");
			}
		}
		logger.info(colNamesBuilder.toString());
		return colNamesBuilder.toString();

	}


	private String getColNamesListadoFechas() {

		StringBuilder colNamesBuilder = new StringBuilder();
		colNamesBuilder.append("\"Artista\" text, \"Agencia\" text, \"Componentes\" text,\"Escenario\" text,");
		int index = 1;
		// Completar hasta dia16 si faltan
		while (index <= 15) {
			colNamesBuilder.append("\"dia")
					.append(index)
					.append("val\" text");

			if (index < 15) {
				colNamesBuilder.append(", ");
			}
			index++;
		}
		return colNamesBuilder.toString();

	}


	private String getFechaListIn(LocalDate fechaIni, LocalDate fechaFin) {

		if (fechaFin.isBefore(fechaIni)) {
			throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio.");
		}

		long daysBetween = ChronoUnit.DAYS.between(fechaIni, fechaFin) + 1; // +1 para incluir ambos días
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		StringBuilder dateListBuilder = new StringBuilder();

		for (int i = 0; i < daysBetween; i++) {
			LocalDate currentDate = fechaIni.plusDays(i);
			dateListBuilder.append("''")
					.append(currentDate.format(formatter))
					.append("''");

			if (i < daysBetween - 1) {
				dateListBuilder.append(", ");
			}
		}

		return dateListBuilder.toString();

	}

	private String getFechaListFechas(LocalDate fechaIni, LocalDate fechaFin, List<LocalDate> dateList) {
		if (fechaFin != null && fechaFin != null) {
			return getFechaListIn(fechaIni, fechaFin);
		}

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		StringBuilder dateListBuilder = new StringBuilder();
		int index = 1;
		for (LocalDate date : dateList) {

			dateListBuilder.append("''")
					.append(date.format(formatter))
					.append("''");

			if (index <= dateList.size() - 1) {
				dateListBuilder.append(", ");
			}
			index++;
		}

		return dateListBuilder.toString();

	}

	private String formatDateForSQL(LocalDate date) {
		if (date == null) {
			throw new IllegalArgumentException("La fecha no puede ser null.");
		}

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		return "''" + date.format(formatter) + "''";
	}

	private List<Map.Entry<String, String>> generateDiaList(LocalDate fechaDesde, LocalDate fechaHasta) {
		List<Map.Entry<String, String>> diaList = new ArrayList<>();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

		LocalDate currentDate = fechaDesde;
		int diaCounter = 1;

		while (!currentDate.isAfter(fechaHasta)) {
			String key = "dia" + diaCounter;
			String value = currentDate.format(formatter);
			diaList.add(new AbstractMap.SimpleEntry<>(key, value));
			diaCounter++;
			currentDate = currentDate.plusDays(1);
		}

		return diaList;
	}

	private List<Map.Entry<String, String>> generateListFechas(LocalDate fechaDesde, LocalDate fechaHasta, List<LocalDate> dateList) {
		int diaCounter = 1;

		List<Map.Entry<String, String>> diaList = new ArrayList<>();


		if (fechaDesde != null && fechaHasta != null) {

			LocalDate currentDate = fechaDesde;
			while (!currentDate.isAfter(fechaHasta) || diaCounter <= 15) {
				String key = "fecha" + diaCounter;
				String value = formatDateForSQL(currentDate);
				diaList.add(new AbstractMap.SimpleEntry<>(key, value));
				currentDate = currentDate.plusDays(1);
				diaCounter++;

			}

		} else {
			for (LocalDate date : dateList) {
				String key = "fecha" + diaCounter;
				String value = formatDateForSQL(date);
				diaList.add(new AbstractMap.SimpleEntry<>(key, value));
				diaCounter++;
			}
			// Completar hasta dia16 si faltan
			LocalDate lastDate = dateList.get(dateList.size() - 1).plusDays(1);
			while (diaCounter <= 15) {
				String key = "fecha" + diaCounter;
				String value = formatDateForSQL(lastDate);
				diaList.add(new AbstractMap.SimpleEntry<>(key, value));
				diaCounter++;
				lastDate = lastDate.plusDays(1);
			}
		}


		return diaList;
	}

	private List<Map.Entry<String, String>> generateDiaListFechas(LocalDate fechaDesde, LocalDate fechaHasta, List<LocalDate> dateList) {

		if (fechaDesde != null && fechaHasta != null) {
			return generateDiaList(fechaDesde, fechaHasta);
		}

		List<Map.Entry<String, String>> diaList = new ArrayList<>();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

		int diaCounter = 1;

		for (LocalDate date : dateList) {
			String key = "dia" + diaCounter;
			String value = date.format(formatter);
			diaList.add(new AbstractMap.SimpleEntry<>(key, value));
			diaCounter++;
		}

		return diaList;
	}

	private List<LocalDate> sortDates(LocalDate... dates) {
		List<LocalDate> dateList = new ArrayList<>();

		// Agregar las fechas a la lista si no son null
		for (LocalDate date : dates) {
			if (date != null) {
				dateList.add(date);
			}
		}

		// Ordenar la lista
		Collections.sort(dateList);

		return dateList;
	}

}
