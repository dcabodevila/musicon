package es.musicalia.gestmusica.auth.model;

import es.musicalia.gestmusica.usuario.*;
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

    public AuthController(UserService userService, CodigoVerificacionService codigoVerificacionService){
        this.userService = userService;
        this.codigoVerificacionService = codigoVerificacionService;
    }

    @GetMapping(value = "/login")
    public String login(Model model, String error, String logout) {
        if (error != null) {
            model.addAttribute("error", "Usuario o contraseña no válidos.");
        }

        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully.");

        }
//        SecurityContextHolder.clearContext();
        return "login";
    }

    @GetMapping("/registration")
    public String registration(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());

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
            return "registration";
        }

        try {

            userService.saveRegistration(registrationForm);
        }
        catch (EmailYaExisteException e){
            bindingResult.rejectValue("email", "error.email", "El email ya está registrado por otro usuario");
            return "registration";
        }
        catch (EnvioEmailException m){
            bindingResult.rejectValue("email", "error.email", "No se ha podido enviar el código al email seleccionado");
            return "registration";
        }
        catch (Exception e){
            model.addAttribute("errors", errors);

            log.error("Error en el guardado del registro", e);
            return "registration";

        }

        redirectAttributes.addFlashAttribute("email", registrationForm.getEmail());
        redirectAttributes.addFlashAttribute("message",
                "Registro exitoso. Se ha enviado un código de verificación a tu email.");


        return "redirect:/auth/verify-email";
    }

    @GetMapping("/verify-email")
    public String mostrarVerificacionEmail(Model model) {
        return "verify-email";
    }

    @PostMapping("/verify-email")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verificarEmail(@RequestParam String email,
                                                              @RequestParam String codigo) {
        try {
            boolean verificado = codigoVerificacionService.verificarCodigo(email, codigo, CodigoVerificacion.TipoVerificacion.REGISTRO, false);

            if (verificado) {

                this.userService.activateUserByEmail(email);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Email verificado correctamente. Ya puedes iniciar sesión.",
                        "redirect", "/auth/login"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Código incorrecto o expirado."
                ));
            }
        } catch (Exception e) {

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error en la verificación: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/resend-code")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reenviarCodigo(@RequestParam String email) {
        try {
            boolean reenviado = codigoVerificacionService.reenviarCodigo(email, CodigoVerificacion.TipoVerificacion.REGISTRO);

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
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error reenviando código: " + e.getMessage()
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
            if (!userService.existsUsuarioByEmail(form.getEmail())) {
                bindingResult.rejectValue("email", "error.email.not-found", "No existe un usuario con este email");
                return "remember-password";
            }

            // Generar y enviar código
            codigoVerificacionService.generarYEnviarCodigo(
                    form.getEmail(),
                    CodigoVerificacion.TipoVerificacion.RECUPERACION_PASSWORD
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
                                                                            @RequestParam String codigo) {
        try {
            boolean verificado = codigoVerificacionService.verificarCodigo(
                    email, codigo, CodigoVerificacion.TipoVerificacion.RECUPERACION_PASSWORD, false
            );

            if (verificado) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Código verificado correctamente.",
                        "redirect", "/auth/reset-password?email=" + email + "&codigo=" + codigo
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Código incorrecto o expirado."
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error en la verificación: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/reset-password")
    public String mostrarResetPassword(@RequestParam String email,
                                       @RequestParam String codigo,
                                       Model model) {
        ResetPasswordForm form = new ResetPasswordForm();
        form.setEmail(email);
        form.setCodigo(codigo);
        model.addAttribute("resetPasswordForm", form);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String procesarResetPassword(@ModelAttribute("resetPasswordForm") @Valid ResetPasswordForm form,
                                        BindingResult bindingResult,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {

        // Validar que las contraseñas coincidan
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("newPassword", "error.password-retry", "Las contraseñas no coinciden");
            bindingResult.rejectValue("confirmPassword", "error.password-retry", "Las contraseñas no coinciden");
        }

        if (bindingResult.hasErrors()) {
            return "reset-password";
        }

        try {
            // Verificar el código una vez más por seguridad
            boolean codigoValido = codigoVerificacionService.verificarCodigo(
                    form.getEmail(), form.getCodigo(), CodigoVerificacion.TipoVerificacion.RECUPERACION_PASSWORD, true
            );

            if (!codigoValido) {
                model.addAttribute("error", "El código ha expirado. Solicita uno nuevo.");
                return "reset-password";
            }

            // Cambiar la contraseña
            userService.changePasswordByEmail(form.getEmail(), form.getNewPassword());

            // Marcar el código como usado para evitar reutilización
            codigoVerificacionService.marcarCodigoComoUsado(form.getEmail(), form.getCodigo());

            redirectAttributes.addFlashAttribute("message",
                    "Contraseña cambiada correctamente. Ya puedes iniciar sesión.");

            return "redirect:/auth/login";

        } catch (Exception e) {
            log.error("Error cambiando contraseña: {}", e.getMessage());
            model.addAttribute("error", "Error cambiando la contraseña. Inténtalo de nuevo.");
            return "reset-password";
        }
    }

    @PostMapping("/resend-reset-code")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reenviarCodigoReset(@RequestParam String email) {
        try {
            boolean reenviado = codigoVerificacionService.reenviarCodigo(
                    email, CodigoVerificacion.TipoVerificacion.RECUPERACION_PASSWORD
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
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error reenviando código: " + e.getMessage()
            ));
        }
    }


}
