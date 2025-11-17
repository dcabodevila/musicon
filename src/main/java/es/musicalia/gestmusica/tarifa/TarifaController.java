package es.musicalia.gestmusica.tarifa;


import es.musicalia.gestmusica.artista.ArtistaDto;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.util.DateUtils;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(value="tarifa")
public class TarifaController {


    private final TarifaService tarifaService;
    private final ArtistaService artistaService;

    public TarifaController(ArtistaService artistaService, TarifaService tarifaService) {
        this.artistaService = artistaService;
        this.tarifaService = tarifaService;
    }

    @PostMapping("/save")
    public ResponseEntity<?> getSearchResultViaAjax(
            @Valid @RequestBody TarifaSaveDto tarifaSaveDto) {

        this.tarifaService.saveTarifa(tarifaSaveDto);
        return ResponseEntity.ok(DefaultResponseBody.builder().success(true).message("Tarifa guardada").messageType("success").build() );

    }

    // TODO: securizar por permiso
    @PostMapping("/eliminar")
    public ResponseEntity<?> eliminarTarifas(
            @Valid @RequestBody TarifaSaveDto tarifaSaveDto) {

        tarifaSaveDto.setActivo(false);
        this.tarifaService.saveTarifa(tarifaSaveDto);

        return ResponseEntity.ok(DefaultResponseBody.builder().success(true).message("Tarifa eliminada").messageType("success").build() );

    }

    @GetMapping("/{idArtista}/{fecha}")
    public ResponseEntity<TarifaDto> findTarifaByArtistaAndFecha(@PathVariable("idArtista") Long idArtista, @PathVariable("fecha") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate fecha) {
        return ResponseEntity.ok(this.tarifaService.findByArtistaIdAndDate(idArtista, fecha));
    }

    @GetMapping("/lista/{idArtista}/{fecha}")
    public ResponseEntity<List<TarifaArtistaCcaaDto>> findTarifasByCcaaAndFecha(
            @PathVariable("idArtista") Long idArtista,
            @PathVariable("fecha") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate fecha) {
        return ResponseEntity.ok(this.tarifaService.findTarifasByFechaAndNumComponentesArtista(idArtista,  fecha));
    }


    @PostMapping("/tarifa-anual")
    public ResponseEntity<byte[]> generarTarifaAnual(Model model, @ModelAttribute("tarifaAnualDto") @Valid TarifaAnualDto tarifaAnualDto,
                                                 BindingResult bindingResult, RedirectAttributes redirectAttributes, Errors errors) {


        byte[] informeGenerado = this.tarifaService.getInformeTarifaAnual(tarifaAnualDto);


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        final ArtistaDto astistaDto = this.artistaService.findArtistaDtoById(tarifaAnualDto.getIdArtista());



        String fileNameToExport = astistaDto.getNombre().concat(tarifaAnualDto.getAno().toString()).concat(DateUtils.getDateStr(new Date(), "-ddMMyyyyHHmmss")).concat(".pdf");
        headers.setContentDispositionFormData("attachment", fileNameToExport);

        return new ResponseEntity<byte[]>(informeGenerado,headers, HttpStatus.OK);



    }




}
