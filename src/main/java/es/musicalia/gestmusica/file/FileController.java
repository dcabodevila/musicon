package es.musicalia.gestmusica.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class FileController {

    private FileService fileService;

    public FileController(FileService fileService){
        this.fileService = fileService;
    }

    @GetMapping("/image/{path}/{id}/{fileName}")
    public ResponseEntity<byte[]> serveImage(@PathVariable String path, @PathVariable String id, @PathVariable String fileName) {
        try {

            byte[] fileBytes = this.fileService.getFileBytes(path.concat("/").concat(id).concat("/").concat(fileName));

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
