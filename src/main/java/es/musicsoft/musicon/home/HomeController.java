package es.musicsoft.musicon.home;

import es.musicsoft.musicon.auth.model.RegistrationForm;
import es.musicsoft.musicon.auth.model.SecurityService;
import es.musicsoft.musicon.usuario.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;

@Controller
public class HomeController {


    private UserService userService;
    private SecurityService securityService;

    public HomeController(UserService userService, SecurityService securityService){
        this.userService = userService;
        this.securityService = securityService;

    }

    @GetMapping("/")
    public String home() {

        if (this.userService.isUserAutheticated()) {

            return "hello.html";
        }

        return "landing-page";
    }

    @GetMapping("/admin")
    public String admin() {
        return "/admin";
    }

    @GetMapping("/user")
    public String user() {
        return "/user";
    }

    @GetMapping("/login-redirect")
    public String loginRedirect() {
        return "redirect:/";
    }

    @GetMapping(value = "/login")
    public String login(Model model, String error, String logout) {
        if (error != null)
            model.addAttribute("error", "Your username and password is invalid.");

        if (logout != null)
            model.addAttribute("message", "You have been logged out successfully.");
        SecurityContextHolder.clearContext();
        return "login";
    }

    @GetMapping("/registration")
    public String registration(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());

        return "registration";
    }

    @PostMapping("/registration")
    public String registration(@ModelAttribute("registrationForm") @Valid RegistrationForm registrationForm,
                               BindingResult bindingResult, Errors errors) {
//        userValidator.validate(userForm, bindingResult);

        if (bindingResult.hasErrors()) {
            return "registration";
        }

        userService.saveRegistration(registrationForm);

        securityService.autoLogin(registrationForm.getUsername(), registrationForm.getPassword());

        return "redirect:/";
    }

    @GetMapping("/403")
    public String error403() {
        return "/error/403";
    }

}
