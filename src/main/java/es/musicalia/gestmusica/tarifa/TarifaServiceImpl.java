package es.musicalia.gestmusica.tarifa;

import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.excel.ExcelExportService;
import es.musicalia.gestmusica.incremento.IncrementoRepository;
import es.musicalia.gestmusica.informe.InformeService;
import es.musicalia.gestmusica.listado.TipoReportEnum;
import es.musicalia.gestmusica.listado.TipoTarifaEnum;
import es.musicalia.gestmusica.localizacion.ProvinciaRepository;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TarifaServiceImpl implements TarifaService {


	private final TarifaRepository tarifaRepository;
	private final ArtistaRepository artistaRepository;
	private final UserService userService;
	private final InformeService informeService;
    private final ProvinciaRepository provinciaRepository;
	private final IncrementoRepository incrementoRepository;
	private final ExcelExportService excelExportService;

	public TarifaServiceImpl(TarifaRepository tarifaRepository, ArtistaRepository artistaRepository, UserService userService, InformeService informeService, ProvinciaRepository provinciaRepository, IncrementoRepository incrementoRepository, ExcelExportService excelExportService){
		this.tarifaRepository = tarifaRepository;
		this.artistaRepository = artistaRepository;
		this.userService = userService;
        this.informeService = informeService;
        this.provinciaRepository = provinciaRepository;
		this.incrementoRepository = incrementoRepository;
		this.excelExportService = excelExportService;
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

	@Override
	@Transactional(readOnly = false)
	public byte[] getInformeTarifaAnual(final TarifaAnualDto tarifaAnualDto) {
		// Cargar el informe desde algún lugar y almacenarlo en un arreglo de bytes.
		Map<String, Object> parametros = new HashMap<String, Object>();
		final Artista artista = this.artistaRepository.findById(tarifaAnualDto.getIdArtista()).orElseThrow();
		parametros.put("titulo", artista.getNombre());
		parametros.put("idArtista", tarifaAnualDto.getIdArtista());
		parametros.put("ano", tarifaAnualDto.getAno().toString());
		parametros.put("idProvincia", tarifaAnualDto.getIdProvincia().intValue());
		parametros.put("conOcupacion", tarifaAnualDto.getConOcupacion());


        String sbDatosAgrupacion = obtenerDatosAgrupacion(artista);


        parametros.put("datosAgrupacion", sbDatosAgrupacion);
        final String provincia = this.provinciaRepository.findById(tarifaAnualDto.getIdProvincia()).get().getNombre();

        parametros.put("datosAgencia", obtenerDatosAgencia(artista, provincia));

		String fileNameToExport = artista.getNombre().concat(DateUtils.getDateStr(new Date(), "ddMMyyyyHHmmss")).concat(".pdf");
		String fileReport = TipoTarifaEnum.ANUAL.equals(tarifaAnualDto.getTipoTarifa())
				? TipoReportEnum.TARIFA_CON_OCUPACION_HORIZONTAL.getNombreFicheroReport()
				: TipoReportEnum.TARIFA_CON_OCUPACION_HORIZONTAL_8MESES.getNombreFicheroReport();

		return this.informeService.imprimirInforme(parametros, fileNameToExport, fileReport);
	}

    private static String obtenerDatosAgrupacion(Artista artista) {
        StringBuilder sbDatosAgrupacion =  new StringBuilder();
        sbDatosAgrupacion.append("Número de componentes: ");
        sbDatosAgrupacion.append(artista.getComponentes());
        sbDatosAgrupacion.append("\n");

        if (artista.isEscenario()){
            sbDatosAgrupacion.append("Escenario: ");
            sbDatosAgrupacion.append(artista.getTipoEscenario().getNombre());
            sbDatosAgrupacion.append("\n");
            sbDatosAgrupacion.append("Medidas escenario: ");
            sbDatosAgrupacion.append(artista.getMedidasEscenario());
            sbDatosAgrupacion.append("\n");
        }

        return sbDatosAgrupacion.toString();
    }

    private static String obtenerDatosAgencia(Artista artista, String provincia) {
        StringBuilder sbDatosAgencia =  new StringBuilder();
        sbDatosAgencia.append("Provincia: ");
        sbDatosAgencia.append(provincia);
        sbDatosAgencia.append("\n");

        if (artista.getCondicionesContratacion()!=null && !artista.getCondicionesContratacion().isEmpty()){
            sbDatosAgencia.append("Condiciones de contratación: ");
            sbDatosAgencia.append(artista.getCondicionesContratacion());
            sbDatosAgencia.append("\n");
        }


        return sbDatosAgencia.toString();

    }

	@Override
    public List<TarifaArtistaCcaaDto> findTarifasByFechaAndNumComponentesArtista(Long idArtista, LocalDate fecha) {
		Artista artista = this.artistaRepository.findById(idArtista)
				.orElseThrow(() -> new RuntimeException("Artista no encontrado"));

		LocalDateTime fechaInicio = fecha.atStartOfDay();
		LocalDateTime fechaFin = fecha.atTime(LocalTime.of(23, 59, 59));

		return this.tarifaRepository.findTarifasByFechaAndNumeroComponentes(
				artista.getComponentes(),
				fechaInicio,
				fechaFin,
				idArtista
		);
	}

	@Override
	public ByteArrayOutputStream exportTarifaAnualToExcel(TarifaAnualDto tarifaAnualDto) {
		// Ejecutar consulta nativa
		List<Object[]> resultados = tarifaRepository.findTarifaAnualData(
			tarifaAnualDto.getIdArtista(),
			tarifaAnualDto.getAno(),
			tarifaAnualDto.getIdProvincia().intValue(),
			tarifaAnualDto.getConOcupacion()
		);

		// Convertir resultados a DTOs
		List<TarifaAnualExportDto> datos = resultados.stream()
			.map(this::mapToTarifaAnualExportDto)
			.collect(Collectors.toList());

		// Obtener información del artista y provincia
		Artista artista = artistaRepository.findById(tarifaAnualDto.getIdArtista())
			.orElseThrow(() -> new RuntimeException("Artista no encontrado"));

		String provincia = provinciaRepository.findById(tarifaAnualDto.getIdProvincia())
			.map(p -> p.getNombre())
			.orElse("N/A");

		// Crear información lateral
		List<String> sideInfo = new ArrayList<>();
		sideInfo.add("Artista: " + artista.getNombre());
		sideInfo.add("Agencia: " + artista.getAgencia().getNombre());
		sideInfo.add("Provincia: " + provincia);
		sideInfo.add("Año: " + tarifaAnualDto.getAno());
		sideInfo.add("Fecha de generación: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

		// Determinar qué columnas incluir según el tipo de tarifa
		List<String> fieldsToInclude = null;
		if (TipoTarifaEnum.OCHO_MESES.equals(tarifaAnualDto.getTipoTarifa())) {
			// Solo mostrar Marzo a Octubre (8 meses)
			fieldsToInclude = Arrays.asList(
				"marzo", "marzoValor",
				"abril", "abrilValor",
				"mayo", "mayoValor",
				"junio", "junioValor",
				"julio", "julioValor",
				"agosto", "agostoValor",
				"septiembre", "septiembreValor",
				"octubre", "octubreValor"
			);
		}
		// Si es ANUAL, fieldsToInclude = null mostrará todas las columnas

		// Exportar a Excel con información lateral
		return excelExportService.exportToExcelWithSideInfo(datos, TarifaAnualExportDto.class, artista.getNombre(), sideInfo, fieldsToInclude);
	}

	/**
	 * Mapea un array de objetos de la consulta nativa al DTO
	 */
	private TarifaAnualExportDto mapToTarifaAnualExportDto(Object[] row) {
		TarifaAnualExportDto dto = new TarifaAnualExportDto();

		dto.setNombre((String) row[0]);
		dto.setAgencia((String) row[1]);
		dto.setEnero((String) row[2]);
		dto.setEneroValor((String) row[3]);
		dto.setFebrero((String) row[4]);
		dto.setFebreroValor((String) row[5]);
		dto.setMarzo((String) row[6]);
		dto.setMarzoValor((String) row[7]);
		dto.setAbril((String) row[8]);
		dto.setAbrilValor((String) row[9]);
		dto.setMayo((String) row[10]);
		dto.setMayoValor((String) row[11]);
		dto.setJunio((String) row[12]);
		dto.setJunioValor((String) row[13]);
		dto.setJulio((String) row[14]);
		dto.setJulioValor((String) row[15]);
		dto.setAgosto((String) row[16]);
		dto.setAgostoValor((String) row[17]);
		dto.setSeptiembre((String) row[18]);
		dto.setSeptiembreValor((String) row[19]);
		dto.setOctubre((String) row[20]);
		dto.setOctubreValor((String) row[21]);
		dto.setNoviembre((String) row[22]);
		dto.setNoviembreValor((String) row[23]);
		dto.setDiciembre((String) row[24]);
		dto.setDiciembreValor((String) row[25]);

		return dto;
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

		final String userName = this.userService.obtenerUsuarioAutenticado().orElseThrow().getUsername();

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
