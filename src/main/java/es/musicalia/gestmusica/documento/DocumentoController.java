package es.musicalia.gestmusica.documento;

import es.musicalia.gestmusica.file.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping(value="/documentos")
public class DocumentoController {


    private final DocumentoService documentoService;
    private final FileService fileService;

    public DocumentoController(DocumentoService documentoService, FileService fileService) {

        this.documentoService = documentoService;
        this.fileService = fileService;
    }

@GetMapping("/descargar/{id}")
public ResponseEntity<byte[]> descargarDocumento(@PathVariable Long id) {
    try {
        Documento documento = documentoService.findById(id);
        String contentType = determinarContentType(documento.getNombre());
        byte[] fileBytes = fileService.getPrivateFileBytes(documento.getUrl(), documento.getResourceType(), contentType);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + documento.getNombre() + "\"")
                .body(fileBytes);
                
    } catch (Exception e) {
        log.error("Error al descargar el documento con ID: {}", id, e);
        return ResponseEntity.notFound().build();
    }
}

private String determinarContentType(String nombreArchivo) {
    if (nombreArchivo == null) {
        return "application/octet-stream";
    }
    
    String extension = nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1).toLowerCase();
    
    switch (extension) {
        case "pdf":
            return "application/pdf";
        case "zip":
            return "application/zip";
        case "doc":
            return "application/msword";
        case "docx":
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        case "txt":
            return "text/plain";
        case "jpg":
        case "jpeg":
            return "image/jpeg";
        case "png":
            return "image/png";
        default:
            return "application/octet-stream";
    }
}

    @GetMapping("/eliminar/{id}")
    public String eliminarDocumento(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {

            final Long idArtista = documentoService.findById(id).getArtista().getId();
            documentoService.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Documento eliminado");
            redirectAttributes.addFlashAttribute("alertClass", "success");
            return "redirect:/documentos/" + idArtista;
        } catch (Exception e) {
            log.error("Error al eliminar el documento con ID: {}", id, e);
            redirectAttributes.addFlashAttribute("message", "Error al eliminar el documento.");
            redirectAttributes.addFlashAttribute("alertClass", "danger");
            return "redirect:/artista";
        }
    }


    /**
     * Muestra la vista para gestionar los documentos de un artista.
     */
    @PreAuthorize("hasPermission(#idArtista, 'ARTISTA', 'DOCUMENTACION') or hasPermission(#idArtista, 'ARTISTA', 'DOCUMENTACION_DESCARGAR')")
    @GetMapping("/{idArtista}")
    public String documentosArtista(Model model, @PathVariable("idArtista") Long idArtista) {
        try {

            // Configurar el modelo
            model.addAttribute("documentos", documentoService.findByArtistaId(idArtista));

            DocumentoDto documentoDto = new DocumentoDto();
            documentoDto.setIdArtista(idArtista);
            model.addAttribute("documentoDto", documentoDto);

            return "documentos-artista";
        } catch (Exception e) {
            log.error("Error al cargar los documentos del artista con ID: {}", idArtista, e);
            model.addAttribute("message", "Error al cargar los documentos.");
            model.addAttribute("alertClass", "danger");
            return "redirect:/artista";
        }
    }

    /**
     * Maneja la subida de un nuevo documento para un artista.
     */
    @PreAuthorize("hasPermission(#documentoDto.idArtista, 'ARTISTA', 'DOCUMENTACION')")
    @PostMapping("/guardar")
    public String guardarDocumento(@ModelAttribute("documentoDto") DocumentoDto documentoDto,
                                   @RequestParam("documento") MultipartFile multipartFile,
                                   RedirectAttributes redirectAttributes) {
        try {
            if (multipartFile == null || multipartFile.isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "Debe seleccionar un archivo para subir.");
                redirectAttributes.addFlashAttribute("alertClass", "warning");
                return "redirect:/documentos/" + documentoDto.getIdArtista();
            }
            String contentType = determinarContentType(multipartFile.getOriginalFilename());

            this.documentoService.guardarDocumento(multipartFile, documentoDto, contentType);

            redirectAttributes.addFlashAttribute("message", "Documento subido correctamente");
            redirectAttributes.addFlashAttribute("alertClass", "success");

        } catch (Exception e) {
            log.error("Error al subir un documento", e);
            redirectAttributes.addFlashAttribute("message", "Error al subir el documento.");
            redirectAttributes.addFlashAttribute("alertClass", "danger");
        }
        return "redirect:/documentos/" + documentoDto.getIdArtista();
    }




}