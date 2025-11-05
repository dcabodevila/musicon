package es.musicalia.gestmusica.listado;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.ajustes.AjustesDto;
import es.musicalia.gestmusica.ajustes.AjustesService;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.permiso.PermisoService;
import es.musicalia.gestmusica.util.DateUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping(value="listado")
public class ListadoController {

    private final LocalizacionService localizacionService;
    private final ListadoService listadoService;
    private final ArtistaService artistaService;
    private final AgenciaService agenciaService;
    private final AjustesService ajustesService;
    private final PermisoService permisoService;
    private final ObjectMapper objectMapper;

    public ListadoController(LocalizacionService localizacionService, ListadoService listadoService, ArtistaService artistaService,
                             AgenciaService agenciaService, AjustesService ajustesService, PermisoService permisoService, ObjectMapper objectMapper){

        this.localizacionService = localizacionService;
        this.listadoService = listadoService;
        this.artistaService = artistaService;
        this.agenciaService = agenciaService;
        this.ajustesService = ajustesService;
        this.permisoService = permisoService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public String listados(@AuthenticationPrincipal CustomAuthenticatedUser user,
                           Model model) {

        ListadoDto listado = new ListadoDto();
        listado.setSolicitadoPara(user.getUsuario().getNombreComercial()!=null? user.getUsuario().getNombreComercial() : user.getUsuario().getNombreCompleto());
        final Long idCcaa = this.localizacionService.findCcaaByUsuarioId(user.getUserId()).getId();
        final Long idProvincia = this.localizacionService.findProvinciaByUsuarioId(user.getUserId()).getId();
        listado.setIdCcaa(idCcaa);
        listado.setIdProvincia(idProvincia);
        listado.setIdTipoOcupacion(1L);
        model.addAttribute("listadoDto", listado);
        model.addAttribute("listaCcaa", this.localizacionService.findAllComunidades());
        model.addAttribute("listaProvinciasCcaaListado", this.localizacionService.findAllProvinciasByCcaaId(idCcaa));
        model.addAttribute("listaMunicipioListado", this.localizacionService.findAllMunicipiosByIdProvincia(idProvincia));
        model.addAttribute("listaTiposOcupacion", this.listadoService.findAllTiposOcupacion());
        model.addAttribute("listaTipoArtista", this.artistaService.listaTipoArtista());
        model.addAttribute("listaAgencias", this.agenciaService.listaAgenciasRecordActivasTarifasPublicas());
        model.addAttribute("listaAjustes", this.ajustesService.getAllAjustesByIdUsuario(user.getUserId()));


        final AjustesDto ajustesDto = this.ajustesService.getAjustesByIdUsuario(user.getUserId());

        if (ajustesDto != null) {
            // Convertir explícitamente de List a Set si es necesario
            if (ajustesDto.getIdsTipoArtista() != null) {
                listado.setIdsTipoArtista(new HashSet<>(ajustesDto.getIdsTipoArtista()));
            }
            if (ajustesDto.getIdsAgencias() != null) {
                listado.setIdsAgencias(new HashSet<>(ajustesDto.getIdsAgencias()));
            }
            if (ajustesDto.getIdsComunidades() != null) {
                listado.setIdsComunidades(new HashSet<>(ajustesDto.getIdsComunidades()));
            }
        }

        return "listados";
    }

    @GetMapping("/audiencia-listados")
    public String getAudienciaListados(@AuthenticationPrincipal CustomAuthenticatedUser user, Model model) {
        ListadoAudienciasDto listadoAudienciasDto = ListadoAudienciasDto.builder()
                .fechaDesde(LocalDate.now().minusMonths(2).withDayOfMonth(1))
                .fechaHasta(LocalDate.now())
                .build();
        List<ListadoRecord> listadosGenerados = this.listadoService.obtenerListadoEntreFechas(listadoAudienciasDto);
        try {
            List<ListadosPorMesDto> listadosPorMes = listadoService.obtenerListadosPorMes(listadosGenerados);
            List<Map<String, Object>> chartDataList = convertirListadosPorMesAMap(listadosPorMes);
            
            // Si no hay datos, crear un elemento con valor 0
            if (chartDataList.isEmpty()) {
                chartDataList = crearDatosVaciosParaGrafico();
            }
            
            String chartDataJson = objectMapper.writeValueAsString(chartDataList);
            model.addAttribute("chartData", chartDataJson);

        } catch (JsonProcessingException e) {
            log.error("Error consultando audiencia listados", e);
        }
        model.addAttribute("listadosGenerados", new ArrayList<ListadoRecord>());
        model.addAttribute("listadoAudienciasDto", listadoAudienciasDto);
        obtenerModelComun(model, user.getUserId());

        return "listados-generados";
    }

    private void obtenerModelComun(Model model, Long idUsuarioLogueado) {

        final Map<Long, Set<String>> mapPermisosAgencia = this.permisoService.obtenerMapPermisosAgencia(idUsuarioLogueado);

        model.addAttribute("listaAgencias", this.agenciaService.findMisAgencias(mapPermisosAgencia.keySet()));
    }

    @PostMapping("/generar")
    public ResponseEntity<?> generarListado(@AuthenticationPrincipal CustomAuthenticatedUser user, @ModelAttribute("listadoDto") @Valid ListadoDto listadoDto,
                                       BindingResult bindingResult) {
        try {
            // Validar errores de binding
            if (bindingResult.hasErrors()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Datos del formulario inválidos");
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
            }

            byte[] informeGenerado = this.listadoService.generarInformeListado(listadoDto, user.getUserId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String fileNameToExport = "Listado_"
                .concat(TipoOcupacionEnum.getDescripcionById(listadoDto.getIdTipoOcupacion()))
                .concat(DateUtils.getDateStr(new Date(), "ddMMyyyyHHmmss"))
                .concat(".pdf");

            headers.setContentDispositionFormData("attachment", fileNameToExport);
            headers.add("Content-Description", "File Transfer");
            headers.add("Content-Transfer-Encoding", "binary");
            headers.add("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
            headers.add("Pragma", "public");
            headers.setContentLength(informeGenerado.length);

            return new ResponseEntity<>(informeGenerado, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generando listado", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error al generar el presupuesto: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
        }
    }

    @PostMapping("/audiencias")
    public String procesarListadoAudiencias(@AuthenticationPrincipal CustomAuthenticatedUser user,@Valid @ModelAttribute ListadoAudienciasDto listadoAudienciasDto,
                                           BindingResult bindingResult,
                                           Model model,
                                           RedirectAttributes redirectAttributes) {

        final Long idUsuarioAutenticado = user.getUserId();

        try {

            // Validar errores de binding
            if (bindingResult.hasErrors()) {
                model.addAttribute("message", "Error en los datos del formulario");
                model.addAttribute("alertClass", "danger");
                model.addAttribute("listadosGenerados", new ArrayList<ListadoRecord>());
                obtenerModelComun(model, idUsuarioAutenticado);
                return "listados-generados";
            }

            // Validar que las fechas sean válidas
            if (listadoAudienciasDto.getFechaDesde() == null || listadoAudienciasDto.getFechaHasta() == null) {
                model.addAttribute("message", "Las fechas inicial y final son obligatorias");
                model.addAttribute("alertClass", "danger");
                model.addAttribute("listadosGenerados", new ArrayList<ListadoRecord>());
                obtenerModelComun(model, idUsuarioAutenticado);

                return "listados-generados";
            }

            if (listadoAudienciasDto.getFechaDesde().isAfter(listadoAudienciasDto.getFechaHasta())) {
                model.addAttribute("message", "La fecha inicial no puede ser posterior a la fecha final");
                model.addAttribute("alertClass", "danger");
                model.addAttribute("listadosGenerados", new ArrayList<ListadoRecord>());
                obtenerModelComun(model, idUsuarioAutenticado);

                return "listados-generados";
            }

            List<ListadoRecord> listados = this.listadoService.obtenerListadoEntreFechas(listadoAudienciasDto);

            model.addAttribute("listadosGenerados", new ArrayList<ListadoRecord>());
            List<ListadosPorMesDto> listadosPorMes = listadoService.obtenerListadosPorMes(listados);
            List<Map<String, Object>> chartDataList = convertirListadosPorMesAMap(listadosPorMes);
            
            // Si no hay datos, crear un elemento con valor 0
            if (chartDataList.isEmpty()) {
                chartDataList = crearDatosVaciosParaGrafico();
            }
            
            String chartDataJson = objectMapper.writeValueAsString(chartDataList);
            model.addAttribute("chartData", chartDataJson);

            obtenerModelComun(model, idUsuarioAutenticado);

            return "listados-generados";

        } catch (Exception e) {
            log.error("Error al procesar listado de audiencias", e);
            model.addAttribute("message", "Error al procesar la solicitud");
            model.addAttribute("alertClass", "danger");
            model.addAttribute("listadosGenerados", new ArrayList<ListadoRecord>());

        }

        obtenerModelComun(model, idUsuarioAutenticado);
        return "listados-generados";
    }

    @PostMapping("/audiencias/data")
    @ResponseBody
    public Map<String, Object> getListadosData(
            @AuthenticationPrincipal CustomAuthenticatedUser user,
            @RequestParam(value = "draw", defaultValue = "1") int draw,
            @RequestParam(value = "start", defaultValue = "0") int start,
            @RequestParam(value = "length", defaultValue = "10") int length,
            @RequestParam(value = "idAgencia", required = false) Long idAgencia,
            @RequestParam(value = "fechaDesde", required = false) String fechaDesdeStr,
            @RequestParam(value = "fechaHasta", required = false) String fechaHastaStr,
            @RequestParam(value = "search[value]", required = false) String searchValue,
            @RequestParam(value = "order[0][column]", defaultValue = "0") int orderColumn,
            @RequestParam(value = "order[0][dir]", defaultValue = "desc") String orderDir) {

    try {
        // Calcular página correctamente - VALIDAR que sea >= 0
        int page = Math.max(0, start / length);
        
        log.info("Parámetros DataTables - start: {}, length: {}, página calculada: {}", start, length, page);

        // IMPORTANTE: Crear Sort explícitamente ANTES del Pageable
        Sort sort = Sort.by(Sort.Direction.DESC, "fechaCreacion");
        Pageable pageable = PageRequest.of(page, length, sort);

        // Parsear fechas
            LocalDate fechaDesde = null;
            LocalDate fechaHasta = null;
        
            if (fechaDesdeStr != null && !fechaDesdeStr.trim().isEmpty()) {
                fechaDesde = DateUtils.parseLocalDate(fechaDesdeStr, "dd-MM-yyyy");
            }
        
            if (fechaHastaStr != null && !fechaHastaStr.trim().isEmpty()) {
                fechaHasta = DateUtils.parseLocalDate(fechaHastaStr, "dd-MM-yyyy");
            }
        
            // Llamar al servicio con los filtros y paginación
            ListadoAudienciasDto filtros = ListadoAudienciasDto.builder()
                    .idAgencia(idAgencia)
                    .fechaDesde(fechaDesde)
                    .fechaHasta(fechaHasta)
                    .build();
                    
            Page<ListadoRecord> pageResult = this.listadoService.obtenerListadoEntreFechasPaginado(
                filtros, searchValue, pageable, user.getUserId());
        
            log.info("Resultado: Total={}, En esta página={}, Página actual={}", 
                     pageResult.getTotalElements(), pageResult.getContent().size(), pageResult.getNumber());
        
            // Preparar respuesta en formato DataTables
            Map<String, Object> response = new HashMap<>();
            response.put("draw", draw);
            response.put("recordsTotal", pageResult.getTotalElements());
            response.put("recordsFiltered", pageResult.getTotalElements());
            response.put("data", pageResult.getContent());
        
            return response;
        
        } catch (Exception e) {
            log.error("Error obteniendo datos paginados para listados", e);
        
            // Respuesta de error para DataTables
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("draw", draw);
            errorResponse.put("recordsTotal", 0);
            errorResponse.put("recordsFiltered", 0);
            errorResponse.put("data", new ArrayList<>());
            errorResponse.put("error", "Error al cargar los datos: " + e.getMessage());
        
            return errorResponse;
        }
    }
    
    @PostMapping("/audiencias/chart-data")
    @ResponseBody
    public Map<String, Object> getChartData(
            @AuthenticationPrincipal CustomAuthenticatedUser user,
            @RequestParam(value = "idAgencia", required = false) Long idAgencia,
            @RequestParam(value = "fechaDesde", required = false) String fechaDesdeStr,
            @RequestParam(value = "fechaHasta", required = false) String fechaHastaStr) {
        
        try {
            // Parsear fechas
            LocalDate fechaDesde = null;
            LocalDate fechaHasta = null;
            
            if (fechaDesdeStr != null && !fechaDesdeStr.trim().isEmpty()) {
                fechaDesde = DateUtils.parseLocalDate(fechaDesdeStr, "dd-MM-yyyy");
            }
            
            if (fechaHastaStr != null && !fechaHastaStr.trim().isEmpty()) {
                fechaHasta = DateUtils.parseLocalDate(fechaHastaStr, "dd-MM-yyyy");
            }
            
            // Crear filtros
            ListadoAudienciasDto filtros = ListadoAudienciasDto.builder()
                    .idAgencia(idAgencia)
                    .fechaDesde(fechaDesde)
                    .fechaHasta(fechaHasta)
                    .build();
                    
            // Obtener listados para el gráfico
            List<ListadoRecord> listados = this.listadoService.obtenerListadoEntreFechas(filtros);
            
            // Generar datos del gráfico
            List<Map<String, Object>> chartData = convertirListadosPorMesAMap(listadoService.obtenerListadosPorMes(listados));
            if (chartData.isEmpty()) {
                chartData = crearDatosVaciosParaGrafico();
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("chartData", chartData);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error obteniendo datos del gráfico", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error al obtener datos del gráfico: " + e.getMessage());
            errorResponse.put("chartData", new ArrayList<>());
            
            return errorResponse;
        }
    }

    private List<Map<String, Object>> crearDatosVaciosParaGrafico() {
        List<Map<String, Object>> datosVacios = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("mes", "Sin datos");
        item.put("cantidad", 0);
        datosVacios.add(item);
        return datosVacios;
    }

    private List<Map<String, Object>> convertirListadosPorMesAMap(List<ListadosPorMesDto> listadosPorMes) {
        return listadosPorMes.stream()
                .map(dto -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("mes", dto.getMes());
                    item.put("cantidad", dto.getCantidad());
                    return item;
                })
                .collect(Collectors.toList());
    }

    private String getColumnName(int column) {
        switch (column) {
            case 0: return "fechaCreacion";
            case 1: return "nombreRepresentante";
            case 2: return "solicitadoPara";
            case 3: return "municipio";
            case 4: return "localidad";
            case 5: return "fechaInicio"; // Para las fechas del presupuesto
            default: return "fechaCreacion";
        }
    }

}