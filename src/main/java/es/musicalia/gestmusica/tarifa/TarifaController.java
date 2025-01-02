package es.musicalia.gestmusica.tarifa;


import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.artista.ArtistaDto;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.auth.model.SecurityService;
import es.musicalia.gestmusica.incremento.Incremento;
import es.musicalia.gestmusica.incremento.IncrementoService;
import es.musicalia.gestmusica.incremento.IncrementoServiceImpl;
import es.musicalia.gestmusica.informe.InformeService;
import es.musicalia.gestmusica.listado.ListadoDto;
import es.musicalia.gestmusica.listado.TipoOcupacionEnum;
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
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping(value="tarifa")
public class TarifaController {


    private final UserService userService;
    private final SecurityService securityService;

    private final TarifaService tarifaService;
    private final ArtistaService artistaService;


    private final InformeService informeService;
    private final IncrementoService incrementoService;

    private Logger logger = LoggerFactory.getLogger(TarifaController.class);

    public TarifaController(UserService userService, SecurityService securityService, AgenciaService agenciaService,
                            ArtistaService artistaService, TarifaService tarifaService, InformeService informeService, IncrementoService incrementoService){
        this.userService = userService;
        this.securityService = securityService;
        this.artistaService = artistaService;
        this.tarifaService = tarifaService;
        this.informeService = informeService;
        this.incrementoService = incrementoService;
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

    @GetMapping("/{idArtista}/{fecha}")
    public ResponseEntity<TarifaDto> findTarifaByArtistaAndFecha(@PathVariable("idArtista") Long idArtista, @PathVariable("fecha") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate fecha) {
        return ResponseEntity.ok(this.tarifaService.findByArtistaIdAndDate(idArtista, fecha));
    }

    @PostMapping("/tarifa-anual")
    public ResponseEntity<byte[]> generarTarifaAnual(Model model, @ModelAttribute("tarifaAnualDto") @Valid TarifaAnualDto tarifaAnualDto,
                                                 BindingResult bindingResult, RedirectAttributes redirectAttributes, Errors errors) {


        byte[] informeGenerado = this.tarifaService.getInformeTarifaAnual(tarifaAnualDto);


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        final ArtistaDto astistaDto = this.artistaService.findArtistaDtoById(tarifaAnualDto.getIdArtista());

        String fileNameToExport = astistaDto.getNombre().concat(DateUtils.getDateStr(new Date(), "ddMMyyyyHHmmss")).concat(".pdf");
        headers.setContentDispositionFormData("attachment", fileNameToExport);

        return new ResponseEntity<byte[]>(informeGenerado,headers, HttpStatus.OK);



    }




}
