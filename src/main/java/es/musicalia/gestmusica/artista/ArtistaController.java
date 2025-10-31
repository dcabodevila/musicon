package es.musicalia.gestmusica.artista;

import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.file.FileService;
import es.musicalia.gestmusica.incremento.IncrementoService;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.ocupacion.OcupacionSaveDto;
import es.musicalia.gestmusica.ocupacion.OcupacionService;
import es.musicalia.gestmusica.tarifa.TarifaAnualDto;
import es.musicalia.gestmusica.usuario.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Controller
@RequestMapping(value="artista")
public class ArtistaController {


    private final UserService userService;
    private final FileService fileService;
    private final ArtistaService artistaService;
    private final AgenciaService agenciaService;
    private final LocalizacionService localizacionService;
    private final IncrementoService incrementoService;
    private final OcupacionService ocupacionService;


    public ArtistaController(UserService userService, ArtistaService artistaService, FileService fileService, AgenciaService agenciaService,
                             LocalizacionService localizacionService, IncrementoService incrementoService, OcupacionService ocupacionService){
        this.userService = userService;
        this.artistaService = artistaService;
        this.fileService = fileService;
        this.agenciaService = agenciaService;
        this.localizacionService = localizacionService;
        this.incrementoService =incrementoService;
        this.ocupacionService = ocupacionService;

    }

