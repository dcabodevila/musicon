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

        try {
            this.incrementoService.saveIncremento(incrementoSaveDto);
            return ResponseEntity.ok(DefaultResponseBody.builder().success(true).message("Incremento guardado").messageType("success").build());

        } catch (Exception e){

            return ResponseEntity.ok(DefaultResponseBody.builder().success(false).message("Error guardando el incremento").messageType("danger").build());
        }


    }

}
