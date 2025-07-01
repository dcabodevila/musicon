package es.musicalia.gestmusica.artista;

import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.file.FileService;
import es.musicalia.gestmusica.incremento.IncrementoService;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.ocupacion.OcupacionService;
import es.musicalia.gestmusica.tarifa.TarifaAnualDto;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Year;
import java.util.ArrayList;
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
    public String artistas(Model model) {
        if (userService.isUserAutheticated()){
            final Usuario usuario = userService.obtenerUsuarioAutenticado();

            final Map<Long, Set<String>> mapPermisosArtista =
                    ((CustomAuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                            .getMapPermisosArtista();

            model.addAttribute("listaArtistas", mapPermisosArtista.isEmpty() ? this.artistaService.findAllArtistasForUser(usuario) : new ArrayList<>());
            model.addAttribute("listaMisArtistas", mapPermisosArtista.isEmpty() ? new ArrayList<>() : this.artistaService.findMisArtistas(mapPermisosArtista.keySet()));
            model.addAttribute("listaOtrosArtistas", mapPermisosArtista.isEmpty() ? new ArrayList<>() : this.artistaService.findOtrosArtistas(mapPermisosArtista.keySet()));
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

        model.addAttribute("listaUsuarios", this.userService.findAllUsuarioRecords());
        model.addAttribute("listaAgencias", artistaDto.getIdAgencia() != null ? this.agenciaService.findAgenciaDtoById(artistaDto.getIdAgencia()) : this.agenciaService.findAllAgenciasForUser(userService.obtenerUsuarioAutenticado()));
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
            return "redirect:/agencia/"+ artistaDto.getIdAgencia();

        } catch (Exception e){
            log.error("Error guardando artista", e);
            model.addAttribute("message", "Error guardando artista");
            model.addAttribute("alertClass", "danger");
            addTarifaAnualModelAttribute(model, artistaDto.getId());
            getModelAttributeDetail(model, artistaDto);

            return "artista-detail";
        }


    }



}
