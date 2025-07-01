package es.musicalia.gestmusica.incremento;


import es.musicalia.gestmusica.auth.model.SecurityService;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import es.musicalia.gestmusica.usuario.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(value="incremento")
public class IncrementoController {



    private final IncrementoService incrementoService;


    public IncrementoController(IncrementoService incrementoService){
        this.incrementoService = incrementoService;
    }

    @GetMapping("/list/{idArtista}")
//    @PreAuthorize("hasAuthority('" + Constantes.Permisos.SOLICITUDES_RESERVA_CONSULTAR + "')")
    @ResponseBody
    public List<IncrementoListDto> listaIncrementos(@PathVariable("idArtista") Long idArtista) {

        //TODO: Comprobar permisos sobre la agencia
        return this.incrementoService.findByIncrementosByArtista(idArtista);

    }
    @PostMapping("/save")
    //    @PreAuthorize("hasAuthority('" + Constantes.Permisos.MODIFICAR_INCREMENTOS + "')")
    public ResponseEntity<?> saveIncremento(
            @Valid @RequestBody IncrementoSaveDto incrementoSaveDto) {

        DefaultResponseBody result = new DefaultResponseBody();
        try {
            this.incrementoService.saveIncremento(incrementoSaveDto);
            result.setSuccess(true);
            result.setMessage("Incremento guardado");
            result.setMessageType("success");
            return ResponseEntity.ok(result);

        } catch (Exception e){
            result.setSuccess(false);
            result.setMessage("Error guardando el incremento");
            result.setMessageType("danger");
            return ResponseEntity.ok(result);
        }


    }

}
