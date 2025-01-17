package es.musicalia.gestmusica.listado;


import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.ajustes.AjustesDto;
import es.musicalia.gestmusica.ajustes.AjustesService;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.informe.InformeService;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.Date;


@Controller
@RequestMapping(value="listado")
public class ListadoController {


    private Logger logger = LoggerFactory.getLogger(ListadoController.class);
    private final UserService userService;
    private final LocalizacionService localizacionService;
    private final ListadoService listadoService;
    private final InformeService informeService;
    private final ArtistaService artistaService;
    private final AgenciaService agenciaService;
    private final AjustesService ajustesService;
    public ListadoController(UserService userService, LocalizacionService localizacionService, ListadoService listadoService, InformeService informeService, ArtistaService artistaService,
                             AgenciaService agenciaService, AjustesService ajustesService){

        this.userService = userService;
        this.localizacionService = localizacionService;
        this.listadoService = listadoService;
        this.informeService = informeService;
        this.artistaService = artistaService;
        this.agenciaService = agenciaService;
        this.ajustesService = ajustesService;
    }

    @GetMapping
    public String listados(Model model) {
        if (userService.isUserAutheticated()){
            final Usuario usuario = userService.obtenerUsuarioAutenticado();
        }
        final Usuario usuario = this.userService.obtenerUsuarioAutenticado();
        ListadoDto listado = new ListadoDto();
        listado.setSolicitadoPara(usuario.getNombreCompleto());
        listado.setIdCcaa(12L);
        listado.setIdProvincia(27L);
        listado.setIdTipoOcupacion(1L);
        model.addAttribute("listadoDto", listado);
        model.addAttribute("listaCcaa", this.localizacionService.findAllComunidades());
        model.addAttribute("listaProvinciasCcaaListado", this.localizacionService.findAllProvinciasByCcaaId(12L));
        model.addAttribute("listaMunicipioListado", this.localizacionService.findAllMunicipiosByIdProvincia(27L));
        model.addAttribute("listaTiposOcupacion", this.listadoService.findAllTiposOcupacion());
        model.addAttribute("listaTipoArtista", this.artistaService.listaTipoArtista());
        model.addAttribute("listaAgencias", this.agenciaService.listaAgenciasRecordActivasTarifasPublicas());

        final AjustesDto ajustesDto = this.ajustesService.getAjustesByIdUsuario(usuario.getId());

        if (ajustesDto!=null){
            listado.setIdsTipoArtista(ajustesDto.getIdsTipoArtista());
            listado.setIdsAgencias(ajustesDto.getIdsAgencias());
            listado.setIdsComunidades(ajustesDto.getIdsComunidades());
        }

        return "listados";
    }

    @PostMapping("/generar")
    public ResponseEntity<byte[]> generarListado(Model model, @ModelAttribute("listadoDto") @Valid ListadoDto listadoDto,
                                 BindingResult bindingResult, RedirectAttributes redirectAttributes, Errors errors) {


        byte[] informeGenerado = this.listadoService.generarInformeListado(listadoDto);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        String fileNameToExport = "Listado_".concat(TipoOcupacionEnum.getDescripcionById(listadoDto.getIdTipoOcupacion())).concat(DateUtils.getDateStr(new Date(), "ddMMyyyyHHmmss")).concat(".pdf");
        headers.setContentDispositionFormData("attachment", fileNameToExport);

        return new ResponseEntity<byte[]>(informeGenerado,headers, HttpStatus.OK);



    }



}
