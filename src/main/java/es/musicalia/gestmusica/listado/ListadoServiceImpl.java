package es.musicalia.gestmusica.listado;

import es.musicalia.gestmusica.accesoartista.AccesoArtista;
import es.musicalia.gestmusica.accesoartista.AccesoArtistaRepository;
import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.agencia.AgenciaRepository;
import es.musicalia.gestmusica.artista.ArtistaRecord;
import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.informe.InformeService;
import es.musicalia.gestmusica.localizacion.CodigoNombreDto;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.MunicipioRepository;
import es.musicalia.gestmusica.localizacion.ProvinciaRepository;
import es.musicalia.gestmusica.permiso.PermisoArtistaEnum;
import es.musicalia.gestmusica.permiso.PermisoRecord;
import es.musicalia.gestmusica.permiso.PermisoRepository;
import es.musicalia.gestmusica.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
@Slf4j
@Service
@Transactional(readOnly = true)
public class ListadoServiceImpl implements ListadoService {

    private final InformeService informeService;
    private final ProvinciaRepository provinciaRepository;
    private final MunicipioRepository municipioRepository;
    private final ListadoMapper listadoMapper;
    private final ListadoRepository listadoRepository;
    private final AgenciaRepository agenciaRepository;
    private final ArtistaRepository artistaRepository;
    private final AccesoArtistaRepository accesoArtistaRepository;
    private final PermisoRepository permisoRepository;

    public ListadoServiceImpl(InformeService informeService, ProvinciaRepository provinciaRepository, MunicipioRepository municipioRepository, ListadoMapper listadoMapper, ListadoRepository listadoRepository, AgenciaRepository agenciaRepository, ArtistaRepository artistaRepository, AccesoArtistaRepository accesoArtistaRepository, PermisoRepository permisoRepository) {
        this.informeService = informeService;
        this.provinciaRepository = provinciaRepository;
        this.municipioRepository = municipioRepository;
        this.listadoMapper = listadoMapper;
        this.listadoRepository = listadoRepository;
        this.agenciaRepository = agenciaRepository;
        this.artistaRepository = artistaRepository;
        this.accesoArtistaRepository = accesoArtistaRepository;
        this.permisoRepository = permisoRepository;
    }

