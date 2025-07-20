package es.musicalia.gestmusica.ocupacion;


import es.musicalia.gestmusica.agencia.AgenciaRecord;
import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.artista.ArtistaDto;
import es.musicalia.gestmusica.artista.ArtistaRecord;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final LocalizacionService localizacionService;

    public OcupacionController(OcupacionService ocupacionService, AgenciaService agenciaService, ArtistaService artistaService, LocalizacionService localizacionService){
        this.ocupacionService = ocupacionService;

        this.agenciaService = agenciaService;
        this.artistaService = artistaService;
        this.localizacionService = localizacionService;
    }


    @GetMapping("/{id}")
    public String getOcupacionModelAndView(@AuthenticationPrincipal CustomAuthenticatedUser user ,Model model, @PathVariable long id) {
        final OcupacionSaveDto ocupacion = ocupacionService.getOcupacionSaveDto(id);
        obtenerModelAttributeComun(user, model, ocupacion);

        return "ocupacion-detail";

    }

    @GetMapping("/nueva/{idArtista}")
    public String getNuevaOcupacionModelAndView(@AuthenticationPrincipal CustomAuthenticatedUser user ,Model model, @PathVariable long idArtista) {

        if (user.getMapPermisosArtista().containsKey(idArtista) && user.getMapPermisosArtista().get(idArtista).contains("OCUPACIONES")) {
            ArtistaDto artista = this.artistaService.findArtistaDtoById(idArtista);
            final OcupacionSaveDto ocupacion = new OcupacionSaveDto();
            ocupacion.setIdArtista(idArtista);
            ocupacion.setIdAgencia(artista.getIdAgencia());
            ocupacion.setIdCcaa(artista.getIdCcaa());
            ocupacion.setImporte(BigDecimal.ZERO);
            ocupacion.setPorcentajeRepre(BigDecimal.ZERO);
            ocupacion.setIva(BigDecimal.ZERO);
            obtenerModelAttributeComun(user, model, ocupacion);
        }
        else {
            throw new org.springframework.security.access.AccessDeniedException("No tiene permisos para crear ocupaciones para este artista");
        }

        return "ocupacion-detail";

    }

    private void obtenerModelAttributeComun(CustomAuthenticatedUser user, Model model, OcupacionSaveDto ocupacion) {
        model.addAttribute("ocupacionDto", ocupacion);
        List<ArtistaRecord> listaArtistasPermisosOcupacion = this.artistaService.findMisArtistas(obtenerArtistasConPermisoOcupaciones(user.getMapPermisosArtista()));

        model.addAttribute("listaArtistas", listaArtistasPermisosOcupacion);
        model.addAttribute("listaCcaa", this.localizacionService.findAllComunidades());

        final List<CodigoNombreRecord> listaProvincias = this.localizacionService.findAllProvinciasByCcaaId(ocupacion.getIdCcaa());
        model.addAttribute("listaProvinciasCcaaArtista", listaProvincias);
        model.addAttribute("listaMunicipioListado", ocupacion.getIdProvincia() != null ? this.localizacionService.findAllMunicipiosByIdProvincia(ocupacion.getIdProvincia()): this.localizacionService.findAllMunicipiosByIdProvincia(listaProvincias.get(0).id()) );
        model.addAttribute("listaTiposOcupacion", this.ocupacionService.listarTiposOcupacion(ocupacion.getIdArtista()));
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
    public ResponseEntity<DefaultResponseBody> anularOcupacion(@PathVariable long id) {

        return ResponseEntity.ok(this.ocupacionService.anularOcupacion(id));

    }

    @GetMapping("/confirmar/{id}")
    public ResponseEntity<DefaultResponseBody> confirmarOcupacion(@PathVariable long id) {

        return ResponseEntity.ok(this.ocupacionService.confirmarOcupacion(id));

    }

    @PostMapping("/save")
    public ResponseEntity<?> saveOcupacion(
            @RequestBody OcupacionSaveDto ocupacionSaveDto) {


        if (ocupacionService.existeOcupacionFecha(ocupacionSaveDto)) {
            return ResponseEntity.ok(DefaultResponseBody.builder().success(false).message("Ya existe una ocupación en esa fecha").messageType("error").build());
        }

        try {
            return ResponseEntity.ok(ocupacionService.saveOcupacion(ocupacionSaveDto));
        }
        catch (ModificacionOcupacionException e) {
            return ResponseEntity.ok(DefaultResponseBody.builder().success(false).message("No tiene permisos para modificar la ocupación de otros usuarios").messageType("error").build());
        }
        catch (Exception e) {
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
        redirectAttributes.addFlashAttribute("listaOcupaciones", this.ocupacionService.findOcupacionesByArtistasListAndDatesActivo(user, ocupacionListFilterDto));
        redirectAttributes.addFlashAttribute("ocupacionListFilterDto", ocupacionListFilterDto);

        return "redirect:/ocupacion/list";

    }
    private void getModelAttributeComunOcupacionList(CustomAuthenticatedUser user, Model model) {

        OcupacionListFilterDto filter = model.containsAttribute("ocupacionListFilterDto") ? (OcupacionListFilterDto) model.getAttribute("ocupacionListFilterDto") : OcupacionListFilterDto.builder().fechaDesde(LocalDate.now()).build() ;

        if (!model.containsAttribute("listaOcupaciones")) {
            model.addAttribute("listaOcupaciones", new ArrayList<>());
        }

        if (!model.containsAttribute("ocupacionListFilterDto")) {
            model.addAttribute("ocupacionListFilterDto", OcupacionListFilterDto.builder().fechaDesde(LocalDate.now()).build());
        }
        final List<AgenciaRecord> listaAgenciaRecord = this.agenciaService.findMisAgencias(user.getMapPermisosAgencia().keySet());

        if (CollectionUtils.isNotEmpty(listaAgenciaRecord)){
            model.addAttribute("listaAgencias", listaAgenciaRecord);
            model.addAttribute("listaArtistas", this.artistaService.findAllArtistasByAgenciaId(filter.getIdAgencia()!=null ? filter.getIdAgencia() : listaAgenciaRecord.get(0).id()));
        }

        model.addAttribute("listaArtistasPermisosOcupacion", this.artistaService.findMisArtistas(obtenerArtistasConPermisoOcupaciones(user.getMapPermisosArtista())));

    }



}