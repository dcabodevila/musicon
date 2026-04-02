package es.musicalia.gestmusica.agencia;


import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.auth.model.SecurityService;
import es.musicalia.gestmusica.file.FileService;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventNames;
import es.musicalia.gestmusica.observabilidad.FunctionalEventOutcome;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.tarifa.TarifaService;
import es.musicalia.gestmusica.usuario.TipoUsuarioEnum;
import es.musicalia.gestmusica.usuario.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

@Slf4j
@Controller
@RequestMapping(value="agencia")
public class AgenciasController {


    private final UserService userService;
    private final LocalizacionService localizacionService;
    private final FileService fileService;
    private final AgenciaService agenciaService;
    private final ArtistaService artistaService;
    private final SecurityService securityService;
    private final TarifaService tarifaService;
    private final FunctionalEventTracker functionalEventTracker;

    public AgenciasController(UserService userService, LocalizacionService localizacionService, AgenciaService agenciaService, FileService fileService,
                              ArtistaService artistaService, SecurityService securityService, TarifaService tarifaService, FunctionalEventTracker functionalEventTracker){
        this.userService = userService;
        this.localizacionService = localizacionService;
        this.agenciaService = agenciaService;
        this.fileService = fileService;
        this.artistaService = artistaService;
        this.securityService = securityService;
        this.tarifaService = tarifaService;
        this.functionalEventTracker = functionalEventTracker;
    }

    @GetMapping
    public String agencias(@AuthenticationPrincipal CustomAuthenticatedUser user, Model model) {
        model.addAttribute("listaAgencias",this.agenciaService.findAllAgenciasForUser());

        return "agencias";
    }

    @GetMapping("/mis-agencias")
    public String misAgencias(@AuthenticationPrincipal CustomAuthenticatedUser user, Model model) {

        final Map<Long, Set<String>> mapPermisosAgencia = user.getMapPermisosAgencia();
        model.addAttribute("listaAgencias", mapPermisosAgencia.isEmpty() ? new ArrayList<>() : this.agenciaService.findMisAgencias(mapPermisosAgencia.keySet()));

        return "agencias";
    }

    @GetMapping("/crear")
    public String crearAgencias(Model model) {
        model.addAttribute("agenciaDto", new AgenciaDto());
        model.addAttribute("listaProvincias", this.localizacionService.findAllProvincias());
        model.addAttribute("listaUsuarios", this.userService.findAllUsuarioRecordsNotAdmin());

        return "agencia-detail-edit";
    }
    @GetMapping("/edit/{id}")
    @PreAuthorize("hasPermission(#idAgencia, 'AGENCIA', 'AGENCIA_EDITAR')")
    public String detalleEditarAgencia(Model model, @PathVariable("id") Long idAgencia) {
        model.addAttribute("agenciaDto", this.agenciaService.findAgenciaDtoById(idAgencia));
        model.addAttribute("listaProvincias", this.localizacionService.findAllProvincias());
        model.addAttribute("listaUsuarios", this.userService.findAllUsuarioRecordsNotAdmin());

        return "agencia-detail-edit";
    }
    @GetMapping("/{id}")
    public String detalleAgencia(Model model, @PathVariable("id") Long idAgencia) {
        model.addAttribute("agenciaDto", this.agenciaService.findAgenciaDtoById(idAgencia));
        final var listaArtistas = this.artistaService.findAllArtistasByAgenciaId(idAgencia);
        model.addAttribute("listaArtistas", listaArtistas);
        final boolean sinTarifasActivas = !listaArtistas.isEmpty() && !this.tarifaService.agenciaTieneTarifasActivas(idAgencia);
        model.addAttribute("sinTarifasActivas", sinTarifasActivas);

        return "agencia-detail";
    }

    @PostMapping("/guardar")
    public String guardarAgencia(@AuthenticationPrincipal CustomAuthenticatedUser user, Model model, @ModelAttribute("agenciaDto") @Valid AgenciaDto agenciaDto,  @RequestParam(value = "image", required = false) MultipartFile multipartFile,
                                 BindingResult bindingResult, RedirectAttributes redirectAttributes, Errors errors) {

        boolean isCreacion = agenciaDto.getId() == null;

        if (bindingResult.hasErrors()) {
            if (isCreacion) {
                functionalEventTracker.track(
                        FunctionalEventNames.AGENCIA_CREATED,
                        FunctionalEventOutcome.FAILURE,
                        user != null ? user.getUserId() : null,
                        user != null && user.getUsuario() != null ? user.getUsuario().getUsername() : null,
                        Map.of("reason", "validation")
                );
            }
            return "agencia-detail-edit";
        }

        try {
            final Agencia agencia = this.agenciaService.saveAgencia(agenciaDto);
            String uploadDir = "image/agencia-photos/" + agencia.getId();
            final String uploadedFile = this.fileService.guardarFichero(multipartFile);
            agenciaDto.setId(agencia.getId());
            if (uploadedFile!=null){
                agenciaDto.setLogo(uploadedFile);
                this.agenciaService.saveAgencia(agenciaDto);
            }

            // Recarga/invalida sesión si se ha asignado un responsable
            if (agenciaDto.getIdUsuario() != null) {
                try {
                    securityService.recargarOInvalidarSesion(agenciaDto.getIdUsuario());
                } catch (Exception e) {
                    log.error("Error recargando/invalidando sesión tras asignación de responsable: {}", e.getMessage());
                }
            }

            redirectAttributes.addFlashAttribute("message", "Agencia guardada correctamente");
            redirectAttributes.addFlashAttribute("alertClass", "success");

            if (isCreacion) {
                functionalEventTracker.track(
                        FunctionalEventNames.AGENCIA_CREATED,
                        FunctionalEventOutcome.SUCCESS,
                        user != null ? user.getUserId() : null,
                        user != null && user.getUsuario() != null ? user.getUsuario().getUsername() : null,
                        Map.of("agencia_id", agencia.getId())
                );
            }
            return "redirect:/agencia/"+ agencia.getId();

        } catch (Exception e){
            log.error("Error guardando agencia", e);
            if (isCreacion) {
                functionalEventTracker.track(
                        FunctionalEventNames.AGENCIA_CREATED,
                        FunctionalEventOutcome.FAILURE,
                        user != null ? user.getUserId() : null,
                        user != null && user.getUsuario() != null ? user.getUsuario().getUsername() : null,
                        Map.of("reason", "unexpected_error")
                );
            }
            model.addAttribute("message", "Error guardando agencia");
            model.addAttribute("alertClass", "danger");
            return "agencia-detail-edit";
        }


    }

