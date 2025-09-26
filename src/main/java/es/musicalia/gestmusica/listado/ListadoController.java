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

import java.time.LocalDate;
import java.util.*;

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
                .fechaDesde(LocalDate.now().minusMonths(2))
                .fechaHasta(LocalDate.now())
                .build();
        List<ListadoRecord> listadosGenerados = this.listadoService.obtenerListadoEntreFechas(listadoAudienciasDto);
        try {
            String chartDataJson = objectMapper.writeValueAsString(listadoService.obtenerListadosPorMes(listadosGenerados));
            model.addAttribute("chartData", chartDataJson);

        } catch (JsonProcessingException e) {
            log.error("Error consultando audiencia listados", e);
        }
        model.addAttribute("listadosGenerados", listadosGenerados);
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

            model.addAttribute("listadosGenerados", listados);
            String chartDataJson = objectMapper.writeValueAsString(listadoService.obtenerListadosPorMes(listados));
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
}