package es.musicalia.gestmusica.listado;

import es.musicalia.gestmusica.informe.InformeService;
import es.musicalia.gestmusica.localizacion.*;
import es.musicalia.gestmusica.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ListadoServiceImpl implements ListadoService {

	private final InformeService informeService;
	private Logger logger = LoggerFactory.getLogger(ListadoServiceImpl.class);

	public ListadoServiceImpl(InformeService informeService){
        this.informeService = informeService;
    }

	@Override
	public List<CodigoNombreDto> findAllTiposOcupacion() {
		return Arrays.stream(TipoOcupacionEnum.values())
				.map(tipo -> new CodigoNombreDto(tipo.getId(), tipo.getDescripcion()))
				.collect(Collectors.toList());
	}


	@Override
	public byte[] generarInformeListado(ListadoDto listadoDto){
		Map<String, Object> parametros = new HashMap<String, Object>();
		String fileReport = TipoOcupacionEnum.SIN_OCUPACION.getId().equals(listadoDto.getIdTipoOcupacion()) ?  "listado_sin_ocupacion.jrxml": "listado_con_ocupacion.jrxml";


		parametros.put("titulo", "Titulo test");


		String fileNameToExport = "Listado_".concat(TipoOcupacionEnum.getDescripcionById(listadoDto.getIdTipoOcupacion())).concat(DateUtils.getDateStr(new Date(), "ddMMyyyyHHmmss")).concat(".pdf");


		parametros.put("colNames", getColNamesListado(listadoDto.getFechaDesde(), listadoDto.getFechaDesde().plusDays(15)));

		parametros.put("fechaRangoDesde", formatDateForSQL(listadoDto.getFechaDesde()));
		parametros.put("fechaRangoHasta", formatDateForSQL(listadoDto.getFechaDesde().plusDays(15)));

		parametros.put("fechaListIn", getFechaListIn(listadoDto.getFechaDesde(), listadoDto.getFechaHasta()));

		List<Map.Entry<String, String>> diaList = generateDiaList(listadoDto.getFechaDesde(), listadoDto.getFechaHasta());

		// Agregar cada par clave-valor de la lista al mapa de parámetros
		for (Map.Entry<String, String> entry : diaList) {
			parametros.put(entry.getKey(), entry.getValue());
		}
		return this.informeService.imprimirInforme(parametros, fileNameToExport, fileReport);
	}

	private String getColNamesListado(LocalDate fechaIni, LocalDate fechaFin){
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

	private String getFechaListIn(LocalDate fechaIni, LocalDate fechaFin){

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

	public static String formatDateForSQL(LocalDate date) {
		if (date == null) {
			throw new IllegalArgumentException("La fecha no puede ser null.");
		}

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		return "''" + date.format(formatter) + "''";
	}

	public static List<Map.Entry<String, String>> generateDiaList(LocalDate fechaDesde, LocalDate fechaHasta) {
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
}
