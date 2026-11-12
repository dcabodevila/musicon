package es.musicalia.gestmusica.auth.model;

import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.mail.EmailTemplateEnum;
import es.musicalia.gestmusica.observabilidad.FunctionalEventNames;
import es.musicalia.gestmusica.observabilidad.FunctionalEventOutcome;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.usuario.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Slf4j
@Controller
@RequestMapping(value="auth")
public class AuthController {

    private final UserService userService;
    private final CodigoVerificacionService codigoVerificacionService;
    private final LocalizacionService localizacionService;
    private final SecurityService securityService;
    private final FunctionalEventTracker functionalEventTracker;

    public AuthController(UserService userService, CodigoVerificacionService codigoVerificacionService, LocalizacionService localizacionService, SecurityService securityService, FunctionalEventTracker functionalEventTracker){
        this.userService = userService;
        this.codigoVerificacionService = codigoVerificacionService;
        this.localizacionService = localizacionService;
        this.securityService = securityService;
        this.functionalEventTracker = functionalEventTracker;
    }

    @GetMapping(value = "/login")
    public String login(Model model, String error, String logout) {
        if (error != null) {
            model.addAttribute("error", "Usuario o contraseña no válidos.");
        }
        if (logout != null) {
            model.addAttribute("message", "Has cerrado sesión correctamente.");
        }

//        SecurityContextHolder.clearContext();
        return "login";
    }

    @GetMapping("/registration")
    public String registration(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        model.addAttribute("listaProvincias", this.localizacionService.findAllProvincias());

        return "registration";
    }

    @PostMapping("/registration")
    public String registration(Model model, @ModelAttribute("registrationForm") @Valid RegistrationForm registrationForm,
                               BindingResult bindingResult,  RedirectAttributes redirectAttributes,
                               Errors errors) {




        if (userService.usernameExists(registrationForm.getUsername().trim())){
            bindingResult.rejectValue("username", "error.username-exists", "El nombre de usuario ya existe");
        }
        if (!registrationForm.getPassword().equals(registrationForm.getRetryPassword())){
            bindingResult.rejectValue("password", "error.password-retry", "Las contraseñas no coinciden");
            bindingResult.rejectValue("retryPassword", "error.password-retry", "Las contraseñas no coinciden");
        }

        if (bindingResult.hasErrors()) {
            functionalEventTracker.track(
                    FunctionalEventNames.AUTH_REGISTRATION_SUBMITTED,
                    FunctionalEventOutcome.FAILURE,
                    null,
                    registrationForm.getUsername(),
                    Map.of("reason", "validation")
            );
            model.addAttribute("listaProvincias", this.localizacionService.findAllProvincias());
            return "registration";
        }

        try {

            Usuario usuarioRegistrado = userService.saveRegistration(registrationForm);
            functionalEventTracker.track(
                    FunctionalEventNames.AUTH_REGISTRATION_SUBMITTED,
                    FunctionalEventOutcome.SUCCESS,
                    usuarioRegistrado.getId(),
                    usuarioRegistrado.getUsername(),
                    Map.of("tipo_usuario", String.valueOf(registrationForm.getTipoUsuario()))
            );
        }
        catch (EmailYaExisteException e){
            functionalEventTracker.track(
                    FunctionalEventNames.AUTH_REGISTRATION_SUBMITTED,
                    FunctionalEventOutcome.FAILURE,
                    null,
                    registrationForm.getUsername(),
                    Map.of("reason", "email_exists")
            );
            bindingResult.rejectValue("email", "error.email", "El email ya está registrado por otro usuario");
            model.addAttribute("listaProvincias", this.localizacionService.findAllProvincias());
            return "registration";
        }
        catch (Exception e){
            functionalEventTracker.track(
                    FunctionalEventNames.AUTH_REGISTRATION_SUBMITTED,
                    FunctionalEventOutcome.FAILURE,
                    null,
                    registrationForm.getUsername(),
                    Map.of("reason", "unexpected_error")
            );
            model.addAttribute("errors", errors);
            model.addAttribute("listaProvincias", this.localizacionService.findAllProvincias());
            log.error("Error en el guardado del registro", e);
            return "registration";

        }

        redirectAttributes.addFlashAttribute("email", registrationForm.getEmail());

        return "redirect:/auth/verify-email";
    }

    @GetMapping("/verify-email")
    public String mostrarVerificacionEmail(Model model, @ModelAttribute("email") String email) {
        model.addAttribute("email", email);
        return "verify-email";
    }