    @Override
    public List<ListadosPorMesDto> obtenerListadosPorMes(List<ListadoRecord> listados) {

        // Nombres de meses en español
        String[] nombresMeses = {
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        };

        // Agrupar por mes y contar
        Map<String, Long> contadorPorMes = listados.stream()
                .collect(Collectors.groupingBy(
                        listado -> {
                            LocalDateTime fechaCreacion = listado.fechaCreacion();
                            int mes = fechaCreacion.getMonthValue();
                            int ano = fechaCreacion.getYear();
                            return nombresMeses[mes - 1] + " " + ano;
                        },
                        Collectors.counting()
                ));

        // Convertir a lista y ordenar por fecha
        return contadorPorMes.entrySet().stream()
                .map(entry -> new ListadosPorMesDto(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> {
                    // Ordenar por año y mes
                    String[] partsA = a.getMes().split(" ");
                    String[] partsB = b.getMes().split(" ");
                    int añoA = Integer.parseInt(partsA[1]);
                    int añoB = Integer.parseInt(partsB[1]);
                    if (añoA != añoB) {
                        return Integer.compare(añoA, añoB);
                    }
                    // Si mismo año, ordenar por mes
                    int mesA = Arrays.asList(nombresMeses).indexOf(partsA[0]);
                    int mesB = Arrays.asList(nombresMeses).indexOf(partsB[0]);
                    return Integer.compare(mesA, mesB);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CodigoNombreDto> findAllTiposOcupacion() {
        return Arrays.stream(TipoOcupacionEnum.values())
                .map(tipo -> new CodigoNombreDto(tipo.getId(), tipo.getDescripcion()))
                .collect(Collectors.toList());
    }

    private String convertSetLongToString(Set<Long> setIds) {
        if (setIds == null || setIds.isEmpty()) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();
        Iterator<Long> iterator = setIds.iterator();

        while (iterator.hasNext()) {
            stringBuilder.append(iterator.next());
            if (iterator.hasNext()) {
                stringBuilder.append(", ");
            }
        }

        return stringBuilder.toString();
    }

    // Actualizar las llamadas en el método generarInformeListado
    @Transactional
    @Override
    public byte[] generarInformeListado(ListadoDto listadoDto, Long idUsuario) {

        log.info("Generando listado");
        Map<String, Object> parametros = new HashMap<String, Object>();

        String fileNameToExport = "Listado_".concat(TipoOcupacionEnum.getDescripcionById(listadoDto.getIdTipoOcupacion())).concat(DateUtils.getDateStr(new Date(), "ddMMyyyyHHmmss")).concat(".pdf");

        List<LocalDate> dateList = sortDates(listadoDto.getFecha1(), listadoDto.getFecha2(), listadoDto.getFecha3(), listadoDto.getFecha4(), listadoDto.getFecha5(), listadoDto.getFecha6(), listadoDto.getFecha7());

        parametros.put("colNames", getColNamesListadoFechas());
        parametros.put("fechaListIn", getFechaListFechas(listadoDto.getFechaDesde(), listadoDto.getFechaHasta(), dateList));
        parametros.put("idProvincia", listadoDto.getIdProvincia().intValue());
        parametros.put("observaciones", listadoDto.getComentario());
        parametros.put("solicitante", listadoDto.getSolicitadoPara());
        parametros.put("provincia", this.provinciaRepository.findById(listadoDto.getIdProvincia()).orElseThrow().getNombre());
        parametros.put("municipio", listadoDto.getIdMunicipio() != null ? this.municipioRepository.findById(listadoDto.getIdMunicipio()).orElseThrow().getNombre() : "");
        parametros.put("lugar", listadoDto.getLocalidad() != null ? listadoDto.getLocalidad() : "");
        parametros.put("idsTipoArtista", convertSetLongToString(listadoDto.getIdsTipoArtista()));
        parametros.put("idsAgencias", convertSetLongToString(listadoDto.getIdsAgencias()));
        parametros.put("idsComunidades", convertSetLongToString(listadoDto.getIdsComunidades()));
        parametros.put("idsArtistaRestringidos", obtenerIdsArtistaRestringidos(idUsuario));


        List<Map.Entry<String, String>> diaList = generateDiaListFechas(listadoDto.getFechaDesde(), listadoDto.getFechaHasta(), dateList);
        // Agregar cada par clave-valor de la lista al mapa de parámetros
        for (Map.Entry<String, String> entry : diaList) {
            parametros.put(entry.getKey(), entry.getValue());
        }
        List<Map.Entry<String, String>> listFechas = generateListFechas(listadoDto.getFechaDesde(), listadoDto.getFechaHasta(), dateList);
        for (Map.Entry<String, String> entry : listFechas) {
            parametros.put(entry.getKey(), entry.getValue());
        }

        final boolean isReportVertical = diaList.size() <= 8;

        TipoReportEnum tipoReport = TipoOcupacionEnum.SIN_OCUPACION.getId().equals(listadoDto.getIdTipoOcupacion()) ? (isReportVertical ? TipoReportEnum.LISTADO_SIN_OCUPACION_VERTICAL : TipoReportEnum.LISTADO_SIN_OCUPACION_HORIZONTAL) : (isReportVertical ? TipoReportEnum.LISTADO_CON_OCUPACION_VERTICAL : TipoReportEnum.LISTADO_CON_OCUPACION_HORIZONTAL);
        parametros.put("titulo", tipoReport.getTitulo());

        byte[] listado = this.informeService.imprimirInforme(parametros, fileNameToExport, tipoReport.getNombreFicheroReport());

        guardarListadoEntity(listadoDto);

        return listado;
    }

    private String obtenerIdsArtistaRestringidos(Long idUsuario) {
        final PermisoRecord permisoRecord = this.permisoRepository.findPermisoRecordByCodigo(PermisoArtistaEnum.PRESUPUESTOS_RESTRINGIDOS.name());

        Optional<List<AccesoArtista>> optionalAccesosArtista = this.accesoArtistaRepository.findAllAccesosArtistaByIdUsuarioIdPermiso(idUsuario, permisoRecord.id());
        Set<Long> idsArtistaRestringidos = new HashSet<>();
        if (optionalAccesosArtista.isPresent() && !optionalAccesosArtista.get().isEmpty()) {
            idsArtistaRestringidos = optionalAccesosArtista.get().stream()
                    .map(accesoArtista -> accesoArtista.getArtista().getId())
                    .collect(Collectors.toSet());
        }

        if (idsArtistaRestringidos.isEmpty()) {
            return "-1";
        }

        return idsArtistaRestringidos.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    // Método privado para mapear de Entity a Record
    private ListadoRecord mapToListadoRecord(Listado listado) {
        return new ListadoRecord(
                listado.getId(),
                listado.getSolicitadoPara(),
                listado.getUsuario().getNombreComercial() != null ? listado.getUsuario().getNombreComercial() : listado.getUsuario().getNombre() + " " + listado.getUsuario().getApellidos(),
                listado.getLocalidad(),
                listado.getMunicipio().getNombre(),
                listado.getUsuario().getNombre(),
                listado.getUsuario().getApellidos(),
                listado.getTipoOcupacion() != null ? listado.getTipoOcupacion().name() : null,
                listado.getFechaCreacion(),
                listado.getFecha1(),
                listado.getFecha2(),
                listado.getFecha3(),
                listado.getFecha4(),
                listado.getFecha5(),
                listado.getFecha6(),
                listado.getFecha7(),
                listado.getFechaDesde(),
                listado.getFechaHasta()
        );
    }


    private void guardarListadoEntity(ListadoDto listadoDto) {
        Listado listadoEntity = this.listadoMapper.toEntity(listadoDto);

        listadoEntity.setUsuario(((CustomAuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUsuario());

        // Cargar municipio
        Municipio municipio = municipioRepository.findById(listadoDto.getIdMunicipio())
                .orElseThrow(() -> new IllegalArgumentException("Municipio no encontrado con ID: " + listadoDto.getIdMunicipio()));
        listadoEntity.setMunicipio(municipio);


        // Cargar agencias si existen
        if (listadoDto.getIdsAgencias() != null && !listadoDto.getIdsAgencias().isEmpty()) {
            Set<Agencia> agencias = new HashSet<>(agenciaRepository.findAllById(listadoDto.getIdsAgencias()));
            listadoEntity.setAgencias(agencias);
        }

        listadoEntity.setArtistas(this.artistaRepository.findArtistasByComunidadesAndTipos(listadoDto.getIdsComunidades(), listadoDto.getIdsTipoArtista()));
        listadoEntity.setActivo(true);
        this.listadoRepository.save(listadoEntity);
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
        log.info(colNamesBuilder.toString());
        return colNamesBuilder.toString();

    }


    private String getColNamesListadoFechas() {

        StringBuilder colNamesBuilder = new StringBuilder();
        colNamesBuilder.append("\"Artista\" text, \"Agencia\" text, \"Telefono\" text, \"Telefono2\" text,\"Telefono3\" text,\"Componentes\" text,\"Escenario\" text,");
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

    private String convertListLongToString(List<Long> listIds) {
        StringBuilder dateListBuilder = new StringBuilder();
        int index = 1;
        for (Long id : listIds) {

            dateListBuilder.append(id);

            if (index <= listIds.size() - 1) {
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
        final boolean isReportVertical = diaList.size() <= 8;
        int maxDias = isReportVertical ? 8 : 16;
        while (diaList.size() < maxDias) {
            diaList.add(new AbstractMap.SimpleEntry<>("dia" + (diaList.size() + 1), ""));
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

        final boolean isReportVertical = diaList.size() <= 8;
        int maxDias = isReportVertical ? 8 : 16;
        while (diaList.size() < maxDias) {
            diaList.add(new AbstractMap.SimpleEntry<>("dia" + (diaList.size() + 1), ""));
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


    // Método base que contiene la lógica correcta
    private Specification<Listado> crearSpecificationListados(ListadoAudienciasDto filtros) {
        Specification<Listado> spec = null;
        if (filtros.getIdAgencia() == null) {
            spec = ListadoSpecifications.findListadosByAgenciaAndFechas(
                    null,
                    filtros.getFechaDesde().atStartOfDay(),
                    filtros.getFechaHasta().plusDays(1).atStartOfDay().minusNanos(1)
            );

        } else {

            Set<Long> idsArtistaAgencia = this.artistaRepository.findAllArtistasRecordByIdAgencia(filtros.getIdAgencia()).stream()
                    .map(ArtistaRecord::id)
                    .collect(Collectors.toSet());

            spec = ListadoSpecifications.findListadosByAgenciaAndFechasAndComunidades(
                    filtros.getIdAgencia(),
                    filtros.getFechaDesde().atStartOfDay(),
                    filtros.getFechaHasta().plusDays(1).atStartOfDay().minusNanos(1), idsArtistaAgencia
            );

        }
        return spec;
    }

    // Actualizar obtenerListadoEntreFechas para usar el método base
    public List<ListadoRecord> obtenerListadoEntreFechas(ListadoAudienciasDto filtros) {
        Specification<Listado> spec = crearSpecificationListados(filtros);

        List<Listado> listados = listadoRepository.findAll(spec,
                Sort.by(Sort.Direction.DESC, "fechaCreacion"));

        return listados.stream()
                .map(this::mapToListadoRecord)
                .collect(Collectors.toList());
    }

    public Page<ListadoRecord> obtenerListadoEntreFechasPaginado(
            ListadoAudienciasDto filtros,
            String searchValue,
            Pageable pageable,
            Long idUsuario) {

        log.info("Ejecutando findAll y paginando manualmente - Página: {}, Tamaño: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        // Usar la MISMA specification que funciona sin paginación
        Specification<Listado> spec = crearSpecificationListados(filtros);

        // Obtener TODOS los registros con ordenamiento
        List<Listado> todosLosListados = listadoRepository.findAll(spec,
                Sort.by(Sort.Direction.DESC, "fechaCreacion"));

        log.info("Total de registros obtenidos: {}", todosLosListados.size());

        // Convertir TODOS a ListadoRecord
        List<ListadoRecord> todosLosRecords = todosLosListados.stream()
                .map(this::mapToListadoRecord)
                .collect(Collectors.toList());

        // Calcular el subset para la página solicitada
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        int start = pageNumber * pageSize;
        int end = Math.min(start + pageSize, todosLosRecords.size());

        log.info("Calculando subset - Página: {}, Inicio: {}, Fin: {}, Total: {}",
                pageNumber, start, end, todosLosRecords.size());

        // Extraer el subset correspondiente a esta página
        List<ListadoRecord> subsetPagina = new ArrayList<>();
        if (start < todosLosRecords.size()) {
            subsetPagina = todosLosRecords.subList(start, end);
        }

        log.info("Devolviendo subset - Elementos en esta página: {}, Total elementos: {}",
                subsetPagina.size(), todosLosRecords.size());

        // Crear Page con el subset pero manteniendo el total real
        return new PageImpl<>(subsetPagina, pageable, todosLosRecords.size());
    }


}