    @GetMapping("/onboarding")
    public String mostrarOnboardingAgencia(@AuthenticationPrincipal CustomAuthenticatedUser user, Model model, RedirectAttributes redirectAttributes) {
        // Validar que el usuario es de tipo AGENCIA
        if (user.getUsuario().getTipoUsuario() != TipoUsuarioEnum.AGENCIA) {
            redirectAttributes.addFlashAttribute("message", "Solo usuarios de tipo Agencia pueden acceder a esta página.");
            redirectAttributes.addFlashAttribute("alertClass", "danger");
            return "redirect:/";
        }

        // Verificar si el usuario ya tiene agencia
        if (!user.getMapPermisosAgencia().isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Ya tienes una agencia creada. Accede a ella desde el menú principal.");
            redirectAttributes.addFlashAttribute("alertClass", "info");
            return "redirect:/";
        }

        model.addAttribute("agenciaOnboardingDto", new AgenciaOnboardingDto());
        model.addAttribute("listaProvincias", this.localizacionService.findAllProvincias());

        return "agencia-onboarding";
    }

    @PostMapping("/onboarding")
    public String guardarAgenciaOnboarding(@AuthenticationPrincipal CustomAuthenticatedUser user,
                                           @ModelAttribute("agenciaOnboardingDto") @Valid AgenciaOnboardingDto onboardingDto,
                                           BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("listaProvincias", this.localizacionService.findAllProvincias());
            return "agencia-onboarding";
        }

        try {
            Agencia agencia = this.agenciaService.crearAgenciaOnboarding(onboardingDto, user.getUsuario().getId());

            functionalEventTracker.track(
                    FunctionalEventNames.AGENCIA_CREATED,
                    FunctionalEventOutcome.SUCCESS,
                    user.getUserId(),
                    user.getUsuario() != null ? user.getUsuario().getUsername() : null,
                    Map.of("agencia_id", agencia.getId(), "flow", "onboarding")
            );

            // Recarga permisos del usuario actual tras onboarding exitoso
            try {
                securityService.reloadUserAuthorities();
            } catch (Exception e) {
                log.error("Error recargando permisos tras onboarding: {}", e.getMessage());
            }

            redirectAttributes.addFlashAttribute("message", "Agencia creada correctamente. Bienvenido a festia.es");
            redirectAttributes.addFlashAttribute("alertClass", "success");
            return "redirect:/agencia/" + agencia.getId();
        } catch (RuntimeException e) {
            log.error("Error creando agencia onboarding: {}", e.getMessage());
            functionalEventTracker.track(
                    FunctionalEventNames.AGENCIA_CREATED,
                    FunctionalEventOutcome.FAILURE,
                    user.getUserId(),
                    user.getUsuario() != null ? user.getUsuario().getUsername() : null,
                    Map.of("reason", "runtime_error", "flow", "onboarding")
            );
            if (e.getMessage().contains("ya tiene una agencia")) {
                redirectAttributes.addFlashAttribute("message", "Ya tienes una agencia creada.");
                redirectAttributes.addFlashAttribute("alertClass", "warning");
                return "redirect:/";
            }
            model.addAttribute("listaProvincias", this.localizacionService.findAllProvincias());
            model.addAttribute("message", "Error al crear la agencia: " + e.getMessage());
            model.addAttribute("alertClass", "danger");
            return "agencia-onboarding";
        } catch (Exception e) {
            log.error("Error inesperado creando agencia onboarding", e);
            functionalEventTracker.track(
                    FunctionalEventNames.AGENCIA_CREATED,
                    FunctionalEventOutcome.FAILURE,
                    user.getUserId(),
                    user.getUsuario() != null ? user.getUsuario().getUsername() : null,
                    Map.of("reason", "unexpected_error", "flow", "onboarding")
            );
            model.addAttribute("listaProvincias", this.localizacionService.findAllProvincias());
            model.addAttribute("message", "Error inesperado al crear la agencia");
            model.addAttribute("alertClass", "danger");
            return "agencia-onboarding";
        }
    }



}