    @PostMapping("/verify-email")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verificarEmail(@RequestParam String email,
                                                              @RequestParam String codigo,
                                                              HttpServletRequest request) {
        try {
            // Solo cuentas pendientes de verificar: nunca activar ni iniciar sesión en una cuenta ya verificada
            boolean verificado = userService.isEmailPendienteVerificacion(email)
                    && codigoVerificacionService.verificarCodigo(email, codigo, EmailTemplateEnum.REGISTRO, false);

            if (verificado) {

                this.userService.activateUserByEmail(email);

                try {
                    securityService.autologin(email, request);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Email verificado correctamente.",
                            "redirect", "/"
                    ));
                } catch (Exception autologinEx) {
                    log.warn("Autologin fallido tras verificación de email para {}", email, autologinEx);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Email verificado correctamente. Ya puedes iniciar sesión.",
                            "redirect", "/auth/login"
                    ));
                }
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Código incorrecto o expirado."
                ));
            }
        } catch (Exception e) {
            log.error("Error verificando email para {}", email, e);

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error en la verificación. Inténtalo de nuevo más tarde."
            ));
        }
    }

    @PostMapping("/resend-code")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reenviarCodigo(@RequestParam String email) {
        try {
            // Misma respuesta para cuentas inexistentes o ya verificadas: no se envía nada y no se revela si existen
            boolean reenviado = !userService.isEmailPendienteVerificacion(email)
                    || codigoVerificacionService.reenviarCodigo(email, EmailTemplateEnum.REGISTRO);

            if (reenviado) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Código reenviado correctamente."
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Debe esperar antes de reenviar el código."
                ));
            }
        } catch (Exception e) {
            log.error("Error reenviando código de registro para {}", email, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error reenviando código. Inténtalo de nuevo más tarde."
            ));
        }
    }

    @GetMapping("/remember-password")
    public String rememberPassword(Model model) {
        model.addAttribute("rememberPasswordForm", new RememberPasswordForm());
        return "remember-password";
    }

    @PostMapping("/remember-password")
    public String processRememberPassword(Model model,
                                          @ModelAttribute("rememberPasswordForm") @Valid RememberPasswordForm form,
                                          BindingResult bindingResult,
                                          RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "remember-password";
        }

        try {
            // Verificar que el email existe
            if (!userService.existsUsuarioActivoByEmail(form.getEmail())) {
                bindingResult.rejectValue("email", "error.email.not-found", "No existe un usuario activo con este email");
                return "remember-password";
            }

            // Generar y enviar código
            codigoVerificacionService.generarYEnviarCodigo(
                    form.getEmail(),
                    EmailTemplateEnum.RECUPERACION_PASSWORD
            );

            redirectAttributes.addFlashAttribute("email", form.getEmail());
            redirectAttributes.addFlashAttribute("message",
                    "Se ha enviado un código de verificación a tu email.");

            return "redirect:/auth/verify-reset-password";

        } catch (Exception e) {
            log.error("Error enviando código de recuperación: {}", e.getMessage());
            model.addAttribute("error", "Error enviando el código. Inténtalo de nuevo.");
            return "remember-password";
        }
    }

    @GetMapping("/verify-reset-password")
    public String mostrarVerificacionResetPassword(Model model) {
        return "verify-reset-password";
    }

    @PostMapping("/verify-reset-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verificarCodigoResetPassword(@RequestParam String email,
                                                                            @RequestParam String codigo,
                                                                            HttpSession session) {
        try {
            boolean verificado = codigoVerificacionService.verificarCodigo(
                    email, codigo, EmailTemplateEnum.RECUPERACION_PASSWORD, false
            );

            if (verificado) {
                // The grant stays server-side: neither the email nor the code travel in the URL
                session.setAttribute(ResetPasswordGrant.SESSION_ATTRIBUTE, ResetPasswordGrant.forEmail(email));
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Código verificado correctamente.",
                        "redirect", "/auth/reset-password"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Código incorrecto o expirado."
                ));
            }
        } catch (Exception e) {
            log.error("Error verificando código de recuperación para {}", email, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error en la verificación. Inténtalo de nuevo más tarde."
            ));
        }
    }

    @GetMapping("/reset-password")
    public String mostrarResetPassword(HttpSession session, Model model) {
        ResetPasswordGrant grant = validResetPasswordGrant(session);
        if (grant == null) {
            return "redirect:/auth/remember-password";
        }
        ResetPasswordForm form = new ResetPasswordForm();
        form.setEmail(grant.email());
        model.addAttribute("resetPasswordForm", form);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String procesarResetPassword(@ModelAttribute("resetPasswordForm") @Valid ResetPasswordForm form,
                                        BindingResult bindingResult,
                                        Model model,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {

        // The email always comes from the server-side grant, never from the request
        ResetPasswordGrant grant = validResetPasswordGrant(session);
        form.setEmail(grant != null ? grant.email() : null);

        // Validar que las contraseñas coincidan
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("newPassword", "error.password-retry", "Las contraseñas no coinciden");
            bindingResult.rejectValue("confirmPassword", "error.password-retry", "Las contraseñas no coinciden");
        }

        if (bindingResult.hasErrors()) {
            return "reset-password";
        }

        if (grant == null) {
            model.addAttribute("error", "El código ha expirado. Solicita uno nuevo.");
            return "reset-password";
        }

        try {
            userService.changePasswordByEmail(grant.email(), form.getNewPassword());

            // Single use: consume the grant so the password cannot be changed again
            session.removeAttribute(ResetPasswordGrant.SESSION_ATTRIBUTE);

            redirectAttributes.addFlashAttribute("message",
                    "Contraseña cambiada correctamente. Ya puedes iniciar sesión.");

            return "redirect:/auth/login";

        } catch (Exception e) {
            log.error("Error cambiando contraseña: {}", e.getMessage());
            model.addAttribute("error", "Error cambiando la contraseña. Inténtalo de nuevo.");
            return "reset-password";
        }
    }

    /**
     * Returns the reset grant stored in the session when it is still valid; an expired grant is discarded.
     */
    private ResetPasswordGrant validResetPasswordGrant(HttpSession session) {
        if (session.getAttribute(ResetPasswordGrant.SESSION_ATTRIBUTE) instanceof ResetPasswordGrant grant) {
            if (!grant.isExpired()) {
                return grant;
            }
            session.removeAttribute(ResetPasswordGrant.SESSION_ATTRIBUTE);
        }
        return null;
    }

    @PostMapping("/resend-reset-code")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reenviarCodigoReset(@RequestParam String email) {
        try {
            boolean reenviado = codigoVerificacionService.reenviarCodigo(
                    email, EmailTemplateEnum.RECUPERACION_PASSWORD
            );

            if (reenviado) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Código reenviado correctamente."
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Debe esperar antes de reenviar el código."
                ));
            }
        } catch (Exception e) {
            log.error("Error reenviando código de recuperación para {}", email, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error reenviando código. Inténtalo de nuevo más tarde."
            ));
        }
    }


}
