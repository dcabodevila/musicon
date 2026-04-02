package es.musicalia.gestmusica.ocupacion;


import es.musicalia.gestmusica.agencia.AgenciaRecord;
import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.artista.ArtistaDto;
import es.musicalia.gestmusica.artista.ArtistaRecord;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventNames;
import es.musicalia.gestmusica.observabilidad.FunctionalEventOutcome;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.util.DateUtils;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping(value="ocupacion")
public class OcupacionController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final OcupacionService ocupacionService;
    private final AgenciaService agenciaService;
    private final ArtistaService artistaService;
    private final LocalizacionService localizacionService;
    private final UserService userService;
    private final FunctionalEventTracker functionalEventTracker;

    public OcupacionController(OcupacionService ocupacionService, AgenciaService agenciaService, ArtistaService artistaService, LocalizacionService localizacionService, UserService userService, FunctionalEventTracker functionalEventTracker){
        this.ocupacionService = ocupacionService;

        this.agenciaService = agenciaService;
        this.artistaService = artistaService;
        this.localizacionService = localizacionService;
        this.userService = userService;
        this.functionalEventTracker = functionalEventTracker;
    }


    @GetMapping("/{id}")
    public String getOcupacionModelAndView(@AuthenticationPrincipal CustomAuthenticatedUser user ,Model model, @PathVariable long id) {
        final OcupacionSaveDto ocupacion = ocupacionService.getOcupacionSaveDto(id);
        obtenerModelAttributeComun(user, model, ocupacion);
        model.addAttribute("listaArtistas", List.of(this.artistaService.findArtistaDtoById(ocupacion.getIdArtista())));
        return "ocupacion-detail";

    }

    @GetMapping("/nueva/{idArtista}")
    public String getNuevaOcupacionModelAndView(@AuthenticationPrincipal CustomAuthenticatedUser user ,Model model, @PathVariable long idArtista) {

        if (user.getMapPermisosArtista().containsKey(idArtista) && user.getMapPermisosArtista().get(idArtista).contains("OCUPACIONES")) {

            final OcupacionSaveDto ocupacion = getNewOcupacionSaveDto(idArtista, user.getUserId() );
            obtenerModelAttributeComun(user, model, ocupacion);
            List<ArtistaRecord> listaArtistasPermisosOcupacion = this.artistaService.findMisArtistas(obtenerArtistasConPermisoOcupaciones(user.getMapPermisosArtista()));
            model.addAttribute("listaArtistas", listaArtistasPermisosOcupacion);
        }
        else {
            throw new org.springframework.security.access.AccessDeniedException("No tiene permisos para crear ocupaciones para este artista");
        }

        return "ocupacion-detail";

    }

    private OcupacionSaveDto getNewOcupacionSaveDto(long idArtista, Long idUsuario) {
        ArtistaDto artista = this.artistaService.findArtistaDtoById(idArtista);
        final OcupacionSaveDto ocupacion = new OcupacionSaveDto();
        ocupacion.setIdArtista(idArtista);
        ocupacion.setIdAgencia(artista.getIdAgencia());
        ocupacion.setIdCcaa(this.localizacionService.findCcaaByUsuarioId(idUsuario).getId());
        ocupacion.setIdProvincia(this.localizacionService.findProvinciaByUsuarioId(idUsuario).getId());
        ocupacion.setImporte(BigDecimal.ZERO);
        ocupacion.setPorcentajeRepre(BigDecimal.ZERO);
        ocupacion.setIva(BigDecimal.ZERO);
        ocupacion.setIdUsuario(this.userService.obtenerUsuarioAutenticado().get().getId());
        return ocupacion;
    }

    private void obtenerModelAttributeComun(CustomAuthenticatedUser user, Model model, OcupacionSaveDto ocupacion) {
        model.addAttribute("ocupacionDto", ocupacion);

        model.addAttribute("listaCcaa", this.localizacionService.findAllComunidades());

        final List<CodigoNombreRecord> listaProvincias = this.localizacionService.findAllProvinciasByCcaaId(ocupacion.getIdCcaa());
        model.addAttribute("listaProvinciasCcaaArtista", listaProvincias);
        final List<CodigoNombreRecord> listaMunicipios = ocupacion.getIdProvincia() != null ? this.localizacionService.findAllMunicipiosByIdProvincia(ocupacion.getIdProvincia()): this.localizacionService.findAllMunicipiosByIdProvincia(listaProvincias.get(0).id());
        model.addAttribute("listaMunicipioListado",  listaMunicipios);
        model.addAttribute("listaLocalidadesListado", ocupacion.getIdMunicipio() != null ? this.localizacionService.findLocalidadByIdMunicipio(ocupacion.getIdMunicipio()): this.localizacionService.findLocalidadByIdMunicipio(listaMunicipios.get(0).id()) );

        model.addAttribute("listaTiposOcupacion", this.ocupacionService.listarTiposOcupacion(ocupacion.getIdArtista()));
        final ArtistaDto artista = this.artistaService.findArtistaDtoById(ocupacion.getIdArtista());
        boolean isArtistaPermiteOdg = artista.isPermiteOrquestasDeGalicia();
        boolean isArtistaPublicarEventos = Boolean.TRUE.equals(artista.getPublicarEventos());
        model.addAttribute("isArtistaPermiteOrquestasDeGalicia", isArtistaPermiteOdg);
        model.addAttribute("isArtistaPermiteOdg", isArtistaPermiteOdg);
        model.addAttribute("isArtistaPublicarEventos", isArtistaPublicarEventos);
        model.addAttribute("listaUsuarios", this.userService.findAllUsuarioRecordsNotAdmin());
        model.addAttribute("idUsuarioAutenticado", this.userService.isUserAutheticated()? this.userService.obtenerUsuarioAutenticado().get().getId() : null);
    }

    public Set<Long> obtenerArtistasConPermisoOcupaciones(Map<Long, Set<String>> mapPermisosArtista) {
        return mapPermisosArtista.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().contains("OCUPACIONES"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<OcupacionEditDto> getOcupacionEditDtoByArtistaIdAndDates(@PathVariable long id) {

        return ResponseEntity.ok(ocupacionService.findOcupacionEditDtoByArtistaIdAndDates(id));

    }

    @GetMapping("/anular/{id}")
    public ResponseEntity<DefaultResponseBody> anularOcupacion(@AuthenticationPrincipal CustomAuthenticatedUser user, @PathVariable long id) {

        DefaultResponseBody response = this.ocupacionService.anularOcupacion(id);

        functionalEventTracker.track(
                FunctionalEventNames.OCUPACION_CANCELLED,
                response.isSuccess() ? FunctionalEventOutcome.SUCCESS : FunctionalEventOutcome.FAILURE,
                user != null ? user.getUserId() : null,
                user != null && user.getUsuario() != null ? user.getUsuario().getUsername() : null,
                Map.of("ocupacion_id", id)
        );

        return ResponseEntity.ok(response);

    }

    @GetMapping("/confirmar/{id}")
    public ResponseEntity<DefaultResponseBody> confirmarOcupacion(@AuthenticationPrincipal CustomAuthenticatedUser user, @PathVariable long id) {

        DefaultResponseBody response = this.ocupacionService.confirmarOcupacion(id);

        functionalEventTracker.track(
                FunctionalEventNames.OCUPACION_CONFIRMED,
                response.isSuccess() ? FunctionalEventOutcome.SUCCESS : FunctionalEventOutcome.FAILURE,
                user != null ? user.getUserId() : null,
                user != null && user.getUsuario() != null ? user.getUsuario().getUsername() : null,
                Map.of("ocupacion_id", id)
        );

        return ResponseEntity.ok(response);

    }

    @PostMapping("/save")
    public ResponseEntity<?> saveOcupacion(
            @AuthenticationPrincipal CustomAuthenticatedUser user,
            @RequestBody OcupacionSaveDto ocupacionSaveDto) {

        final boolean isCreacion = ocupacionSaveDto.getId() == null;

        if (ocupacionService.existeOcupacionFecha(ocupacionSaveDto)) {
            if (isCreacion) {
                functionalEventTracker.track(
                        FunctionalEventNames.OCUPACION_CREATED,
                        FunctionalEventOutcome.FAILURE,
                        user != null ? user.getUserId() : null,
                        user != null && user.getUsuario() != null ? user.getUsuario().getUsername() : null,
                        Map.of("reason", "ocupacion_duplicada")
                );
            }
            return ResponseEntity.ok(DefaultResponseBody.builder().success(false).message("Ya existe una ocupación en esa fecha").messageType("error").build());
        }

        try {
            DefaultResponseBody response = ocupacionService.saveOcupacion(ocupacionSaveDto);
            if (isCreacion) {
                functionalEventTracker.track(
                        FunctionalEventNames.OCUPACION_CREATED,
                        response.isSuccess() ? FunctionalEventOutcome.SUCCESS : FunctionalEventOutcome.FAILURE,
                        user != null ? user.getUserId() : null,
                        user != null && user.getUsuario() != null ? user.getUsuario().getUsername() : null,
                        Map.of("ocupacion_id", response.getIdEntidad() != null ? response.getIdEntidad() : "")
                );
            }
            return ResponseEntity.ok(response);
        }
        catch (ModificacionOcupacionException e) {
            if (isCreacion) {
                functionalEventTracker.track(
                        FunctionalEventNames.OCUPACION_CREATED,
                        FunctionalEventOutcome.FAILURE,
                        user != null ? user.getUserId() : null,
                        user != null && user.getUsuario() != null ? user.getUsuario().getUsername() : null,
                        Map.of("reason", "sin_permisos")
                );
            }
            return ResponseEntity.ok(DefaultResponseBody.builder().success(false).message("No tiene permisos para modificar la ocupación de otros usuarios").messageType("error").build());
        }
        catch (Exception e) {
            if (isCreacion) {
                functionalEventTracker.track(
                        FunctionalEventNames.OCUPACION_CREATED,
                        FunctionalEventOutcome.FAILURE,
                        user != null ? user.getUserId() : null,
                        user != null && user.getUsuario() != null ? user.getUsuario().getUsername() : null,
                        Map.of("reason", "unexpected_error")
                );
            }
            return ResponseEntity.ok(DefaultResponseBody.builder().success(false).message("Error inesperado al guardar").messageType("error").build());
        }

    }

    @GetMapping("/list")
    public String getListadoOcupaciones(@AuthenticationPrincipal CustomAuthenticatedUser user, Model model){

        getModelAttributeComunOcupacionList(user, model);

        return "ocupaciones";
    }
    @PostMapping("/list")
    public String postListadoOcupaciones(@AuthenticationPrincipal CustomAuthenticatedUser user,
                                         @ModelAttribute OcupacionListFilterDto ocupacionListFilterDto,
                                         Model model, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("ocupacionListFilterDto", ocupacionListFilterDto);

        return "redirect:/ocupacion/list";

    }

    @PostMapping("/list/data")
    @ResponseBody
    public Map<String, Object> getListadoOcupacionesData(
            @AuthenticationPrincipal CustomAuthenticatedUser user,
            @RequestParam(value = "draw", defaultValue = "1") int draw,
            @RequestParam(value = "start", defaultValue = "0") int start,
            @RequestParam(value = "length", defaultValue = "10") int length,
            @RequestParam(value = "idAgencia", required = false) String idAgenciaStr,
            @RequestParam(value = "idArtista", required = false) String idArtistaStr,
            @RequestParam(value = "fechaDesde", required = false) String fechaDesdeStr,
            @RequestParam(value = "fechaHasta", required = false) String fechaHastaStr,
            @RequestParam(value = "search[value]", required = false) String searchValue,
            @RequestParam(value = "order[0][column]", defaultValue = "1") int orderColumn,
            @RequestParam(value = "order[0][dir]", defaultValue = "desc") String orderDir
    ) {
        try {
            int pageSize = length > 0 ? length : 10;
            int page = Math.max(0, start / pageSize);
            Sort sort = Sort.by("desc".equalsIgnoreCase(orderDir) ? Sort.Direction.DESC : Sort.Direction.ASC, getSortColumnName(orderColumn));
            Pageable pageable = PageRequest.of(page, pageSize, sort);
            Long idAgencia = parseLongValue(idAgenciaStr);
            Long idArtista = parseLongValue(idArtistaStr);
            LocalDate fechaDesde = parseDateValue(fechaDesdeStr);
            LocalDate fechaHasta = parseDateValue(fechaHastaStr);

            OcupacionListFilterDto filtros = OcupacionListFilterDto.builder()
                    .idAgencia(idAgencia)
                    .idArtista(idArtista)
                    .fechaDesde(fechaDesde != null ? fechaDesde : LocalDate.now())
                    .fechaHasta(fechaHasta)
                    .build();

            Page<OcupacionListRecord> pageResult = this.ocupacionService.findOcupacionesByArtistasListAndDatesActivoPaginado(
                    user, filtros, searchValue, pageable
            );
            Page<OcupacionListRecord> pageTotal = this.ocupacionService.findOcupacionesByArtistasListAndDatesActivoPaginado(
                    user, filtros, null, PageRequest.of(0, 1)
            );

            List<Map<String, Object>> rows = pageResult.getContent().stream()
                    .map(this::toDataTableRow)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("draw", draw);
            response.put("recordsTotal", pageTotal.getTotalElements());
            response.put("recordsFiltered", pageResult.getTotalElements());
            response.put("data", rows);
            return response;
        } catch (Exception e) {
            log.error("Error obteniendo ocupaciones paginadas", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("draw", draw);
            errorResponse.put("recordsTotal", 0);
            errorResponse.put("recordsFiltered", 0);
            errorResponse.put("data", new ArrayList<>());
            errorResponse.put("error", "Error al cargar las ocupaciones");
            return errorResponse;
        }
    }


    @PostMapping("/publicar-odg/{idOcupacion}")
    public ResponseEntity<DefaultResponseBody> publicarEnOrquestasDeGalicia(@PathVariable long idOcupacion) {
        try {

            return ResponseEntity.ok(this.ocupacionService.publicarOcupacionOrquestasDeGalicia(idOcupacion));

        } catch (Exception e) {
            return ResponseEntity.ok(DefaultResponseBody.builder()
                    .success(false)
                    .message("Error al publicar en OrquestasDeGalicia: " + e.getMessage())
                    .messageType("error")
                    .build());
        }
    }

    @PostMapping("/actualizar-odg/{idOcupacion}")
    public ResponseEntity<DefaultResponseBody> actualizarEnOrquestasDeGalicia(@PathVariable long idOcupacion) {
        try {
            return ResponseEntity.ok(this.ocupacionService.actualizarOcupacionOrquestasDeGalicia(idOcupacion));
        } catch (OrquestasDeGaliciaException e) {
            return ResponseEntity.ok(DefaultResponseBody.builder()
                    .success(false)
                    .message("Error al actualizar en OrquestasDeGalicia: " + e.getMessage())
                    .messageType("error")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.ok(DefaultResponseBody.builder()
                    .success(false)
                    .message("Error al actualizar en OrquestasDeGalicia: " + e.getMessage())
                    .messageType("error")
                    .build());
        }
    }

    @PostMapping("/eliminar-odg/{idOcupacion}")
    public ResponseEntity<DefaultResponseBody> eliminarDeOrquestasDeGalicia(@PathVariable long idOcupacion) {
        try {
            return ResponseEntity.ok(this.ocupacionService.eliminarOcupacionOrquestasDeGalicia(idOcupacion));
        } catch (Exception e) {
            return ResponseEntity.ok(DefaultResponseBody.builder()
                    .success(false)
                    .message("Error al eliminar de OrquestasDeGalicia: " + e.getMessage())
                    .messageType("error")
                    .build());
        }
    }

    @PostMapping("/ocupaciones-excel")
    public ResponseEntity<byte[]> exportarOcupacionesExcel(@AuthenticationPrincipal CustomAuthenticatedUser user,
                                                            @ModelAttribute OcupacionListFilterDto ocupacionListFilterDto) {
        // Generar archivo Excel
        var excelStream = this.ocupacionService.exportOcupacionesToExcel(user, ocupacionListFilterDto);

        // Configurar headers de respuesta
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        // Generar nombre del archivo
        String fileNameToExport = "Ocupaciones_"
                .concat(DateUtils.getDateStr(new Date(), "ddMMyyyyHHmmss"))
                .concat(".xlsx");

        headers.setContentDispositionFormData("attachment", fileNameToExport);

        return new ResponseEntity<>(excelStream.toByteArray(), headers, HttpStatus.OK);
    }

    @PostMapping("/ocupaciones-pdf")
    public ResponseEntity<byte[]> exportarOcupacionesPDF(@AuthenticationPrincipal CustomAuthenticatedUser user,
                                                          @ModelAttribute OcupacionListFilterDto ocupacionListFilterDto) {
        // Generar archivo PDF
        byte[] pdfBytes = this.ocupacionService.exportOcupacionesToPDF(user, ocupacionListFilterDto);

        // Configurar headers de respuesta
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);

        // Generar nombre del archivo
        String fileNameToExport = "Ocupaciones_"
                .concat(DateUtils.getDateStr(new Date(), "ddMMyyyyHHmmss"))
                .concat(".pdf");

        headers.setContentDispositionFormData("attachment", fileNameToExport);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    private void getModelAttributeComunOcupacionList(CustomAuthenticatedUser user, Model model) {

        OcupacionListFilterDto filter = model.containsAttribute("ocupacionListFilterDto") ? (OcupacionListFilterDto) model.getAttribute("ocupacionListFilterDto") : OcupacionListFilterDto.builder().fechaDesde(LocalDate.now()).fechaHasta(LocalDate.now().plusMonths(2)).build() ;

        if (!model.containsAttribute("listaOcupaciones")) {
            model.addAttribute("listaOcupaciones", new ArrayList<>());
        }

        if (!model.containsAttribute("ocupacionListFilterDto")) {
            model.addAttribute("ocupacionListFilterDto", OcupacionListFilterDto.builder().fechaDesde(LocalDate.now().minusMonths(2)).build());
        }
        final List<AgenciaRecord> listaAgenciaRecord = this.agenciaService.findMisAgencias(user.getMapPermisosAgencia().keySet());

        if (CollectionUtils.isNotEmpty(listaAgenciaRecord)){
            model.addAttribute("listaAgencias", listaAgenciaRecord);
            model.addAttribute("listaArtistas", this.artistaService.findAllArtistasByAgenciaId(filter.getIdAgencia()!=null ? filter.getIdAgencia() : listaAgenciaRecord.get(0).id()));
        }

        model.addAttribute("listaArtistasPermisosOcupacion", this.artistaService.findMisArtistas(obtenerArtistasConPermisoOcupaciones(user.getMapPermisosArtista())));

    }

    private String getSortColumnName(int column) {
        return switch (column) {
            case 0 -> "artista.nombre";
            case 1 -> "fechaCreacion";
            case 2 -> "fecha";
            case 3 -> "poblacion";
            case 4 -> "municipio.nombre";
            case 5 -> "usuario.nombre";
            case 6 -> "ocupacionEstado.nombre";
            case 7 -> "matinal";
            default -> "fechaCreacion";
        };
    }

    private Map<String, Object> toDataTableRow(OcupacionListRecord ocupacion) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", ocupacion.id());
        row.put("artista", ocupacion.artista());
        row.put("fechaCreacion", ocupacion.fechaCreacion() != null ? ocupacion.fechaCreacion().format(DATE_FORMATTER) : "");
        row.put("start", ocupacion.start() != null ? ocupacion.start().format(DATE_FORMATTER) : "");
        row.put("localidad", ocupacion.localidad() != null ? ocupacion.localidad() : "");
        row.put("municipioProvincia", String.format("%s, %s", ocupacion.municipio(), ocupacion.provincia()));
        row.put("nombreUsuario", ocupacion.nombreUsuario());
        row.put("tipoOcupacion", ocupacion.tipoOcupacion());
        row.put("estado", ocupacion.estado());
        row.put("matinal", ocupacion.matinal());
        row.put("soloMatinal", ocupacion.soloMatinal());
        return row;
    }

    private LocalDate parseDateValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return DateUtils.parseLocalDate(value, "dd-MM-yyyy");
    }

    private Long parseLongValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }



}