    @GetMapping
    public String artistas(@AuthenticationPrincipal CustomAuthenticatedUser user,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {

        Pageable pageable = PageRequest.of(page, 8);
        Page<ArtistaDto> paginaArtistas = this.artistaService.findAllArtistasForUserPaginated(user.getUsuario(), pageable);

        model.addAttribute("listaArtistasSelect", this.artistaService.findAllArtistasForUser(user.getUsuario()));


        model.addAttribute("listaArtistas", paginaArtistas.getContent());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", paginaArtistas.getTotalPages());
        model.addAttribute("totalElementos", paginaArtistas.getTotalElements());
        model.addAttribute("tieneSiguiente", paginaArtistas.hasNext());
        model.addAttribute("tieneAnterior", paginaArtistas.hasPrevious());
        model.addAttribute("isMisArtistas", false);
        return "artistas";
    }

    @GetMapping("/mis-artistas")
    public String misArtistas(@AuthenticationPrincipal CustomAuthenticatedUser user, @RequestParam(defaultValue = "0") int page, Model model) {
        if (userService.isUserAutheticated()){

            final Map<Long, Set<String>> mapPermisosArtista = user.getMapPermisosArtista();

            Pageable pageable = PageRequest.of(page, 16);
            Page<ArtistaRecord> paginaArtistas = this.artistaService.findMisArtistasPaginated(mapPermisosArtista.keySet(),pageable);
            model.addAttribute("listaArtistasSelect", mapPermisosArtista.isEmpty() ? new ArrayList<>() : this.artistaService.findMisArtistas(mapPermisosArtista.keySet()));

            model.addAttribute("listaArtistas", paginaArtistas.getContent());
            model.addAttribute("paginaActual", page);
            model.addAttribute("totalPaginas", paginaArtistas.getTotalPages());
            model.addAttribute("totalElementos", paginaArtistas.getTotalElements());
            model.addAttribute("tieneSiguiente", paginaArtistas.hasNext());
            model.addAttribute("tieneAnterior", paginaArtistas.hasPrevious());
            model.addAttribute("isMisArtistas", true);

            model.addAttribute("listaArtistas", mapPermisosArtista.isEmpty() ? new ArrayList<>() : paginaArtistas);

        }
        return "artistas";
    }

    @GetMapping("/crear/{idAgencia}")
    public String crearArtistas(Model model, @PathVariable("idAgencia") Long idAgencia) {

        final ArtistaDto artistaDto = new ArtistaDto();
        artistaDto.setIdAgencia(idAgencia);

        getModelAttributeDetail(model, artistaDto);
        return "artista-detail-edit";
    }

    private void getModelAttributeDetail(Model model, ArtistaDto artistaDto) {
        model.addAttribute("artistaDto", artistaDto);
        model.addAttribute("isArtistaPermiteOrquestasDeGalicia", artistaDto.isPermiteOrquestasDeGalicia());
        model.addAttribute("listaUsuarios", this.userService.findAllUsuarioRecords());
        model.addAttribute("idUsuarioAutenticado", this.userService.isUserAutheticated()? this.userService.obtenerUsuarioAutenticado().get().getId() : null);
        model.addAttribute("listaAgencias", artistaDto.getIdAgencia() != null ? this.agenciaService.findAgenciaRecordById(artistaDto.getIdAgencia()) : this.agenciaService.findAllAgenciasForUser());
        model.addAttribute("listaTipoArtista", this.artistaService.listaTipoArtista());
        model.addAttribute("listaTipoEscenario", this.artistaService.listaTipoEscenario());
        model.addAttribute("listaCcaa", this.localizacionService.findAllComunidades());
        model.addAttribute("anoTarifa", Year.now());
        model.addAttribute("listaProvincias", this.localizacionService.findAllProvincias());
        model.addAttribute("listaTiposOcupacion", this.ocupacionService.listarTiposOcupacion(artistaDto.getId()));
        model.addAttribute("listaTiposIncremento", this.incrementoService.listTipoIncremento());
    }

    private void getModelAttributeArtistaOcupacion(Model model, ArtistaDto artistaDto){
        model.addAttribute("listaProvinciasCcaaArtista", this.localizacionService.findAllProvinciasByCcaaId(artistaDto.getIdCcaa()));
    }

    @GetMapping("/{id}")
    public String detalleArtista(Model model, @PathVariable("id") Long idArtista) {
        final ArtistaDto artistaDto = this.artistaService.findArtistaDtoById(idArtista);
        getModelAttributeDetail(model, artistaDto);
        getModelAttributeArtistaOcupacion(model, artistaDto);
        model.addAttribute("ocupacionDto", getNewOcupacionSaveDto(idArtista));
        model.addAttribute("listaArtistas", List.of(this.artistaService.findArtistaDtoById(idArtista)));

        addTarifaAnualModelAttribute(model, idArtista);

        return "artista-detail";
    }

    private static void addTarifaAnualModelAttribute(Model model, Long idArtista) {
        TarifaAnualDto tarifaAnualDto = new TarifaAnualDto();
        tarifaAnualDto.setIdArtista(idArtista);
        tarifaAnualDto.setAno(Year.now().getValue());

        model.addAttribute("tarifaAnualDto", tarifaAnualDto);
    }

    @GetMapping("/edit/{id}")
    public String editArtista(Model model, @PathVariable("id") Long idArtista) {
        final ArtistaDto artistaDto = this.artistaService.findArtistaDtoById(idArtista);

        getModelAttributeDetail(model, artistaDto);

        return "artista-detail-edit";
    }

    @PostMapping("/guardar")
    public String guardarArtista(Model model, @ModelAttribute("artistaDto") @Valid ArtistaDto artistaDto, @RequestParam(value = "image", required = false) MultipartFile multipartFile,
                                 BindingResult bindingResult, RedirectAttributes redirectAttributes, Errors errors) {

        if (bindingResult.hasErrors()) {
            getModelAttributeDetail(model, artistaDto);

            return "artista-detail";
        }

        try {
            final Artista artista = this.artistaService.saveArtista(artistaDto);
            artistaDto.setId(artista.getId());
            final String uploadedFile = this.fileService.guardarFichero(multipartFile);

            if (uploadedFile!=null){
                artistaDto.setLogo(uploadedFile);
                this.artistaService.saveArtista(artistaDto);
            }

            redirectAttributes.addFlashAttribute("message", "Artista guardado correctamente");
            redirectAttributes.addFlashAttribute("alertClass", "success");
            return "redirect:/artista/"+ artistaDto.getId();

        } catch (Exception e){
            log.error("Error guardando artista", e);
            model.addAttribute("message", "Error guardando artista");
            model.addAttribute("alertClass", "danger");
            addTarifaAnualModelAttribute(model, artistaDto.getId());
            getModelAttributeDetail(model, artistaDto);

            return "artista-detail";
        }


    }

    @GetMapping("/artistas/{idAgencia}")
    @ResponseBody
    public ResponseEntity<List<ArtistaRecord>> obtenerArtistasPorAgencia(@PathVariable Long idAgencia) {
        List<ArtistaRecord> artistas = artistaService.findArtistasRecordByIdAgencia(idAgencia);
        return ResponseEntity.ok(artistas);
    }


    private OcupacionSaveDto getNewOcupacionSaveDto(long idArtista) {
        ArtistaDto artista = this.artistaService.findArtistaDtoById(idArtista);
        final OcupacionSaveDto ocupacion = new OcupacionSaveDto();
        ocupacion.setIdArtista(idArtista);
        ocupacion.setIdAgencia(artista.getIdAgencia());
        ocupacion.setIdCcaa(artista.getIdCcaa());
        ocupacion.setImporte(BigDecimal.ZERO);
        ocupacion.setPorcentajeRepre(BigDecimal.ZERO);
        ocupacion.setIva(BigDecimal.ZERO);
        return ocupacion;
    }
}
