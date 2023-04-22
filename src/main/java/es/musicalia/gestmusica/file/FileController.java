package es.musicalia.gestmusica.file;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class FileController {

    private FileService fileService;

    public FileController(FileService fileService){
        this.fileService = fileService;
    }

    @GetMapping("/image/{path}/{id}/{fileName}")
    public ResponseEntity<byte[]> serveImage(@PathVariable String path, @PathVariable String id, @PathVariable String fileName) {
        try {

            byte[] fileBytes = this.fileService.getImageFileBytes(path.concat("/").concat(id).concat("/").concat(fileName));

            ByteArrayResource resource = new ByteArrayResource(fileBytes);

            // Crear una respuesta HTTP con el archivo
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=\"" + fileName + "\"")
                    .body(resource.getByteArray());

        } catch (IOException ex) {
            return ResponseEntity.notFound().build();
        }
    }



}
