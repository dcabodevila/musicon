package es.musicalia.gestmusica.localizacion;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value="localizacion")
public class LocalizacionController {


    private LocalizacionService localizacionService;

    private Logger logger = LoggerFactory.getLogger(LocalizacionController.class);

    public LocalizacionController(LocalizacionService localizacionService){
        this.localizacionService = localizacionService;
    }


    @GetMapping("/provincias/{idCcaa}")
    public ResponseEntity<List<CodigoNombreDto>> listProvinciasByIdCcaa(@PathVariable("idCcaa") Long idCcaa) {

        return ResponseEntity.ok(this.localizacionService.findAllProvinciasByCcaaId(idCcaa));

    }

    @GetMapping("/municipios/{idProvincia}")
    public ResponseEntity<List<CodigoNombreDto>> listMunicipiosByIdProvincia(@PathVariable("idProvincia") Long idProvincia) {

        return ResponseEntity.ok(this.localizacionService.findAllMunicipiosByIdProvincia(idProvincia));

    }

}
