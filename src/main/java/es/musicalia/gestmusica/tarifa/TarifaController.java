package es.musicalia.gestmusica.tarifa;


import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.artista.ArtistaDto;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.auth.model.SecurityService;
import es.musicalia.gestmusica.informe.InformeService;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.util.DateUtils;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping(value="tarifa")
public class TarifaController {


    private UserService userService;
    private SecurityService securityService;

    private TarifaService tarifaService;
    private ArtistaService artistaService;


    private InformeService informeService;

    private Logger logger = LoggerFactory.getLogger(TarifaController.class);

    public TarifaController(UserService userService, SecurityService securityService,  AgenciaService agenciaService,
                            ArtistaService artistaService, TarifaService tarifaService, InformeService informeService){
        this.userService = userService;
        this.securityService = securityService;
        this.artistaService = artistaService;
        this.tarifaService = tarifaService;
        this.informeService = informeService;

    }

    @PostMapping("/save")
    public ResponseEntity<?> getSearchResultViaAjax(
            @Valid @RequestBody TarifaSaveDto tarifaSaveDto) {

        DefaultResponseBody result = new DefaultResponseBody();
        this.tarifaService.saveTarifa(tarifaSaveDto);
        result.setSuccess(true);
        result.setMessage("Tarifa guardada");
        result.setMessageType("success");
        return ResponseEntity.ok(result);

    }

    // TODO: securizar por permiso
    @PostMapping("/eliminar")
    public ResponseEntity<?> eliminarTarifas(
            @Valid @RequestBody TarifaSaveDto tarifaSaveDto) {


        // TODO: securizar por permiso de usuario sobre agrupación
        DefaultResponseBody result = new DefaultResponseBody();
        tarifaSaveDto.setActivo(false);
        this.tarifaService.saveTarifa(tarifaSaveDto);
        result.setSuccess(true);
        result.setMessage("Tarifa eliminada");
        result.setMessageType("success");
        return ResponseEntity.ok(result);

    }

    @GetMapping("/tarifa-anual/{idArtista}/{anoTarifa}")
    public ResponseEntity<byte[]> downloadTarifaAnual(@PathVariable("idArtista") Long idArtista,@PathVariable("anoTarifa") String anoTarifa) {

        // Cargar el informe desde algún lugar y almacenarlo en un arreglo de bytes.
        Map<String, Object> parametros = new HashMap<String, Object>();
        final ArtistaDto astistaDto = this.artistaService.findArtistaDtoById(idArtista);
        parametros.put("titulo", astistaDto.getNombre());
        parametros.put("idArtista", idArtista.intValue());
        parametros.put("ano", anoTarifa);
        logger.info("Tarifa anual para el artista con id: " +idArtista);

        String fileNameToExport = astistaDto.getNombre().concat(DateUtils.getDateStr(new Date(), "ddMMyyyyHHmmss")).concat(".pdf");
        String fileReport = "tarifa_anual_horizontal.jrxml";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileNameToExport);

        return new ResponseEntity<byte[]>(this.informeService.imprimirInforme(parametros, fileNameToExport, fileReport),headers, HttpStatus.OK);

    }

    @GetMapping("/{idArtista}/{fecha}")
    public ResponseEntity<TarifaDto> findTarifaByArtistaAndFecha(@PathVariable("idArtista") Long idArtista, @PathVariable("fecha") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate fecha) {
        return ResponseEntity.ok(this.tarifaService.findByArtistaIdAndDate(idArtista, fecha));
    }






}
