package es.musicalia.gestmusica.ocupacion;


import es.musicalia.gestmusica.agencia.AgenciaRecord;
import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.artista.ArtistaRecord;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.permiso.TipoPermisoEnum;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping(value="ocupacion")
public class OcupacionController {

    private final OcupacionService ocupacionService;
    private final AgenciaService agenciaService;
    private final ArtistaService artistaService;

    public OcupacionController(OcupacionService ocupacionService, AgenciaService agenciaService, ArtistaService artistaService){
        this.ocupacionService = ocupacionService;

        this.agenciaService = agenciaService;
        this.artistaService = artistaService;
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<OcupacionEditDto> getOcupacionEditDtoByArtistaIdAndDates(@PathVariable long id) {

        return ResponseEntity.ok(ocupacionService.findOcupacionEditDtoByArtistaIdAndDates(id));

    }

    @GetMapping("/anular/{id}")
    public ResponseEntity<DefaultResponseBody> anularOcupacion(@PathVariable long id) {
        DefaultResponseBody result = new DefaultResponseBody();

        this.ocupacionService.anularOcupacion(id);
        result.setSuccess(true);
        result.setMessage("Ocupación anulada");
        result.setMessageType("success");

        return ResponseEntity.ok(result);

    }

    @GetMapping("/confirmar/{id}")
    public ResponseEntity<DefaultResponseBody> confirmarOcupacion(@PathVariable long id) {
        DefaultResponseBody result = new DefaultResponseBody();

        this.ocupacionService.confirmarOcupacion(id);
        result.setSuccess(true);
        result.setMessage("Ocupación confirmada");
        result.setMessageType("success");

        return ResponseEntity.ok(result);

    }

    @PostMapping("/save")
    public ResponseEntity<?> saveOcupacion(
            @RequestBody OcupacionSaveDto ocupacionSaveDto) {

        DefaultResponseBody result = new DefaultResponseBody();

        if (ocupacionService.existeOcupacionFecha(ocupacionSaveDto)) {
            result.setSuccess(false);
            result.setMessage("Ya existe una ocupación en esa fecha");
            result.setMessageType("error");

        }
        else {
            try {
                ocupacionService.saveOcupacion(ocupacionSaveDto);
                result.setSuccess(true);
                result.setMessage("Ocupación guardada");
                result.setMessageType("success");
            } catch (ModificacionOcupacionException e) {
                result.setSuccess(false);
                result.setMessage("No tiene permisos para modificar la ocupación de otros usuarios");
                result.setMessageType("error");
            }



        }

        return ResponseEntity.ok(result);


    }

    @GetMapping("/list")
    public String getListadoOcupaciones(@AuthenticationPrincipal CustomAuthenticatedUser user, Model model){

        getModelAttributeComunOcupacionList(user, model, OcupacionListFilterDto.builder().fechaDesde(LocalDate.now()).build());
        model.addAttribute("listaOcupaciones", new ArrayList<>());

        return "ocupaciones";
    }

    private void getModelAttributeComunOcupacionList(CustomAuthenticatedUser user, Model model, OcupacionListFilterDto filter) {

        model.addAttribute("ocupacionListFilterDto", filter);

        final List<AgenciaRecord> listaAgenciaRecord = this.agenciaService.findMisAgencias(user.getMapPermisosAgencia().keySet());

        model.addAttribute("listaAgencias", listaAgenciaRecord);
        if (CollectionUtils.isNotEmpty(listaAgenciaRecord)){
            model.addAttribute("listaAgencias", listaAgenciaRecord);
            model.addAttribute("listaArtistas", this.artistaService.findAllArtistasByAgenciaId(filter.getIdAgencia()!=null ? filter.getIdAgencia() : listaAgenciaRecord.get(0).id()));
        }

    }

    @PostMapping("/list")
    public String postListadoOcupaciones(@AuthenticationPrincipal CustomAuthenticatedUser user,
                                         @ModelAttribute OcupacionListFilterDto ocupacionListFilterDto,
                                         Model model) {
        model.addAttribute("listaOcupaciones", this.ocupacionService.findOcupacionesByArtistasListAndDatesActivo(user, ocupacionListFilterDto));

        getModelAttributeComunOcupacionList(user, model, ocupacionListFilterDto);
        return "ocupaciones";
    }

}